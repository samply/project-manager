package de.samply.datashield;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.datashield.dto.*;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.project.ProjectBridgeheadService;
import de.samply.project.ProjectBridgeheadUserService;
import de.samply.security.SessionUser;
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


    // Services
    private final ProjectBridgeheadService projectBridgeheadService;
    private final ProjectBridgeheadUserService projectBridgeheadUserService;
    private final NotificationService notificationService;

    private final SessionUser sessionUser;
    private final WebClientFactory webClientFactory;
    private final WebClient webClient;

    private final BridgeheadsConfiguration bridgeheadsConfiguration;

    private final boolean isTokenManagerActive;

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public DataShieldTokenManagerService(SessionUser sessionUser,
                                         WebClientFactory webClientFactory,
                                         @Value(ProjectManagerConst.TOKEN_MANAGER_URL_SV) String tokenManagerUrl,
                                         ProjectBridgeheadService projectBridgeheadService,
                                         ProjectBridgeheadUserService projectBridgeheadUserService,
                                         NotificationService notificationService,
                                         BridgeheadsConfiguration bridgeheadsConfiguration,
                                         @Value(ProjectManagerConst.ENABLE_TOKEN_MANAGER_SV) boolean isTokenManagerActive) {
        this.sessionUser = sessionUser;
        this.webClientFactory = webClientFactory;
        this.projectBridgeheadService = projectBridgeheadService;
        this.projectBridgeheadUserService = projectBridgeheadUserService;
        this.notificationService = notificationService;
        this.bridgeheadsConfiguration = bridgeheadsConfiguration;
        this.isTokenManagerActive = isTokenManagerActive;
        this.webClient = webClientFactory.createWebClient(tokenManagerUrl);
    }

    public Mono<Void> generateTokensInOpal(@NotNull Project project, @NotNull ProjectBridgehead bridgehead, @NotNull String email, Supplier<Mono> ifSuccessMonoSupplier) throws DataShieldTokenManagerServiceException {
        if (!isTokenManagerActive) {
            log.error("Token manager is not active. It couldn't generate token in opal for project {} and bridgehead {} and user {}", project.getCode(), bridgehead.getBridgehead(), email);
            return Mono.empty();
        }
        List<ProjectBridgehead> bridgeheads = List.of(bridgehead);
        List<String> tokenManagerIds = fetchTokenManagerIds(bridgeheads);
        if (!tokenManagerIds.isEmpty()) {
            log.info("Generating token in Opal for project {}, bridgehead {} and user {}...", project, bridgehead, email);
            AtomicInteger retryCount = new AtomicInteger(0);
            return webClient.post().uri(uriBuilder ->
                            uriBuilder.path(ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_TOKENS).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new TokenParams(email, project.getCode(), tokenManagerIds))
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(
                            Retry.fixedDelay(webClientFactory.getWebClientMaxNumberOfRetries(), Duration.ofSeconds(webClientFactory.getWebClientTimeInSecondsAfterRetryWithFailure()))
                                    .filter(error -> error instanceof WebClientResponseException)
                                    .doBeforeRetry(_ -> retryCount.incrementAndGet())
                    )
                    .doOnError(WebClientResponseException.class, ex -> {
                        HttpStatusCode statusCode = ex.getStatusCode();
                        String error = ExceptionUtils.getStackTrace(ex);
                        log.error(error);
                        if (statusCode.equals(HttpStatus.BAD_REQUEST)) {
                            log.error("Received 400 Bad Request");
                        } else if (statusCode.is5xxServerError()) {
                            log.error("Received Server Error: {}", statusCode);
                        } else {
                            log.error("Received HTTP Status Code: {}", statusCode);
                        }
                        if (retryCount.get() >= webClientFactory.getWebClientMaxNumberOfRetries()) {
                            bridgeheads.forEach(tempBridgehead ->
                                    notificationService.createNotification(project, tempBridgehead.getBridgehead(), email,
                                            OperationType.CREATE_DATASHIELD_TOKEN, "Error generating token", error, (HttpStatus) statusCode));
                        }
                    })
                    .doOnSuccess(_ -> bridgeheads.forEach(tempBridgehead -> {
                        log.info("Token generated successfully for bridgehead {}", tempBridgehead);
                        notificationService.createNotification(project, tempBridgehead.getBridgehead(), email,
                                OperationType.CREATE_DATASHIELD_TOKEN, "Token generated successfully in Token Manager", null, null);
                    }))
                    .then((ifSuccessMonoSupplier != null) ? ifSuccessMonoSupplier.get() : Mono.empty());
        }
        return Mono.empty();
    }

    public List<ProjectBridgehead> fetchProjectBridgeheads(Project project, ProjectBridgehead bridgehead, String email) throws DataShieldTokenManagerServiceException {
        return fetchProjectBridgeheads(project, bridgehead, email, _ -> true);
    }

    public List<ProjectBridgehead> fetchProjectBridgeheads(Project project, ProjectBridgehead bridgehead, String email, Function<ProjectBridgehead, Boolean> filter) throws DataShieldTokenManagerServiceException {
        Optional<ProjectBridgeheadUser> projectBridgeheadUser = projectBridgeheadUserService.fetchFirstUsersOrderByModifiedAtDesc(email, bridgehead);
        if (projectBridgeheadUser.isEmpty()) {
            throw new DataShieldTokenManagerServiceException("User " + email + " with token manager rights not found for project " + project);
        }
        ProjectRole userProjectRole = projectBridgeheadUser.get().getProjectRole();
        if (userProjectRole == ProjectRole.DEVELOPER || userProjectRole == ProjectRole.PILOT) {
            return (filter.apply(projectBridgeheadUser.get().getProjectBridgehead())) ? List.of(bridgehead) : List.of();
        } else if (userProjectRole == ProjectRole.FINAL) {
            return projectBridgeheadService.fetchBridgeheads(project).stream().filter(filter::apply).toList();
        } else {
            throw new DataShieldTokenManagerServiceException("Role " + userProjectRole + " of user " + email + " not supported");
        }
    }

    public Mono<DataShieldTokenManagerTokenStatus> fetchTokenStatus(@NotNull Project project, @NotNull ProjectBridgehead bridgehead, @NotNull String email) {
        if (!isTokenManagerActive) {
            return Mono.just(new DataShieldTokenManagerTokenStatus(project.getCode(), bridgehead.getBridgehead(), email, null, DataShieldProjectStatus.INACTIVE, DataShieldTokenStatus.INACTIVE));
        }
        Optional<String> tokenManagerId = fetchTokenManagerId(bridgehead);
        if (tokenManagerId.isPresent()) {
            String uri = UriComponentsBuilder.fromPath(ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_TOKEN_STATUS)
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_BRIDGEHEAD, tokenManagerId.get())
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_PROJECT_CODE, project)
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_EMAIL, email)
                    .toUriString();
            log.debug("Fetching DataSHIELD Token status for project {}, bridgehead {} and user {}...", project, bridgehead, email);
            return webClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(DataShieldTokenManagerTokenStatus.class)
                    .onErrorResume(exception -> {
                        log.debug(ExceptionUtils.getStackTrace(exception));
                        return Mono.just(new DataShieldTokenManagerTokenStatus(project.getCode(), bridgehead.getBridgehead(), email, Instant.now().toString(), DataShieldProjectStatus.ERROR, DataShieldTokenStatus.ERROR));
                    })
                    .doOnSuccess(dataShieldTokenManagerTokenStatus ->
                            log.debug("Token status received: {}", printJsonObject(dataShieldTokenManagerTokenStatus))
                    )
                    .map(this::replaceTokenManagerId);


        } else {
            throw new DataShieldTokenManagerServiceException("Bridgehead " + bridgehead + " not configured for token manager");
        }
    }

    private String printJsonObject(Object object) {
        try {
            return (object != null) ? objectMapper.writeValueAsString(object) : "EMPTY OBJECT";
        } catch (JsonProcessingException e) {
            return "EXCEPTION WHILE PARSING OBJECT";
        }
    }

    public Mono<DataShieldTokenManagerProjectStatus> fetchProjectStatus(@NotNull Project project, @NotNull ProjectBridgehead bridgehead) {
        if (!isTokenManagerActive) {
            return Mono.just(new DataShieldTokenManagerProjectStatus(project.getCode(), bridgehead.getBridgehead(), DataShieldProjectStatus.INACTIVE));
        }
        Optional<String> tokenManagerId = fetchTokenManagerId(bridgehead);
        if (tokenManagerId.isPresent()) {
            log.debug("Fetching DataSHIELD project status for project {} and bridgehead {}", project, bridgehead);
            String uri = UriComponentsBuilder.fromPath(ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_PROJECT_STATUS)
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_BRIDGEHEAD, tokenManagerId.get())
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_PROJECT_CODE, project)
                    .toUriString();
            return webClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(DataShieldTokenManagerProjectStatus.class)
                    .onErrorResume(exception -> {
                        log.debug(ExceptionUtils.getStackTrace(exception));
                        return Mono.just(new DataShieldTokenManagerProjectStatus(project.getCode(), bridgehead.getBridgehead(), DataShieldProjectStatus.ERROR));
                    })
                    .doOnSuccess(dataShieldTokenManagerProjectStatus -> log.debug("DataSHIELD ProjectCode Status fetched: {}", printJsonObject(dataShieldTokenManagerProjectStatus)))
                    .map(this::replaceTokenManagerId);
        } else {
            throw new DataShieldTokenManagerServiceException("Bridgehead " + bridgehead + " not configured for token manager");
        }
    }

    public Resource fetchAuthenticationScript(Project project, ProjectBridgehead bridgehead) throws DataShieldTokenManagerServiceException {
        if (!isTokenManagerActive) {
            return new ByteArrayResource("Token Manager inactive".getBytes());
        }
        List<String> tokenManagerIds = fetchTokenManagerIds(fetchProjectBridgeheads(project, bridgehead, sessionUser.getEmail()));
        if (!tokenManagerIds.isEmpty()) {
            log.debug("Fetching authentication script for project {} and bridgehead {}", project, bridgehead);
            String authenticationScript = webClient.post().uri(uriBuilder ->
                            uriBuilder.path(ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_SCRIPTS).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new TokenParams(sessionUser.getEmail(), project.getCode(), tokenManagerIds))
                    .accept(MediaType.TEXT_PLAIN).retrieve().bodyToMono(String.class).block();
            log.debug("Authentication script fetched");
            if (!StringUtils.hasText(authenticationScript)) {
                throw new DataShieldTokenManagerServiceException("Script could not be generated for project " + project + " and user " + sessionUser.getEmail());
            }
            return new ByteArrayResource(authenticationScript.getBytes());
        }
        throw new DataShieldTokenManagerServiceException("Script could not be generated for project " + project + " and user " + sessionUser.getEmail());
    }

    public Boolean existsAuthenticationScript(Project project, ProjectBridgehead bridgehead) {
        if (!isTokenManagerActive) {
            return false;
        }
        List<String> tokenManagerIds = fetchTokenManagerIds(fetchProjectBridgeheads(project, bridgehead, sessionUser.getEmail()));
        log.debug("Checking if authentication script exists for project {} and bridgehead {}", project, bridgehead);
        return Boolean.valueOf(webClient.post()
                .uri(uriBuilder ->
                        uriBuilder.path(ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.AUTHENTICATION_SCRIPT_STATUS).build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TokenParams(sessionUser.getEmail(), project.getCode(), tokenManagerIds))
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(result -> log.debug("Exists authentication script: {}", Boolean.valueOf(result)))
                .onErrorResume(exception -> {
                    log.debug(ExceptionUtils.getStackTrace(exception));
                    return Mono.just("false");
                })
                .block());
    }


    public Mono<Void> refreshToken(@NotNull Project project, @NotNull ProjectBridgehead bridgehead, @NotNull String email, Supplier<Mono> ifSuccessMonoSupplier) throws DataShieldTokenManagerServiceException {
        if (!isTokenManagerActive) {
            log.error("Token Manager inactive in project manager. It couldn't refresh token for project {} and bridgehead {} and user {}", project, bridgehead, email);
            return Mono.empty();
        }
        List<String> tokenManagerIds = fetchTokenManagerIds(fetchProjectBridgeheads(project, bridgehead, email));
        if (!tokenManagerIds.isEmpty()) {
            TokenParams tokenParams = new TokenParams(email, project.getCode(), tokenManagerIds);
            String uri = ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_REFRESH_TOKEN;
            log.info("Refreshing DataSHIELD Token for project {}, bridgehead {} and user {}...", project, bridgehead, email);
            return webClient.put()
                    .uri(uri)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(BodyInserters.fromValue(tokenParams))
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(_ -> {
                        log.info("DataSHIELD Token refreshed");
                        notificationService.createNotification(project, bridgehead.getBridgehead(), email,
                                OperationType.REFRESH_DATASHIELD_TOKEN, "Token refreshed", null, null);
                    })
                    .then((ifSuccessMonoSupplier != null) ? ifSuccessMonoSupplier.get() : Mono.empty());
        }
        return Mono.empty();
    }

    public Mono<Void> removeTokens(@NotNull Project project, @NotNull ProjectBridgehead bridgehead, @NotNull String email, Supplier<Mono> ifSuccessMonoSupplier) {
        if (!isTokenManagerActive) {
            log.error("Token Manager inactive in project manager. It couldn't remove tokens for project {} and bridgehead {} and user {}", project, bridgehead, email);
            return Mono.empty();
        }
        Optional<String> tokenManagerId = fetchTokenManagerId(bridgehead);
        if (tokenManagerId.isPresent()) {
            String uri = UriComponentsBuilder.fromPath(ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_TOKENS)
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_BRIDGEHEAD, tokenManagerId.get())
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_PROJECT_CODE, project)
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_EMAIL, email)
                    .toUriString();
            log.info("Removing token for project {}, bridgehead {} and user {}...", project, bridgehead, email);
            return webClient.delete()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .doOnSuccess(_ -> {
                        log.info("DataSHIELD Token removed");
                        notificationService.createNotification(project, bridgehead.getBridgehead(), email,
                                OperationType.REMOVE_DATASHIELD_TOKEN, "Token removed", null, null);
                    })
                    .then((ifSuccessMonoSupplier != null) ? ifSuccessMonoSupplier.get() : Mono.empty());
        } else {
            log.error("Bridgehead {} not configured for token manager", bridgehead);
            return Mono.empty();
        }
    }

    public Mono<Void> removeProjectAndTokens(@NotNull Project project, @NotNull ProjectBridgehead bridgehead) {
        if (!isTokenManagerActive) {
            log.error("Token Manager inactive in project manager. It cannot remove tokens for project {} and bridgehead {}", project, bridgehead);
            return Mono.empty();
        }
        Optional<String> tokenManagerId = fetchTokenManagerId(bridgehead);
        if (tokenManagerId.isPresent()) {
            String uri = UriComponentsBuilder.fromPath(ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_PROJECT)
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_BRIDGEHEAD, tokenManagerId.get())
                    .queryParam(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_PROJECT_CODE, project)
                    .toUriString();
            log.info("Removing token for project {}, bridgehead {}", project, bridgehead);
            return webClient.delete()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .doOnSuccess(_ -> {
                        log.debug("ProjectCode and token removed");
                        notificationService.createNotification(project, bridgehead.getBridgehead(), null,
                                OperationType.REMOVE_DATASHIELD_TOKEN, "Token removed", null, null);
                    })
                    .then();
        } else {
            log.info("Bridgehead {} not configured for token manager", bridgehead);
            return Mono.empty();
        }
    }

    private List<String> fetchTokenManagerIds(List<ProjectBridgehead> bridgeheads) {
        return bridgeheads.stream().map(bridgehead -> {
            Optional<String> tokenManagerId = fetchTokenManagerId(bridgehead);
            return tokenManagerId.orElse(null);
        }).filter(ObjectUtils::isNotEmpty).toList();
    }

    private Optional<String> fetchTokenManagerId(ProjectBridgehead bridgehead) {
        return bridgeheadsConfiguration.getTokenManagerId(bridgehead.getBridgehead());
    }

    private DataShieldTokenManagerProjectStatus replaceTokenManagerId(DataShieldTokenManagerProjectStatus dataShieldTokenManagerProjectStatus) {
        return (dataShieldTokenManagerProjectStatus != null) ?
                new DataShieldTokenManagerProjectStatus(
                        dataShieldTokenManagerProjectStatus.projectCode(),
                        bridgeheadsConfiguration.fetchBridgeheadForTokenManagerId(dataShieldTokenManagerProjectStatus.bridgehead()),
                        dataShieldTokenManagerProjectStatus.projectStatus()
                )
                : null;
    }

    private DataShieldTokenManagerTokenStatus replaceTokenManagerId(DataShieldTokenManagerTokenStatus dataShieldTokenManagerTokenStatus) {
        return (dataShieldTokenManagerTokenStatus != null) ?
                new DataShieldTokenManagerTokenStatus(
                        dataShieldTokenManagerTokenStatus.projectCode(),
                        bridgeheadsConfiguration.fetchBridgeheadForTokenManagerId(dataShieldTokenManagerTokenStatus.bridgehead()),
                        dataShieldTokenManagerTokenStatus.email(),
                        dataShieldTokenManagerTokenStatus.createdAt(),
                        dataShieldTokenManagerTokenStatus.projectStatus(),
                        dataShieldTokenManagerTokenStatus.tokenStatus()
                )
                : null;
    }

}
