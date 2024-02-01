package de.samply.token;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.security.SessionUser;
import de.samply.token.dto.DataShieldTokenManagerProjectStatus;
import de.samply.token.dto.DataShieldTokenManagerTokenStatus;
import de.samply.token.dto.TokenParams;
import de.samply.user.roles.ProjectRole;
import de.samply.utils.WebClientFactory;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
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
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

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

    public DataShieldTokenManagerService(SessionUser sessionUser,
                                         WebClientFactory webClientFactory,
                                         @Value(ProjectManagerConst.TOKEN_MANAGER_URL_SV) String tokenManagerUrl,
                                         ProjectRepository projectRepository,
                                         ProjectBridgeheadRepository projectBridgeheadRepository,
                                         ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                                         NotificationService notificationService,
                                         BridgeheadConfiguration bridgeheadConfiguration) {
        this.sessionUser = sessionUser;
        this.webClientFactory = webClientFactory;
        this.projectRepository = projectRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.notificationService = notificationService;
        this.bridgeheadConfiguration = bridgeheadConfiguration;
        this.webClient = webClientFactory.createWebClient(tokenManagerUrl);
    }

    public void generateTokensInOpal(@NotNull String projectCode, @NotNull String bridgehead, @NotNull String email) throws DataShieldTokenManagerServiceException {
        List<String> bridgeheads = fetchProjectBridgeheads(projectCode, bridgehead, email);
        List<String> tokenManagerIds = fetchTokenManagerIds(bridgeheads);
        AtomicInteger retryCount = new AtomicInteger(0);
        webClient.post().uri(uriBuilder ->
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
                .subscribe(result -> bridgeheads.forEach(tempBridgehead ->
                        notificationService.createNotification(projectCode, tempBridgehead, email,
                                OperationType.CREATE_DATASHIELD_TOKEN, "Token generated successfully in Token Manager", null, null)));
    }

    private List<String> fetchProjectBridgeheads(String projectCode, String bridgehead, String email) throws DataShieldTokenManagerServiceException {
        Project project = fetchProject(projectCode);
        Optional<ProjectBridgeheadUser> projectBridgeheadUser = projectBridgeheadUserRepository.getFirstByEmailAndProjectBridgehead_ProjectAndProjectBridgehead_BridgeheadOrderByModifiedAtDesc(email, project, bridgehead);
        if (projectBridgeheadUser.isEmpty()) {
            throw new DataShieldTokenManagerServiceException("User " + email + " with token manager rights not found for project " + projectCode);
        }
        ProjectRole userProjectRole = projectBridgeheadUser.get().getProjectRole();
        if (userProjectRole == ProjectRole.DEVELOPER || userProjectRole == ProjectRole.PILOT) {
            return List.of(bridgehead);
        } else if (userProjectRole == ProjectRole.FINAL) {
            return projectBridgeheadRepository.findByProject(project).stream().map(projectBridgehead -> projectBridgehead.getBridgehead()).toList();
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
    public DataShieldTokenManagerTokenStatus fetchTokenStatus(@NotNull String projectCode, @NotNull String bridgehead, @NotNull String email) {
        return replaceTokenManagerId(webClient.get()
                .uri(ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_TOKENS + '/' + email + ProjectManagerConst.TOKEN_MANAGER_PROJECT_STATUS_SUFFIX + '/' + fetchTokenManagerId(bridgehead))
                .accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(DataShieldTokenManagerTokenStatus.class).block());
    }

    public DataShieldTokenManagerProjectStatus fetchProjectStatus(@NotNull String projectCode, @NotNull String bridgehead) {
        return replaceTokenManagerId(webClient.get()
                .uri(ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_PROJECT_STATUS + '/' + projectCode + ProjectManagerConst.TOKEN_MANAGER_PROJECT_STATUS_SUFFIX + '/' + fetchTokenManagerId(bridgehead))
                .accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(DataShieldTokenManagerProjectStatus.class).block());
    }

    public Resource fetchAuthenticationScript(String projectCode, String bridgehead) throws DataShieldTokenManagerServiceException {
        List<String> tokenManagerIds = fetchTokenManagerIds(fetchProjectBridgeheads(projectCode, bridgehead, sessionUser.getEmail()));
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

    public void refreshToken(@NotNull String projectCode, @NotNull String bridgehead, @NotNull String email) throws DataShieldTokenManagerServiceException {
        List<String> bridgeheads = fetchTokenManagerIds(fetchProjectBridgeheads(projectCode, bridgehead, email));
        TokenParams tokenParams = new TokenParams(email, projectCode, bridgeheads);
        String uri = ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_REFRESH_TOKEN;

        webClient.put()
                .uri(uriBuilder -> uriBuilder.path(uri).build())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(BodyInserters.fromValue(tokenParams))
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(s -> notificationService.createNotification(projectCode, bridgehead, email,
                        OperationType.REFRESH_DATASHIELD_TOKEN, "Token refreshed", null, null));
    }

    public void removeTokens(@NotNull String projectCode, @NotNull String bridgehead, @NotNull String email) {
        String uri = ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_TOKENS
                + '/' + email + '/' + fetchTokenManagerId(bridgehead);

        webClient.delete()
                .uri(uriBuilder -> uriBuilder.path(uri).build())
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(s -> notificationService.createNotification(projectCode, bridgehead, email,
                        OperationType.REMOVE_DATASHIELD_TOKEN, "Token removed", null, null));
    }

    public void removeProjectAndTokens(@NotNull String projectCode, @NotNull String bridgehead) {
        String uri = ProjectManagerConst.TOKEN_MANAGER_ROOT + ProjectManagerConst.TOKEN_MANAGER_PROJECT_STATUS
                + '/' + projectCode + '/' + fetchTokenManagerId(bridgehead);

        webClient.delete()
                .uri(uriBuilder -> uriBuilder.path(uri).build())
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(s -> notificationService.createNotification(projectCode, bridgehead, null,
                        OperationType.REMOVE_DATASHIELD_TOKEN, "Token removed", null, null));
    }

    private List<String> fetchTokenManagerIds(List<String> bridgeheads) {
        return bridgeheads.stream().map(this::fetchTokenManagerId).toList();
    }

    private String fetchTokenManagerId(String bridgehead) {
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