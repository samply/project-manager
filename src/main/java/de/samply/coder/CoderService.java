package de.samply.coder;

import de.samply.app.ProjectManagerConst;
import de.samply.coder.request.CreateRequestBody;
import de.samply.coder.request.CreateRequestParameter;
import de.samply.coder.request.Response;
import de.samply.coder.request.TransitionRequestBody;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.model.ProjectCoder;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.db.repository.ProjectCoderRepository;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.security.SessionUser;
import de.samply.utils.WebClientFactory;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class CoderService {

    private final boolean coderEnabled;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private final ProjectCoderRepository projectCoderRepository;
    private final NotificationService notificationService;
    private final SessionUser sessionUser;
    private final String coderTemplateVersionId;
    private final String coderCreatePath;
    private final String coderDeletePath;
    private final String enableJupyterLab;
    private final String enableRstudio;
    private final String enableVsCodeServer;
    private final String dotFilesUrl;
    private final String enableFileReceiver;
    private final String coderSessionToken;
    private final String researchEnvironmentUrl;

    private final WebClient webClient;


    public CoderService(
            ProjectCoderRepository projectCoderRepository,
            NotificationService notificationService,
            ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
            SessionUser sessionUser,
            @Value(ProjectManagerConst.ENABLE_CODER_SV) boolean coderEnabled,
            @Value(ProjectManagerConst.CODER_BASE_URL_SV) String coderBaseUrl,
            @Value(ProjectManagerConst.CODER_ORGANISATION_ID_SV) String coderOrganizationId,
            @Value(ProjectManagerConst.CODER_TEMPLATE_VERSION_ID_SV) String coderTemplateVersionId,
            @Value(ProjectManagerConst.CODER_CREATE_PATH_SV) String coderCreatePath,
            @Value(ProjectManagerConst.CODER_DELETE_PATH_SV) String coderDeletePath,
            @Value(ProjectManagerConst.CODER_ENABLE_JUPYTER_LAB_PARAM_VALUE_SV) String enableJupyterLab,
            @Value(ProjectManagerConst.CODER_ENABLE_RSTUDIO_PARAM_VALUE_SV) String enableRstudio,
            @Value(ProjectManagerConst.CODER_ENABLE_VS_CODE_SERVER_PARAM_VALUE_SV) String enableVsCodeServer,
            @Value(ProjectManagerConst.CODER_DOTFILES_URL_PARAM_VALUE_SV) String dotFilesUrl,
            @Value(ProjectManagerConst.CODER_ENABLE_FILE_RECEIVER_PARAM_VALUE_SV) String enableFileReceiver,
            @Value(ProjectManagerConst.CODER_SESSION_TOKEN_SV) String coderSessionToken,
            WebClientFactory webClientFactory) {
        this.coderEnabled = coderEnabled;
        this.projectCoderRepository = projectCoderRepository;
        this.notificationService = notificationService;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.sessionUser = sessionUser;
        this.enableJupyterLab = enableJupyterLab;
        this.enableRstudio = enableRstudio;
        this.enableVsCodeServer = enableVsCodeServer;
        this.dotFilesUrl = dotFilesUrl;
        this.enableFileReceiver = enableFileReceiver;
        this.coderSessionToken = coderSessionToken;
        Map<String, String> pathVariables = Map.of(ProjectManagerConst.CODER_ORGANISATION_ID, coderOrganizationId);
        this.coderCreatePath = replaceVariablesInPath(coderCreatePath, pathVariables);
        this.coderDeletePath = replaceVariablesInPath(coderDeletePath, pathVariables);

        this.coderTemplateVersionId = coderTemplateVersionId;
        this.webClient = webClientFactory.createWebClient(coderBaseUrl);
        this.researchEnvironmentUrl = coderBaseUrl;
    }

    private String replaceVariablesInPath(String path, Map<String, String> pathVariables) {
        AtomicReference<String> result = new AtomicReference<>(path);
        if (path != null) {
            pathVariables.entrySet().stream().filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .forEach(entry -> result.set(result.get().replace(fetchVariableExpresion(entry.getKey()), entry.getValue())));
        }

        return result.get();
    }

    private String fetchVariableExpresion(String variable) {
        return "{" + variable + "}";
    }

    public Mono<Void> createWorkspace(String email, String projectCode) throws CoderServiceException {
        Optional<ProjectBridgeheadUser> user = projectBridgeheadUserRepository.getFirstByEmailAndProjectBridgehead_ProjectCodeOrderByModifiedAtDesc(email, projectCode);
        if (user.isEmpty()) {
            log.error("User " + email + " for project " + projectCode + " not found");
            return Mono.empty();
        }
        return createWorkspace(user.get());
    }

    public Mono<Void> createWorkspace(@NotNull ProjectBridgeheadUser projectBridgeheadUser) {
        if (coderEnabled) {
            if (projectCoderRepository.findFirstByProjectBridgeheadUserAndDeletedAtIsNullOrderByCreatedAtDesc(projectBridgeheadUser).isEmpty()) {
                ProjectCoder projectCoder = generateProjectCoder(projectBridgeheadUser);
                CreateRequestBody createRequestBody = generateCreateRequestBody(projectCoder);
                return createWorkspace(projectCoder, createRequestBody).flatMap(response -> {
                    projectCoder.setWorkspaceId(response.getLatestBuild().getWorkspaceId());
                    projectCoderRepository.save(projectCoder);
                    notificationService.createNotification(projectBridgeheadUser.getProjectBridgehead().getProject().getCode(),
                            projectBridgeheadUser.getProjectBridgehead().getBridgehead(), projectBridgeheadUser.getEmail(),
                            OperationType.CREATE_CODER_WORKSPACE,
                            "Created workspace " + projectCoder.getWorkspaceId(), null, null);
                    return Mono.empty();
                }).then();

            }
        }
        return Mono.empty();
    }

    private Mono<Response> createWorkspace(ProjectCoder projectCoder, CreateRequestBody createRequestBody) {
        return this.webClient.post()
                .uri(uriBuilder -> uriBuilder.path(ProjectManagerConst.CODER_API_PATH).path(
                        replaceVariablesInPath(coderCreatePath, Map.of(ProjectManagerConst.CODER_MEMBER_ID,
                                fetchCoderMemberId(projectCoder.getProjectBridgeheadUser().getEmail())))).build())
                .header(ProjectManagerConst.CODER_SESSION_TOKEN_HEADER, coderSessionToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequestBody)
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().equals(HttpStatus.OK) || clientResponse.statusCode().equals(HttpStatus.CREATED)) {
                        return clientResponse.bodyToMono(Response.class);
                    } else {
                        log.error("Http error " + clientResponse.statusCode() + " creating workspace in Coder for user "
                                + projectCoder.getProjectBridgeheadUser().getEmail() + " in project " +
                                projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode());
                        return clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Error: {}", errorBody);
                            return Mono.error(new RuntimeException(errorBody));
                        });
                    }
                });
    }

    private ProjectCoder generateProjectCoder(ProjectBridgeheadUser projectBridgeheadUser) {
        ProjectCoder projectCoder = new ProjectCoder();
        projectCoder.setProjectBridgeheadUser(projectBridgeheadUser);
        projectCoder.setAppId(fetchCoderAppId(projectBridgeheadUser));
        projectCoder.setAppSecret(generateAppSecret());
        return projectCoder;
    }

    public Mono<Void> deleteWorkspace(@NotNull String email, @NotNull String projectCode) throws CoderServiceException {
        Optional<ProjectBridgeheadUser> user = projectBridgeheadUserRepository.getFirstByEmailAndProjectBridgehead_ProjectCodeOrderByModifiedAtDesc(email, projectCode);
        if (user.isEmpty()) {
            log.error("User " + email + " for project " + projectCode + " not found");
            return Mono.empty();
        }
        return deleteWorkspace(user.get());
    }

    public Mono<Void> deleteWorkspace(@NotNull ProjectBridgeheadUser user) {
        if (coderEnabled) {
            Optional<ProjectCoder> projectCoder = projectCoderRepository.findFirstByProjectBridgeheadUserAndDeletedAtIsNullOrderByCreatedAtDesc(user);
            if (projectCoder.isPresent()) {
                return deleteWorkspace(projectCoder.get()).doOnSuccess(response -> {
                    projectCoder.get().setDeletedAt(Instant.now());
                    projectCoderRepository.save(projectCoder.get());
                    notificationService.createNotification(user.getProjectBridgehead().getProject().getCode(),
                            user.getProjectBridgehead().getBridgehead(), user.getEmail(), OperationType.DELETE_CODER_WORKSPACE,
                            "Deleted workspace " + projectCoder.get().getWorkspaceId(), null, null);
                }).then();
            };
        }
        return Mono.empty();
    }

    private Mono<Response> deleteWorkspace(ProjectCoder projectCoder) {
        return this.webClient.post()
                .uri(uriBuilder -> uriBuilder.path(ProjectManagerConst.CODER_API_PATH)
                        .path(replaceVariablesInPath(coderDeletePath, Map.of(ProjectManagerConst.CODER_WORKSPACE_ID, projectCoder.getWorkspaceId()))).build())
                .header(ProjectManagerConst.CODER_SESSION_TOKEN_HEADER, coderSessionToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TransitionRequestBody(ProjectManagerConst.CODER_DELETE_TRANSITION))
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().equals(HttpStatus.OK) || clientResponse.statusCode().equals(HttpStatus.CREATED)) {
                        return clientResponse.bodyToMono(Response.class);
                    } else {
                        log.error("Http error " + clientResponse.statusCode() + " deleting workspace in Coder for user "
                                + projectCoder.getProjectBridgeheadUser().getEmail() + " in project " +
                                projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode());
                        return clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Error: {}", errorBody);
                            return Mono.error(new RuntimeException(errorBody));
                        });
                    }
                });
    }

    private CreateRequestBody generateCreateRequestBody(ProjectCoder projectCoder) {
        CreateRequestBody createRequestBody = new CreateRequestBody();
        createRequestBody.setTemplateVersionId(coderTemplateVersionId);
        createRequestBody.setName(projectCoder.getAppId());
        addRichParameterValues(createRequestBody, projectCoder);
        return createRequestBody;
    }

    private void addRichParameterValues(CreateRequestBody createRequestBody, ProjectCoder projectCoder) {
        List<CreateRequestParameter> createRequestParameters = List.of(
                new CreateRequestParameter(ProjectManagerConst.CODER_ENABLE_RSTUDIO_PARAM_KEY, enableRstudio),
                new CreateRequestParameter(ProjectManagerConst.CODER_ENABLE_JUPYTER_LAB_PARAM_KEY, enableJupyterLab),
                new CreateRequestParameter(ProjectManagerConst.CODER_ENABLE_VS_CODE_SERVER_PARAM_KEY, enableVsCodeServer),
                new CreateRequestParameter(ProjectManagerConst.CODER_DOTFILES_URL_PARAM_KEY, dotFilesUrl),
                new CreateRequestParameter(ProjectManagerConst.CODER_ENABLE_FILE_RECEIVER_PARAM_KEY, enableFileReceiver),
                new CreateRequestParameter(ProjectManagerConst.CODER_SAMPLY_BEAM_APP_ID_PARAM_KEY, projectCoder.getAppId()),
                new CreateRequestParameter(ProjectManagerConst.CODER_SAMPLY_BEAM_APP_SECRET_PARAM_KEY, projectCoder.getAppSecret())
        );
        createRequestBody.setRichParameterValues(createRequestParameters.toArray(CreateRequestParameter[]::new));
    }

    public String fetchCoderAppId(@NotNull ProjectBridgeheadUser projectBridgeheadUser) {
        return projectBridgeheadUser.getEmail().substring(0, projectBridgeheadUser.getEmail().indexOf("@"))
                .replace(".", "-") + "-" + projectBridgeheadUser.getProjectBridgehead().getProject().getCode();
    }

    private String generateAppSecret() {
        return UUID.randomUUID().toString();
    }

    private String fetchCoderMemberId(@NotNull String email) {
        return email.substring(0, email.indexOf("@")).replace(".", "");
    }

    public String getResearchEnvironmentUrl() {
        return researchEnvironmentUrl;
    }

    public boolean existsUserResearchEnvironmentWorkspace(@NotNull String projectCode, @NotNull String bridgehead) {
        return existsUserResearchEnvironmentWorkspace(projectCode, bridgehead, sessionUser.getEmail());
    }

    public boolean existsUserResearchEnvironmentWorkspace(@NotNull String projectCode, @NotNull String bridgehead, String email) {
        List<ProjectCoder> projectCoder = this.projectCoderRepository.findByBridgeheadAndProjectCodeAndEmailOrderedByCreatedAtDesc(bridgehead, projectCode, email);
        return !projectCoder.isEmpty() && projectCoder.get(0).getDeletedAt() == null;
    }

}
