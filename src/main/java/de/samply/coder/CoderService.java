package de.samply.coder;

import de.samply.app.ProjectManagerConst;
import de.samply.coder.request.CreateRequestBody;
import de.samply.coder.request.CreateRequestParameter;
import de.samply.coder.request.Response;
import de.samply.coder.request.TransitionRequestBody;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectCoder;
import de.samply.db.repository.ProjectCoderRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
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
    private final ProjectRepository projectRepository;
    private final ProjectCoderRepository projectCoderRepository;
    private final NotificationService notificationService;
    private final String coderTemplateVersionId;
    private final String coderCreatePath;
    private final String coderDeletePath;
    private final String enableJupyterLab;
    private final String enableRstudio;
    private final String enableVsCodeServer;
    private final String dotFilesUrl;
    private final String enableFileReceiver;
    private final String coderSessionToken;

    private final WebClient webClient;

    public CoderService(
            ProjectCoderRepository projectCoderRepository,
            NotificationService notificationService,
            ProjectRepository projectRepository,
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
        this.projectRepository = projectRepository;
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

    public void createWorkspace(String email, String projectCode) throws CoderServiceException {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new CoderServiceException("Project " + projectCode + " not found");
        }
        createWorkspace(email, project.get());
    }

    public void createWorkspace(@NotNull String email, @NotNull Project project) {
        if (coderEnabled) {
            ProjectCoder projectCoder = generateProjectCoder(email, project);
            CreateRequestBody createRequestBody = generateCreateRequestBody(projectCoder);
            Response response = createWorkspace(projectCoder, createRequestBody).block();
            projectCoder.setWorkspaceId(response.getLatestBuild().getWorkspaceId());
            projectCoderRepository.save(projectCoder);
            notificationService.createNotification(project.getCode(), null, email, OperationType.CREATE_CODER_WORKSPACE,
                    "Created workspace " + projectCoder.getWorkspaceId(), null, null);

        }
    }

    private Mono<Response> createWorkspace(ProjectCoder projectCoder, CreateRequestBody createRequestBody) {
        return this.webClient.post()
                .uri(uriBuilder -> uriBuilder.path(ProjectManagerConst.CODER_API_PATH).path(
                        replaceVariablesInPath(coderCreatePath, Map.of(ProjectManagerConst.CODER_MEMBER_ID, fetchCoderMemberId(projectCoder.getEmail())))).build())
                .header(ProjectManagerConst.CODER_SESSION_TOKEN_HEADER, coderSessionToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequestBody)
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().equals(HttpStatus.OK) || clientResponse.statusCode().equals(HttpStatus.CREATED)) {
                        return clientResponse.bodyToMono(Response.class);
                    } else {
                        log.error("Http error " + clientResponse.statusCode() + " creating workspace in Coder for user "
                                + projectCoder.getEmail() + " in project " + projectCoder.getProject().getCode());
                        return clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Error: {}", errorBody);
                            return Mono.error(new RuntimeException(errorBody));
                        });
                    }
                });
    }

    private ProjectCoder generateProjectCoder(String email, Project project) {
        ProjectCoder projectCoder = new ProjectCoder();
        projectCoder.setProject(project);
        projectCoder.setEmail(email);
        projectCoder.setAppId(fetchCoderAppId(email, project));
        projectCoder.setAppSecret(generateAppSecret());
        return projectCoder;
    }

    public void deleteWorkspace(@NotNull String email, @NotNull String projectCode) throws CoderServiceException {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new CoderServiceException("Project " + projectCode + " not found");
        }
        deleteWorkspace(email, project.get());
    }

    public void deleteWorkspace(@NotNull String email, @NotNull Project project) {
        if (coderEnabled) {
            projectCoderRepository.findByProjectAndEmail(project, email)
                    .filter(projectCoder -> projectCoder.getDeletedAt() == null).ifPresent(projectCoder -> {
                        deleteWorkspace(projectCoder).block();
                        projectCoder.setDeletedAt(Instant.now());
                        projectCoderRepository.save(projectCoder);
                        notificationService.createNotification(project.getCode(), null, email, OperationType.DELETE_CODER_WORKSPACE,
                                "Deleted workspace " + projectCoder.getWorkspaceId(), null, null);
                    });
        }
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
                                + projectCoder.getEmail() + " in project " + projectCoder.getProject().getCode());
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

    public String fetchCoderAppId(@NotNull String email, @NotNull Project project) {
        return email.substring(0, email.indexOf("@")).replace(".", "-") + "-" + project.getCode();
    }

    private String generateAppSecret() {
        return UUID.randomUUID().toString();
    }

    private String fetchCoderMemberId(@NotNull String email) {
        return email.substring(0, email.indexOf("@")).replace(".", "");
    }
}
