package de.samply.token;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.security.SessionUser;
import de.samply.token.dto.OpalStatus;
import de.samply.token.dto.TokenParams;
import de.samply.user.UserServiceException;
import de.samply.user.roles.ProjectRole;
import de.samply.utils.WebClientFactory;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class TokenManagerService {

    private final SessionUser sessionUser;
    private final WebClientFactory webClientFactory;
    private final WebClient webClient;
    private final ProjectRepository projectRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private final NotificationService notificationService;

    public TokenManagerService(SessionUser sessionUser,
                               WebClientFactory webClientFactory,
                               @Value(ProjectManagerConst.TOKEN_MANAGER_URL_SV) String tokenManagerUrl,
                               ProjectRepository projectRepository,
                               ProjectBridgeheadRepository projectBridgeheadRepository,
                               ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                               NotificationService notificationService) {
        this.sessionUser = sessionUser;
        this.webClientFactory = webClientFactory;
        this.projectRepository = projectRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.notificationService = notificationService;
        this.webClient = webClientFactory.createWebClient(tokenManagerUrl);
    }

    public void generateTokensAndProjectsInOpal(@NotNull String projectCode, @NotNull String bridgehead, Runnable errorRunnable) throws TokenManagerServiceException {
        List<String> bridgeheads = fetchSessionUserProjectBridgeheads(projectCode, bridgehead);
        AtomicInteger retryCount = new AtomicInteger(0);
        webClient.post().uri(uriBuilder -> uriBuilder.path(ProjectManagerConst.TOKEN_MANAGER_TOKENS).build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(BodyInserters.fromValue(new TokenParams(sessionUser.getEmail(), projectCode, bridgeheads)))
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
                                notificationService.createNotification(projectCode, tempBridgehead, sessionUser.getEmail(),
                                        OperationType.GENERATE_TOKEN, "Error generating token", error));
                        errorRunnable.run();
                    }
                })
                .subscribe(result -> bridgeheads.forEach(tempBridgehead ->
                        notificationService.createNotification(projectCode, tempBridgehead, sessionUser.getEmail(),
                                OperationType.GENERATE_TOKEN, "Token generated successfully in Token Manager", null)));
    }

    private List<String> fetchSessionUserProjectBridgeheads(String projectCode, String bridgehead) throws TokenManagerServiceException {
        Project project = fetchProject(projectCode);
        Optional<ProjectBridgeheadUser> projectBridgeheadUser = projectBridgeheadUserRepository.getFirstByEmailAndProjectBridgehead_ProjectAndProjectBridgehead_BridgeheadOrderByModifiedAtDesc(sessionUser.getEmail(), project, bridgehead);
        if (projectBridgeheadUser.isEmpty()) {
            throw new TokenManagerServiceException("User " + sessionUser.getEmail() + " with token manager rights not found for project " + projectCode);
        }
        ProjectRole userProjectRole = projectBridgeheadUser.get().getProjectRole();
        if (userProjectRole == ProjectRole.DEVELOPER || userProjectRole == ProjectRole.PILOT) {
            return List.of(bridgehead);
        } else if (userProjectRole == ProjectRole.FINAL) {
            return projectBridgeheadRepository.findByProject(project).stream().map(projectBridgehead -> projectBridgehead.getBridgehead()).toList();
        } else {
            throw new TokenManagerServiceException("Role " + userProjectRole + " of user " + sessionUser.getEmail() + " not supported");
        }
    }

    private Project fetchProject(String projectCode) throws TokenManagerServiceException {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new TokenManagerServiceException("Project " + projectCode + " not found");
        }
        return project.get();
    }

    public OpalStatus fetchProjectStatus(@NotNull String projectCode, @NotNull String bridgehead) {
        //TODO add bridgehead to request
        return webClient.get()
                .uri(ProjectManagerConst.TOKEN_MANAGER_PROJECT_STATUS + '/' + projectCode + ProjectManagerConst.TOKEN_MANAGER_PROJECT_STATUS_SUFFIX)
                .accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(OpalStatus.class).block();
    }

    public Resource fetchAuthenticationScript(String projectCode) throws UserServiceException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ProjectManagerConst.TOKEN_MANAGER_SCRIPTS)
                .queryParam("project", projectCode)
                .queryParam("user", sessionUser.getEmail());
        String uri = builder.toUriString();
        String authenticationScript = webClient.get().uri(uri)
                .accept(MediaType.TEXT_PLAIN).retrieve().bodyToMono(String.class).block();
        if (!StringUtils.hasText(authenticationScript)) {
            throw new UserServiceException("Script could not be generated for project " + projectCode + " and user " + sessionUser.getEmail());
        }
        return new ByteArrayResource(authenticationScript.getBytes());
    }

    public void refreshToken(@NotNull String projectCode, @NotNull String bridgehead) {
        //TODO
    }

    public void recreateProject(@NotNull String projectCode, @NotNull String bridgehead) {
        //TODO
    }

    public void removeProjectAndTokens(@NotNull String projectCode) {
        //TODO
    }

}
