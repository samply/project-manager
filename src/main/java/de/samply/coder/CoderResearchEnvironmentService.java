package de.samply.coder;

import de.samply.annotations.ModuleComponent;
import de.samply.app.ProjectManagerConst;
import de.samply.coder.request.CreateRequestBody;
import de.samply.coder.request.Response;
import de.samply.coder.request.TransitionRequestBody;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.model.ProjectCoder;
import de.samply.db.repository.ProjectCoderRepository;
import de.samply.modules.OptionalModule;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.project.ProjectType;
import de.samply.researchenvironment.ResearchEnvironmentService;
import de.samply.utils.WebClientFactory;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Research environment implemented with Coder: creates and deletes the users' workspaces through the Coder API.
 */
@Slf4j
@Service
@ModuleComponent(OptionalModule.RESEARCH_ENVIRONMENT)
// Only for the IDE: at runtime only this or DisabledResearchEnvironmentService exists (module condition). IntelliJ does not
// evaluate the condition and would report two candidates for ResearchEnvironmentService.
@Primary
public class CoderResearchEnvironmentService implements ResearchEnvironmentService {

    private final ProjectCoderRepository projectCoderRepository;
    private final NotificationService notificationService;
    private final CoderConfiguration coderConfiguration;
    private final WebClient webClient;

    private final String coderCreatePath;
    private final String coderDeletePath;
    private final String coderSessionToken;
    private final String researchEnvironmentUrl;
    private final String coderBeamIdSuffix;
    private final String testCoderFileBeamId;

    private final int coderWorkspaceMaxLength;


    public CoderResearchEnvironmentService(
            ProjectCoderRepository projectCoderRepository,
            NotificationService notificationService,
            CoderConfiguration coderConfiguration,
            WebClientFactory webClientFactory,
            @Value(ProjectManagerConst.CODER_BASE_URL_SV) String coderBaseUrl,
            @Value(ProjectManagerConst.CODER_ORGANISATION_ID_SV) String coderOrganizationId,
            @Value(ProjectManagerConst.CODER_CREATE_PATH_SV) String coderCreatePath,
            @Value(ProjectManagerConst.CODER_DELETE_PATH_SV) String coderDeletePath,
            @Value(ProjectManagerConst.CODER_SESSION_TOKEN_SV) String coderSessionToken,
            @Value(ProjectManagerConst.CODER_WORKSPACE_NAME_MAX_LENGTH_SV) int coderWorkspaceMaxLength,
            @Value(ProjectManagerConst.CODER_BEAM_ID_SUFFIX_SV) String coderBeamIdSuffix,
            @Value(ProjectManagerConst.CODER_TEST_FILE_BEAM_ID_SV) String testCoderFileBeamId) {
        this.projectCoderRepository = projectCoderRepository;
        this.notificationService = notificationService;
        this.coderConfiguration = coderConfiguration;
        this.coderSessionToken = coderSessionToken;
        this.coderWorkspaceMaxLength = coderWorkspaceMaxLength;
        Map<String, String> pathVariables = Map.of(ProjectManagerConst.CODER_ORGANISATION_ID, coderOrganizationId);
        this.coderCreatePath = replaceVariablesInPath(coderCreatePath, pathVariables);
        this.coderDeletePath = replaceVariablesInPath(coderDeletePath, pathVariables);

        this.webClient = webClientFactory.createWebClient(coderBaseUrl);
        this.researchEnvironmentUrl = coderBaseUrl;
        this.coderBeamIdSuffix = coderBeamIdSuffix;
        this.testCoderFileBeamId = testCoderFileBeamId;
    }

    private String replaceVariablesInPath(String path, Map<String, String> pathVariables) {
        AtomicReference<String> result = new AtomicReference<>(path);
        if (path != null) {
            pathVariables.entrySet().stream().filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .forEach(entry -> result.set(result.get().replace(fetchVariableExpression(entry.getKey()), entry.getValue())));
        }

        return result.get();
    }

    private String fetchVariableExpression(String variable) {
        return "{" + variable + "}";
    }

    @Override
    public Mono<ProjectCoder> createWorkspace(@NotNull ProjectBridgeheadUser projectBridgeheadUser) {
        if (projectCoderRepository.findFirstByProjectBridgeheadUserAndDeletedAtIsNullOrderByCreatedAtDesc(projectBridgeheadUser).isEmpty()) {
            ProjectCoder projectCoder = generateProjectCoder(projectBridgeheadUser);
            CreateRequestBody createRequestBody = generateCreateRequestBody(projectCoder);
            return createWorkspace(projectCoder, createRequestBody).flatMap(response -> {
                projectCoder.setWorkspaceId(response.getLatestBuild().getWorkspaceId());
                projectCoderRepository.save(projectCoder);
                notificationService.createNotification(projectBridgeheadUser.getProjectBridgehead().getProject(),
                        projectBridgeheadUser.getProjectBridgehead().getBridgehead(), projectBridgeheadUser.getEmail(),
                        OperationType.CREATE_CODER_WORKSPACE,
                        "Created workspace " + projectCoder.getWorkspaceId(), null, null);
                return Mono.just(projectCoder);
            });
        }
        return Mono.empty();
    }

    private Mono<Response> createWorkspace(ProjectCoder projectCoder, CreateRequestBody createRequestBody) {
        log.info("Creating workspace in Coder for project {} and user {}...", projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode(), projectCoder.getProjectBridgeheadUser().getEmail());
        return this.webClient.post()
                .uri(uriBuilder -> uriBuilder.path(ProjectManagerConst.CODER_API_PATH).path(
                        replaceVariablesInPath(coderCreatePath, Map.of(ProjectManagerConst.CODER_MEMBER_ID,
                                fetchCoderMemberId(projectCoder.getProjectBridgeheadUser().getEmail())))).build())
                .header(ProjectManagerConst.CODER_SESSION_TOKEN_HEADER, coderSessionToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequestBody)
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().equals(HttpStatus.OK) || clientResponse.statusCode().equals(HttpStatus.CREATED)) {
                        log.info("Coder workspace created");
                        return clientResponse.bodyToMono(Response.class);
                    } else {
                        log.error("Http error {} creating workspace in Coder for user {} in project {}", clientResponse.statusCode(), projectCoder.getProjectBridgeheadUser().getEmail(), projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode());
                        return clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            logError(errorBody);
                            return Mono.error(new RuntimeException(errorBody));
                        });
                    }
                });
    }

    private void logError(String error) {
        log.error("Error: {}", error);
    }

    private ProjectCoder generateProjectCoder(ProjectBridgeheadUser projectBridgeheadUser) {
        ProjectCoder projectCoder = new ProjectCoder();
        projectCoder.setProjectBridgeheadUser(projectBridgeheadUser);
        projectCoder.setAppId(fetchCoderAppId(projectBridgeheadUser));
        projectCoder.setAppSecret(generateAppSecret());
        return projectCoder;
    }

    @Override
    public Flux<ProjectCoder> deleteAllWorkspaces(@NotNull String projectCode, @NotNull String bridgehead) {
        return Flux.fromIterable(projectCoderRepository.findDistinctByProjectCodeAndBridgeheadIfNotDeleted(projectCode, bridgehead))
                .flatMap(this::deleteWorkspace);
    }

    @Override
    public Mono<ProjectCoder> deleteWorkspace(@NotNull ProjectBridgeheadUser user) {
        return projectCoderRepository.findFirstByProjectBridgeheadUserAndDeletedAtIsNullOrderByCreatedAtDesc(user)
                .map(this::deleteWorkspace)
                .orElseGet(Mono::empty);
    }

    @Override
    public Mono<ProjectCoder> deleteWorkspace(@NotNull ProjectCoder projectCoder) {
        return deleteWorkspaceInCoder(projectCoder).doOnSuccess(_ -> {
            projectCoder.setDeletedAt(Instant.now());
            projectCoderRepository.save(projectCoder);
            notificationService.createNotification(projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject(),
                    projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(), projectCoder.getProjectBridgeheadUser().getEmail(), OperationType.DELETE_CODER_WORKSPACE,
                    "Deleted workspace " + projectCoder.getWorkspaceId(), null, null);
        }).flatMap(_ -> Mono.just(projectCoder));
    }

    private Mono<Response> deleteWorkspaceInCoder(ProjectCoder projectCoder) {
        log.info("Deleting coder workspace for project {} and user {}...", projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode(), projectCoder.getProjectBridgeheadUser().getEmail());
        return this.webClient.post()
                .uri(uriBuilder -> uriBuilder.path(ProjectManagerConst.CODER_API_PATH)
                        .path(replaceVariablesInPath(coderDeletePath, Map.of(ProjectManagerConst.CODER_WORKSPACE_ID, projectCoder.getWorkspaceId()))).build())
                .header(ProjectManagerConst.CODER_SESSION_TOKEN_HEADER, coderSessionToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TransitionRequestBody(ProjectManagerConst.CODER_DELETE_TRANSITION))
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().equals(HttpStatus.OK) || clientResponse.statusCode().equals(HttpStatus.CREATED)) {
                        log.info("Coder workspace deleted");
                        return clientResponse.bodyToMono(Response.class);
                    } else {
                        log.error("Http error {} deleting workspace in Coder for user {} in project {}", clientResponse.statusCode(), projectCoder.getProjectBridgeheadUser().getEmail(), projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode());
                        return clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            logError(errorBody);
                            return Mono.error(new RuntimeException(errorBody));
                        });
                    }
                });
    }

    private CreateRequestBody generateCreateRequestBody(ProjectCoder projectCoder) {
        CreateRequestBody createRequestBody = coderConfiguration.cloneCreateRequestBody(ProjectType.RESEARCH_ENVIRONMENT);
        createRequestBody.setName(projectCoder.getAppId());
        replaceParameterValues(createRequestBody, projectCoder);
        return createRequestBody;
    }

    private void replaceParameterValues(CreateRequestBody createRequestBody, ProjectCoder projectCoder) {
        Arrays.stream(createRequestBody.getRichParameterValues()).forEach(parameter -> CoderParam.replaceParameters(parameter, projectCoder));
    }

    public String fetchCoderAppId(@NotNull ProjectBridgeheadUser projectBridgeheadUser) {
        String email = projectBridgeheadUser.getEmail().substring(0, projectBridgeheadUser.getEmail().indexOf("@")).replaceAll("[^a-zA-Z0-9]", "");
        // Coder workspace names and Beam app IDs only accept alphanumeric characters here
        String projectCode = projectBridgeheadUser.getProjectBridgehead().getProject().getCode().replaceAll("[^a-zA-Z0-9]", "");
        String state = projectBridgeheadUser.getProjectBridgehead().getProject().getState().toString().toLowerCase().substring(0, 3); // Only three first characters

        String coderAppId = email + projectCode + state;
        // Check if the coderAppId exceeds the maximum allowed length
        if (coderAppId.length() > coderWorkspaceMaxLength) {
            // Calculate the surplus length that needs to be trimmed
            int surplus = coderAppId.length() - coderWorkspaceMaxLength;
            // Reduce the projectCode length if necessary, prioritizing keeping at least half of it
            int maxProjectCodeLength = (projectCode.length() / 2 > surplus)
                    ? projectCode.length() / 2
                    : projectCode.length() - surplus;
            // Trim the projectCode to the calculated length, keeping its most distinctive end
            projectCode = projectCode.substring(projectCode.length() - maxProjectCodeLength);
            // Rebuild the coderAppId with the reduced projectCode
            coderAppId = email + projectCode + state;
            // If the coderAppId is still too long, reduce the email length
            if (coderAppId.length() > coderWorkspaceMaxLength) {
                // Recalculate the surplus length after trimming projectCode
                surplus = coderAppId.length() - coderWorkspaceMaxLength;
                // Reduce the email length from the right, ensuring at least 1 character remains
                int newEmailLength = Math.max(1, email.length() - surplus);
                email = email.substring(0, newEmailLength);
                // Final rebuild of the coderAppId
                coderAppId = email + projectCode + state;
            }
        }
        return coderAppId;
    }

    private String generateAppSecret() {
        return UUID.randomUUID().toString();
    }

    private String fetchCoderMemberId(@NotNull String email) {
        return email.substring(0, email.indexOf("@")).replace(".", "");
    }

    @Override
    public boolean existsUserResearchEnvironmentWorkspace(@NotNull Project project, @NotNull ProjectBridgehead bridgehead) {
        List<ProjectCoder> projectCoder = this.projectCoderRepository
                .findByBridgeheadAndProjectCodeOrderedByCreatedAtDesc(bridgehead.getBridgehead(), project.getCode());
        return !projectCoder.isEmpty() && projectCoder.getFirst().getDeletedAt() == null;
    }


    @Override
    public boolean existsUserResearchEnvironmentWorkspace(@NotNull ProjectBridgeheadUser projectBridgeheadUser) {
        List<ProjectCoder> projectCoders = this.projectCoderRepository.findByProjectBridgeheadUserOrderByCreatedAtDesc(projectBridgeheadUser);
        return !projectCoders.isEmpty() && projectCoders.getFirst().getDeletedAt() == null;
    }

    @Override
    public List<ProjectCoder> fetchCoderOrderedByCreatedAtDesc(String projectCode, String bridgehead, String email) {
        return projectCoderRepository.findByBridgeheadAndProjectCodeAndEmailOrderedByCreatedAtDesc(bridgehead, projectCode, email);
    }

    @Override
    public void saveCoder(ProjectCoder projectCoder) {
        projectCoderRepository.save(projectCoder);
    }

    @Override
    public Optional<String> fetchResearchEnvironmentUrl() {
        return Optional.of(researchEnvironmentUrl);
    }

    // Moved from ExporterService: the exporter sends the export file to the workspace's Beam app
    @Override
    public String fetchFileBeamId(@NotNull ProjectCoder projectCoder) {
        return Objects.requireNonNullElseGet(testCoderFileBeamId, () -> projectCoder.getAppId() + ((coderBeamIdSuffix.startsWith(".")) ? "" : ".") + coderBeamIdSuffix);
    }

}
