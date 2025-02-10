package de.samply.token;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.security.SessionUser;
import de.samply.token.dto.*;
import de.samply.user.roles.ProjectRole;
import de.samply.utils.WebClientFactory;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
@Slf4j
public class DataShieldTokenManagerService {

    private final SessionUser sessionUser;
    private final WebClientFactory webClientFactory;
    private final WebClient webClient;
    private final ProjectRepository projectRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private final NotificationService notificationService;
    private final BridgeheadConfiguration bridgeheadConfiguration;
    private final boolean isTokenManagerActive;

    public DataShieldTokenManagerService(SessionUser sessionUser,
                                         WebClientFactory webClientFactory,
                                         @Value(ProjectManagerConst.TOKEN_MANAGER_URL_SV) String tokenManagerUrl,
                                         ProjectRepository projectRepository,
                                         ProjectBridgeheadRepository projectBridgeheadRepository,
                                         ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                                         NotificationService notificationService,
                                         BridgeheadConfiguration bridgeheadConfiguration,
                                         @Value(ProjectManagerConst.ENABLE_TOKEN_MANAGER_SV) boolean isTokenManagerActive) {
        this.sessionUser = sessionUser;
        this.webClientFactory = webClientFactory;
        this.projectRepository = projectRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.notificationService = notificationService;
        this.bridgeheadConfiguration = bridgeheadConfiguration;
        this.isTokenManagerActive = isTokenManagerActive;
        this.webClient = webClientFactory.createWebClient(tokenManagerUrl);
    }

    public Mono<Void> generateTokensInOpal(@NotNull String projectCode, @NotNull String bridgehead, @NotNull String email, Supplier<Mono> ifSuccessMonoSupplier) throws DataShieldTokenManagerServiceException {
        if (!isTokenManagerActive) {
            log.error("Token manager is not active. It couldn't generate token in opal for project " + projectCode + " and bridgehead " + bridgehead + " and user " + email);
            return Mono.empty();
        }
        List<String> bridgeheads = List.of(bridgehead);
        List<String> tokenManagerIds = fetchTokenManagerIds(bridgeheads);
        if (tokenManagerIds.size() > 0) {
            AtomicInteger retryCount = new AtomicInteger(0);
            return webClient.post().uri(uriBuilder ->
                            uriBuilder.path(ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_TOKENS).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new TokenParams(email, projectCode, tokenManagerIds))
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(
                            Retry.fixedDelay(webClientFactory.getWebClientMaxNumberOfRetries(), Duration.ofSeconds(webClientFactory.getWebClientTimeInSecondsAfterRetryWithFailure()))
                                    .filter(error -> error instanceof WebClientResponseException)
                                    .doBeforeRetry(s -> retryCount.incrementAndGet())
                    )
                    .doOnError(WebClientResponseException.class, ex -> {
                        HttpStatusCode statusCode = ex.getStatusCode();
                        String error = ExceptionUtils.getStackTrace(ex);
                        log.error(error);
                        if (statusCode.equals(HttpStatus.BAD_REQUEST)) {
                            log.error("Received 400 Bad Request");
                        } else if (statusCode.is5xxServerError()) {
                            log.error("Received Server Error: " + statusCode);
                        } else {
                            log.error("Received HTTP Status Code: " + statusCode);
                        }
                        if (retryCount.get() >= webClientFactory.getWebClientMaxNumberOfRetries()) {
                            bridgeheads.forEach(tempBridgehead ->
                                    notificationService.createNotification(projectCode, tempBridgehead, email,
                                            OperationType.CREATE_DATASHIELD_TOKEN, "Error generating token", error, (HttpStatus) statusCode));
                        }
                    })
                    .doOnSuccess(response -> bridgeheads.forEach(tempBridgehead ->
                            notificationService.createNotification(projectCode, tempBridgehead, email,
                                    OperationType.CREATE_DATASHIELD_TOKEN, "Token generated successfully in Token Manager", null, null)))
                    .then((ifSuccessMonoSupplier != null) ? ifSuccessMonoSupplier.get() : Mono.empty());
        }
        return Mono.empty();
    }

    public List<String> fetchProjectBridgeheads(String projectCode, String bridgehead, String email) throws DataShieldTokenManagerServiceException {
        return fetchProjectBridgeheads(projectCode, bridgehead, email, projectBridgehead -> true);
    }

    public List<String> fetchProjectBridgeheads(String projectCode, String bridgehead, String email, Function<ProjectBridgehead, Boolean> filter) throws DataShieldTokenManagerServiceException {
        Project project = fetchProject(projectCode);
        Optional<ProjectBridgeheadUser> projectBridgeheadUser = projectBridgeheadUserRepository.getFirstByEmailAndProjectBridgehead_ProjectAndProjectBridgehead_BridgeheadOrderByModifiedAtDesc(email, project, bridgehead);
        if (projectBridgeheadUser.isEmpty()) {
            throw new DataShieldTokenManagerServiceException("User " + email + " with token manager rights not found for project " + projectCode);
        }
        ProjectRole userProjectRole = projectBridgeheadUser.get().getProjectRole();
        if (userProjectRole == ProjectRole.DEVELOPER || userProjectRole == ProjectRole.PILOT) {
            return (filter.apply(projectBridgeheadUser.get().getProjectBridgehead())) ? List.of(bridgehead) : List.of();
        } else if (userProjectRole == ProjectRole.FINAL) {
            return projectBridgeheadRepository.findByProject(project).stream().filter(filter::apply).map(projectBridgehead -> projectBridgehead.getBridgehead()).toList();
        } else {
            throw new DataShieldTokenManagerServiceException("Role " + userProjectRole + " of user " + email + " not supported");
        }
    }

    private Project fetchProject(String projectCode) throws DataShieldTokenManagerServiceException {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new DataShieldTokenManagerServiceException("Project " + projectCode + " not found");
        }
        return project.get();
    }

    public Mono<DataShieldTokenManagerTokenStatus> fetchTokenStatus(@NotNull String projectCode, @NotNull String bridgehead, @NotNull String email) {
        if (!isTokenManagerActive) {
            return Mono.just(new DataShieldTokenManagerTokenStatus(projectCode, bridgehead, email, null, DataShieldProjectStatus.INACTIVE, DataShieldTokenStatus.INACTIVE));
        }
        Optional<String> tokenManagerId = fetchTokenManagerId(bridgehead);
        if (tokenManagerId.isPresent()) {
            String uri = UriComponentsBuilder.fromPath(ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_TOKEN_STATUS)
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_BRIDGEHEAD, tokenManagerId.get())
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_PROJECT_CODE, projectCode)
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_EMAIL, email)
                    .toUriString();
            return webClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(DataShieldTokenManagerTokenStatus.class)
                    .onErrorResume(exception -> {
                        log.debug(ExceptionUtils.getStackTrace(exception));
                        return Mono.just(new DataShieldTokenManagerTokenStatus(projectCode, bridgehead, email, Instant.now().toString(), DataShieldProjectStatus.ERROR, DataShieldTokenStatus.ERROR));
                    })
                    .map(this::replaceTokenManagerId);


        } else {
            throw new DataShieldTokenManagerServiceException("Bridgehead " + bridgehead + " not configured for token manager");
        }
    }

    public Mono<DataShieldTokenManagerProjectStatus> fetchProjectStatus(@NotNull String projectCode, @NotNull String bridgehead) {
        if (!isTokenManagerActive) {
            return Mono.just(new DataShieldTokenManagerProjectStatus(projectCode, bridgehead, DataShieldProjectStatus.INACTIVE));
        }
        Optional<String> tokenManagerId = fetchTokenManagerId(bridgehead);
        if (tokenManagerId.isPresent()) {
            String uri = UriComponentsBuilder.fromPath(ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_PROJECT_STATUS)
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_BRIDGEHEAD, tokenManagerId.get())
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_PROJECT_CODE, projectCode)
                    .toUriString();
            return webClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(DataShieldTokenManagerProjectStatus.class)
                    .onErrorResume(exception -> {
                        log.debug(ExceptionUtils.getStackTrace(exception));
                        return Mono.just(new DataShieldTokenManagerProjectStatus(projectCode, bridgehead, DataShieldProjectStatus.ERROR));
                    })
                    .map(this::replaceTokenManagerId);
        } else {
            throw new DataShieldTokenManagerServiceException("Bridgehead " + bridgehead + " not configured for token manager");
        }
    }

    public Resource fetchAuthenticationScript(String projectCode, String bridgehead) throws DataShieldTokenManagerServiceException {
        if (!isTokenManagerActive) {
            return new ByteArrayResource("Token Manager inactive".getBytes());
        }
        List<String> tokenManagerIds = fetchTokenManagerIds(fetchProjectBridgeheads(projectCode, bridgehead, sessionUser.getEmail()));
        if (tokenManagerIds.size() > 0) {
            String authenticationScript = webClient.post().uri(uriBuilder ->
                            uriBuilder.path(ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_SCRIPTS).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new TokenParams(sessionUser.getEmail(), projectCode, tokenManagerIds))
                    .accept(MediaType.TEXT_PLAIN).retrieve().bodyToMono(String.class).block();
            if (!StringUtils.hasText(authenticationScript)) {
                throw new DataShieldTokenManagerServiceException("Script could not be generated for project " + projectCode + " and user " + sessionUser.getEmail());
            }
            return new ByteArrayResource(authenticationScript.getBytes());
        }
        throw new DataShieldTokenManagerServiceException("Script could not be generated for project " + projectCode + " and user " + sessionUser.getEmail());
    }

    public Boolean existsAuthenticationScript(String projectCode, String bridgehead) {
        if (!isTokenManagerActive) {
            return false;
        }
        List<String> tokenManagerIds = fetchTokenManagerIds(fetchProjectBridgeheads(projectCode, bridgehead, sessionUser.getEmail()));
        return Boolean.valueOf(webClient.post()
                .uri(uriBuilder ->
                        uriBuilder.path(ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.AUTHENTICATION_SCRIPT_STATUS).build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TokenParams(sessionUser.getEmail(), projectCode, tokenManagerIds))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(exception -> {
                    log.debug(ExceptionUtils.getStackTrace(exception));
                    return Mono.just("false");
                })
                .block());
    }


    public Mono<Void> refreshToken(@NotNull String projectCode, @NotNull String bridgehead, @NotNull String email, Supplier<Mono> ifSuccessMonoSupplier) throws DataShieldTokenManagerServiceException {
        if (!isTokenManagerActive) {
            log.error("Token Manager inactive in project manager. It coudn#t refresh token for project " + projectCode + " and bridgehead " + bridgehead + " and user " + email);
            return Mono.empty();
        }
        List<String> tokenManagerIds = fetchTokenManagerIds(fetchProjectBridgeheads(projectCode, bridgehead, email));
        if (tokenManagerIds.size() > 0) {
            TokenParams tokenParams = new TokenParams(email, projectCode, tokenManagerIds);
            String uri = ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_REFRESH_TOKEN;

            return webClient.put()
                    .uri(uri)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(BodyInserters.fromValue(tokenParams))
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(response -> notificationService.createNotification(projectCode, bridgehead, email,
                            OperationType.REFRESH_DATASHIELD_TOKEN, "Token refreshed", null, null))
                    .then((ifSuccessMonoSupplier != null) ? ifSuccessMonoSupplier.get() : Mono.empty());
        }
        return Mono.empty();
    }

    public Mono<Void> removeTokens(@NotNull String projectCode, @NotNull String bridgehead, @NotNull String email, Supplier<Mono> ifSuccessMonoSupplier) {
        if (!isTokenManagerActive) {
            log.error("Token Manager inactive in project manager. It couln't remove tokens for project " + projectCode + " and bridgehead " + bridgehead + " and user " + email);
            return Mono.empty();
        }
        Optional<String> tokenManagerId = fetchTokenManagerId(bridgehead);
        if (tokenManagerId.isPresent()) {
            String uri = UriComponentsBuilder.fromPath(ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_TOKENS)
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_BRIDGEHEAD, tokenManagerId.get())
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_PROJECT_CODE, projectCode)
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_EMAIL, email)
                    .toUriString();
            return webClient.delete()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .doOnSuccess(response -> {
                        notificationService.createNotification(projectCode, bridgehead, email,
                                OperationType.REMOVE_DATASHIELD_TOKEN, "Token removed", null, null);
                    })
                    .then((ifSuccessMonoSupplier != null) ? ifSuccessMonoSupplier.get() : Mono.empty());
        } else {
            log.error("Bridgehead " + bridgehead + " not configured for token manager");
            return Mono.empty();
        }
    }

    public Mono<Void> removeProjectAndTokens(@NotNull String projectCode, @NotNull String bridgehead) {
        if (!isTokenManagerActive) {
            log.error("Token Manager inactive in project manager. It cannot remove tokens for project " + projectCode + " and bridgehead " + bridgehead);
            return Mono.empty();
        }
        Optional<String> tokenManagerId = fetchTokenManagerId(bridgehead);
        if (tokenManagerId.isPresent()) {
            String uri = UriComponentsBuilder.fromPath(ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_PROJECT)
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_BRIDGEHEAD, tokenManagerId.get())
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_PROJECT_CODE, projectCode)
                    .toUriString();
            return webClient.delete()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .doOnSuccess(response -> notificationService.createNotification(projectCode, bridgehead, null,
                            OperationType.REMOVE_DATASHIELD_TOKEN, "Token removed", null, null))
                    .then();
        } else {
            log.error("Bridgehead " + bridgehead + " not configured for token manager");
            return Mono.empty();
        }
    }

    private List<String> fetchTokenManagerIds(List<String> bridgeheads) {
        return bridgeheads.stream().map(bridgehead -> {
            Optional<String> tokenManagerId = fetchTokenManagerId(bridgehead);
            return (tokenManagerId.isPresent()) ? tokenManagerId.get() : null;
        }).filter(ObjectUtils::isNotEmpty).toList();
    }

    private Optional<String> fetchTokenManagerId(String bridgehead) {
        return bridgeheadConfiguration.getTokenManagerId(bridgehead);
    }

    private DataShieldTokenManagerProjectStatus replaceTokenManagerId(DataShieldTokenManagerProjectStatus dataShieldTokenManagerProjectStatus) {
        return (dataShieldTokenManagerProjectStatus != null) ?
                new DataShieldTokenManagerProjectStatus(
                        dataShieldTokenManagerProjectStatus.projectCode(),
                        bridgeheadConfiguration.fetchBridgeheadForTokenManagerId(dataShieldTokenManagerProjectStatus.bridgehead()),
                        dataShieldTokenManagerProjectStatus.projectStatus()
                )
                : dataShieldTokenManagerProjectStatus;
    }

    private DataShieldTokenManagerTokenStatus replaceTokenManagerId(DataShieldTokenManagerTokenStatus dataShieldTokenManagerTokenStatus) {
        return (dataShieldTokenManagerTokenStatus != null) ?
                new DataShieldTokenManagerTokenStatus(
                        dataShieldTokenManagerTokenStatus.projectCode(),
                        bridgeheadConfiguration.fetchBridgeheadForTokenManagerId(dataShieldTokenManagerTokenStatus.bridgehead()),
                        dataShieldTokenManagerTokenStatus.email(),
                        dataShieldTokenManagerTokenStatus.createdAt(),
                        dataShieldTokenManagerTokenStatus.projectStatus(),
                        dataShieldTokenManagerTokenStatus.tokenStatus()
                )
                : dataShieldTokenManagerTokenStatus;
    }

}
