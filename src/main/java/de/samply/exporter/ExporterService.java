package de.samply.exporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.samply.app.ProjectManagerConst;
import de.samply.coder.CoderService;
import de.samply.db.model.*;
import de.samply.email.EmailKeyValuesFactory;
import de.samply.email.EmailService;
import de.samply.email.EmailTemplateType;
import de.samply.exporter.focus.BeamRequest;
import de.samply.exporter.focus.BeamService;
import de.samply.exporter.focus.BeamServiceException;
import de.samply.exporter.focus.TaskType;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.project.ProjectBridgeheadService;
import de.samply.project.ProjectType;
import de.samply.query.QueryState;
import de.samply.security.SessionUser;
import de.samply.user.UserService;
import de.samply.user.roles.ProjectRole;
import de.samply.utils.Base64Utils;
import de.samply.utils.MessageStatus;
import de.samply.utils.WebClientFactory;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class ExporterService {


    private final WebClient webClient;
    private final SessionUser sessionUser;
    private final EmailKeyValuesFactory emailKeyValuesFactory;

    // Services
    private final BeamService beamService;
    private final CoderService coderService;
    private final NotificationService notificationService;
    private final ProjectBridgeheadService projectBridgeheadService;
    private final EmailService emailService;
    private final UserService userService;

    @Getter
    private final Map<ProjectType, Set<String>> exporterTemplates = new HashMap<>();

    private final String focusProjectManagerId;
    private final String exporterApiKey;
    private final String coderBeamIdSuffix;
    private final String testCoderFileBeamId;

    private final String beamWaitTime;
    private final String beamWaitCount;
    private final int maxTimeToWaitFocusTaskInMinutes;

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    public ExporterService(
            @Value(ProjectManagerConst.BEAM_API_KEY_SV) String exporterApiKey,
            @Value(ProjectManagerConst.BEAM_PROJECT_MANAGER_ID_SV) String focusProjectManagerId,
            @Value(ProjectManagerConst.BEAM_URL_SV) String focusUrl,
            @Value(ProjectManagerConst.EXPORT_TEMPLATES_SV) Set<String> exportTemplates,
            @Value(ProjectManagerConst.DATASHIELD_TEMPLATES_SV) Set<String> datashieldTemplates,
            @Value(ProjectManagerConst.RESEARCH_ENVIRONMENT_TEMPLATES_SV) Set<String> researchEnvironmentTemplates,
            @Value(ProjectManagerConst.SAMPLES_TEMPLATES_SV) Set<String> samplesTemplates, CoderService coderService, ProjectBridgeheadService projectBridgeheadService,
            @Value(ProjectManagerConst.BEAM_TTL_SV) String beamWaitTime,
            @Value(ProjectManagerConst.BEAM_WAIT_COUNT_SV) String beamWaitCount,
            @Value(ProjectManagerConst.MAX_TIME_TO_WAIT_FOCUS_TASK_IN_MINUTES_SV) int maxTimeToWaitFocusTaskInMinutes,
            @Value(ProjectManagerConst.CODER_BEAM_ID_SUFFIX_SV) String coderBeamIdSuffix,
            @Value(ProjectManagerConst.CODER_TEST_FILE_BEAM_ID_SV) String testCoderFileBeamId,
            SessionUser sessionUser,
            BeamService beamService,
            NotificationService notificationService,
            WebClientFactory webClientFactory,
            EmailService emailService,
            EmailKeyValuesFactory emailKeyValuesFactory,
            UserService userService) {
        this.coderService = coderService;
        this.projectBridgeheadService = projectBridgeheadService;
        this.userService = userService;

        this.exporterTemplates.put(ProjectType.EXPORT, exportTemplates);
        this.exporterTemplates.put(ProjectType.DATASHIELD, datashieldTemplates);
        this.exporterTemplates.put(ProjectType.RESEARCH_ENVIRONMENT, researchEnvironmentTemplates);
        this.exporterTemplates.put(ProjectType.SAMPLES, samplesTemplates);

        this.sessionUser = sessionUser;
        this.beamService = beamService;
        this.notificationService = notificationService;
        this.focusProjectManagerId = focusProjectManagerId;
        this.beamWaitTime = beamWaitTime;
        this.beamWaitCount = beamWaitCount;
        this.maxTimeToWaitFocusTaskInMinutes = maxTimeToWaitFocusTaskInMinutes;
        this.coderBeamIdSuffix = coderBeamIdSuffix;
        this.testCoderFileBeamId = testCoderFileBeamId;
        this.emailService = emailService;
        this.emailKeyValuesFactory = emailKeyValuesFactory;
        this.webClient = webClientFactory.createWebClient(focusUrl);
        this.exporterApiKey = exporterApiKey;
    }

    public Mono<ExporterServiceResult> sendQueryToBridgehead(ProjectBridgeheadAndType projectBridgeheadAndType) throws ExporterServiceException {
        String description = "Sending query of project " + projectBridgeheadAndType.projectBridgehead().getProject().getCode() + " to bridgehead " + projectBridgeheadAndType.projectBridgehead().getBridgehead() + " ...";
        log.info(description);
        TaskType taskType = TaskType.CREATE;
        return postRequest(projectBridgeheadAndType, generateFocusBody(projectBridgeheadAndType, taskType), taskType, description)
                .doOnSuccess(_ -> logQuerySent());
    }

    private void logQuerySent() {
        log.info("Query sent");
    }

    public Mono<ExporterServiceResult> sendQueryToBridgeheadAndExecute(ProjectBridgeheadAndType projectBridgeheadAndType) throws ExporterServiceException {
        String description = "Sending query of project " + projectBridgeheadAndType.projectBridgehead().getProject().getCode() + " to bridgehead " + projectBridgeheadAndType.projectBridgehead().getBridgehead() + " to be executed...";
        log.info(description);
        TaskType taskType = TaskType.EXECUTE;
        return postRequest(projectBridgeheadAndType, generateFocusBody(projectBridgeheadAndType, taskType), taskType, description)
                .doOnSuccess(_ -> logQuerySent());
    }

    public Mono<ExporterServiceResult> checkExecutionStatus(ProjectBridgeheadAndType projectBridgeheadAndType) throws ExporterServiceException {
        String description = "Checking export execution status of project " + projectBridgeheadAndType.projectBridgehead().getProject().getCode() + " in bridgehead " + projectBridgeheadAndType.projectBridgehead().getBridgehead() + "...";
        log.info(description);
        TaskType taskType = TaskType.STATUS;
        return postRequest(projectBridgeheadAndType, generateFocusBody(projectBridgeheadAndType, taskType), taskType, description)
                .doOnSuccess(_ -> log.info("Status checked"));
    }

    @Async()
    public void transferFileToResearchEnvironment(@NotNull String projectCode, @NotNull String bridgehead) {
        List<ProjectCoder> projectCoder = coderService.fetchCoderOrderedByCreatedAtDesc(projectCode, bridgehead, sessionUser.getEmail());
        if (projectCoder.isEmpty()) {
            throw new ExporterServiceException("ProjectCode " + projectCode + " for bridgehead " + bridgehead + " for user " + sessionUser.getEmail() + " not found");
        }
        transferFileToResearchEnvironment(projectCoder.getFirst()).block();
    }

    public boolean isExportFileTransferredToResearchEnvironment(@NotNull String projectCode, @NotNull String bridgehead) {
        List<ProjectCoder> projectCoder = coderService.fetchCoderOrderedByCreatedAtDesc(projectCode, bridgehead, sessionUser.getEmail());
        if (projectCoder.isEmpty()) {
            throw new ExporterServiceException("ProjectCode " + projectCode + " for bridgehead " + bridgehead + " for user " + sessionUser.getEmail() + " not found");
        }
        return projectCoder.getFirst().isExportTransferred();
    }

    @Async
    public Mono<Void> transferFileToResearchEnvironment(ProjectCoder projectCoder) {
        String description = "Transferring file to Research Environment for project " + projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode() + " in bridgehead " + projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead();
        log.info(description);

        return Flux.fromStream(
                        projectCoder
                                .getProjectBridgeheadUser()
                                .getProjectBridgehead()
                                .getProject()
                                .fetchProjectTypes()
                                .stream()
                                .map(projectType -> new ProjectBridgeheadAndType(projectCoder.getProjectBridgeheadUser().getProjectBridgehead(), projectType))
                ).flatMap(projectBridgeheadAndType ->
                        postRequest(projectBridgeheadAndType,
                                generateTransferFileBeamRequest(projectCoder, projectBridgeheadAndType.projectType()),
                                TaskType.FILE_TRANSFER, description)
                                .doOnError(throwable -> {
                                    MessageStatus messageStatus = MessageStatus.newInstance(throwable, "Error transferring file to Research Environment");
                                    notificationService.createNotification(
                                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject(),
                                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                                            projectCoder.getProjectBridgeheadUser().getEmail(),
                                            OperationType.TRANSFER_FILES_TO_RESEARCH_ENVIRONMENT,
                                            messageStatus.message(),
                                            ExceptionUtils.getStackTrace(throwable),
                                            messageStatus.status()
                                    );
                                    log.error(ExceptionUtils.getStackTrace(throwable));
                                })
                                .doOnSuccess(_ -> {
                                    log.info("Files transferred correctly");
                                    projectCoder.setExportTransferred(true);
                                    coderService.saveCoder(projectCoder);
                                    notificationService.createNotification(
                                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject(),
                                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                                            projectCoder.getProjectBridgeheadUser().getEmail(),
                                            OperationType.TRANSFER_FILES_TO_RESEARCH_ENVIRONMENT,
                                            "File transferred to Research Environment",
                                            null,
                                            HttpStatus.OK
                                    );
                                }))
                .then();
    }

    private BeamRequest generateTransferFileBeamRequest(ProjectCoder projectCoder, ProjectType projectType) {
        return beamService.generateExporterFileTransferBeamRequest(
                projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                projectCoder.getProjectBridgeheadUser().getProjectBridgehead()
                        .fetchExecution(projectType)
                        .orElseThrow(() -> new IllegalStateException("Missing execution for " + projectType))
                        .getExporterExecutionId(),
                fetchCoderFileBeamId(projectCoder)
        );
    }

    private String fetchCoderFileBeamId(ProjectCoder projectCoder) {
        return Objects.requireNonNullElseGet(testCoderFileBeamId, () -> projectCoder.getAppId() + ((coderBeamIdSuffix.startsWith(".")) ? "" : ".") + coderBeamIdSuffix);
    }

    private Mono<ExporterServiceResult> postRequest(ProjectBridgeheadAndType projectBridgeheadAndType, BeamRequest beamRequest, TaskType taskType, String description) {
        ProjectBridgehead projectBridgehead = projectBridgeheadAndType.projectBridgehead();
        ProjectType projectType = projectBridgeheadAndType.projectType();
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path(ProjectManagerConst.BEAM_TASK_PATH).build())
                .header(HttpHeaders.AUTHORIZATION, fetchAuthorization())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(beamRequest)
                .exchangeToMono(clientResponse -> {
                    log.debug("Received HTTP POST response with status {}", clientResponse.statusCode());
                    if (clientResponse.statusCode().equals(HttpStatus.OK) || clientResponse.statusCode().equals(HttpStatus.CREATED)) {
                        fetchBridgeheadOperationType(taskType).ifPresent(operationType ->
                                createBridgeheadNotification(
                                        (HttpStatus) clientResponse.statusCode(),
                                        projectBridgehead,
                                        projectBridgehead
                                                .fetchExecution(projectType)
                                                .orElseThrow(() -> new IllegalStateException("Missing execution for " + projectType))
                                                .getExporterUser(),
                                        operationType,
                                        description));
                        resetProjectBridgeheadDataShield(projectBridgeheadAndType);
                        return Mono.just(new ExporterServiceResult(projectBridgeheadAndType, beamService.serializeFocusQuery(beamRequest)));
                    } else {
                        log.error("Http Error {} posting task {} : {} for project {} and bridgehead {}",
                                clientResponse.statusCode(), beamRequest.getId(), beamRequest.getBody(),
                                projectBridgehead.getProject().getCode(), projectBridgehead.getBridgehead());
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

    private String fetchAuthorization() {
        return ProjectManagerConst.API_KEY + ' ' + focusProjectManagerId + ' ' + exporterApiKey;
    }

    private void createBridgeheadNotification(
            HttpStatus status, ProjectBridgehead projectBridgehead, String email, OperationType operationType, String description) {
        notificationService.createNotification(
                projectBridgehead.getProject(), projectBridgehead.getBridgehead(), email, operationType, description, null, status);
    }

    private Optional<OperationType> fetchBridgeheadOperationType(TaskType taskType) {
        return switch (taskType) {
            case CREATE -> Optional.of(OperationType.SEND_QUERY_TO_BRIDGEHEAD);
            case EXECUTE -> Optional.of(OperationType.SEND_QUERY_TO_BRIDGEHEAD_AND_EXECUTE);
            case FILE_TRANSFER -> Optional.of(OperationType.TRANSFER_FILE_TO_CODER);
            default -> Optional.empty();
        };
    }

    private String convertToBase64String(Object jsonObject) {
        try {
            return Base64Utils.encode(objectMapper.writeValueAsString(jsonObject));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateExportQueryInBase64ForExporterRequest(ProjectBridgeheadAndType projectBridgeheadAndType)
            throws ExporterServiceException {
        ProjectBridgehead projectBridgehead = projectBridgeheadAndType.projectBridgehead();
        ProjectType projectType = projectBridgeheadAndType.projectType();
        Query query = projectBridgehead.getProject().getQuery();
        Map<String, String> result = Stream.of(
                        new String[][]{
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY, query.getQuery()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_FORMAT, query.getQueryFormat().name()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_LABEL, fetchLabel(projectBridgeheadAndType)},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_DESCRIPTION, query.getDescription()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTEXT, generateQueryContextForExporter(query.getContext(), projectBridgehead.getProject().getCode())},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTACT_ID, projectBridgehead.getProject().getCreatorEmail()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_EXECUTION_CONTACT_ID, projectBridgehead
                                        .fetchExecution(projectType)
                                        .orElseThrow(() -> new IllegalStateException("Missing execution for " + projectType))
                                        .getExporterUser()},
                                {ProjectManagerConst.EXPORTER_PARAM_OUTPUT_FORMAT, query
                                        .fetchOutput(projectType)
                                        .orElseThrow(() -> new IllegalStateException("Missing output for " + projectType))
                                        .getOutputFormat().name()},
                                {ProjectManagerConst.EXPORTER_PARAM_TEMPLATE_ID, query
                                        .fetchOutput(projectType)
                                        .orElseThrow(() -> new IllegalStateException("Missing output for " + projectType))
                                        .getTemplateId()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_EXPIRATION_DATE, convertToString(projectBridgehead.getProject().getExpiresAt())}
                        }
                )
                .filter(entry -> StringUtils.hasText(entry[1])) // Filter entries with non-null and non-empty values
                .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1])); // Collect to a Map
        return convertToBase64String(result);
    }

    private String generateExportStatusInBase64ForExporterRequest(ProjectBridgeheadAndType projectBridgeheadAndType) {
        Map<String, String> result = Map.of(
                ProjectManagerConst.EXPORTER_PARAM_QUERY_EXECUTION_ID, projectBridgeheadAndType.projectBridgehead()
                        .fetchExecution(projectBridgeheadAndType.projectType())
                        .orElseThrow(() -> new IllegalStateException("Missing execution for " + projectBridgeheadAndType.projectType()))
                        .getExporterExecutionId()
        );
        return convertToBase64String(result);
    }

    private String generateExporterQueryInBase64ForExporterCreateQuery(ProjectBridgeheadAndType projectBridgeheadAndType)
            throws ExporterServiceException {
        ProjectBridgehead projectBridgehead = projectBridgeheadAndType.projectBridgehead();
        ProjectType projectType = projectBridgeheadAndType.projectType();
        Query query = projectBridgehead.getProject().getQuery();
        Map<String, String> result = Stream.of(
                        new String[][]{
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY, query.getQuery()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_FORMAT, query.getQueryFormat().name()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_LABEL, fetchLabel(projectBridgeheadAndType)},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_DESCRIPTION, query.getDescription()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTEXT, generateQueryContextForExporter(query.getContext(), projectBridgehead.getProject().getCode())},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTACT_ID, projectBridgehead.getProject().getCreatorEmail()},
                                {ProjectManagerConst.EXPORTER_PARAM_DEFAULT_OUTPUT_FORMAT, query
                                        .fetchOutput(projectType)
                                        .orElseThrow(() -> new IllegalStateException("Missing output for " + projectType))
                                        .getOutputFormat().name()},
                                {ProjectManagerConst.EXPORTER_PARAM_DEFAULT_TEMPLATE_ID, query
                                        .fetchOutput(projectType)
                                        .orElseThrow(() -> new IllegalStateException("Missing output for " + projectType))
                                        .getTemplateId()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_EXPIRATION_DATE, convertToString(projectBridgehead.getProject().getExpiresAt())}
                        }
                )
                .filter(entry -> StringUtils.hasText(entry[1])) // Filter entries with non-empty values
                .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1])); // Collect to a Map
        return convertToBase64String(result);
    }

    private String fetchLabel(@NotNull ProjectBridgeheadAndType projectBridgeheadAndType) {
        ProjectBridgehead projectBridgehead = projectBridgeheadAndType.projectBridgehead();
        ProjectType projectType = projectBridgeheadAndType.projectType();
        String label = "[" + projectType.name() + "-"
                + projectBridgehead.getProject().getCode().substring(0, 5) + "] "
                + projectBridgehead.getProject().getQuery().getLabel();
        return (projectBridgehead
                .fetchExecution(projectType)
                .orElseThrow(() -> new IllegalStateException("Missing execution for " + projectType))
                .getExporterDispatchCounter() == 0) ?
                label :
                label + " (Attempt: " + (projectBridgehead
                                         .fetchExecution(projectType)
                                         .orElseThrow(() -> new IllegalStateException("Missing execution for " + projectType))
                                         .getExporterDispatchCounter() + 1) + ")";
    }

    private BeamRequest generateFocusBody(ProjectBridgeheadAndType projectBridgeheadAndType, TaskType taskType) throws ExporterServiceException {
        try {
            return generateFocusQueryWithoutExceptionHandling(projectBridgeheadAndType, taskType);
        } catch (BeamServiceException e) {
            throw new ExporterServiceException(e);
        }
    }

    private BeamRequest generateFocusQueryWithoutExceptionHandling(ProjectBridgeheadAndType projectBridgeheadAndType, TaskType taskType) throws BeamServiceException {
        String exporterQueryInBase64 = switch (taskType) {
            case CREATE -> generateExporterQueryInBase64ForExporterCreateQuery(projectBridgeheadAndType);
            case EXECUTE -> generateExportQueryInBase64ForExporterRequest(projectBridgeheadAndType);
            case STATUS -> generateExportStatusInBase64ForExporterRequest(projectBridgeheadAndType);
            default -> null;
        };
        return beamService.generateFocusBeamRequest(exporterQueryInBase64, taskType, projectBridgeheadAndType.projectBridgehead().getBridgehead());
    }

    private String convertToString(LocalDate date) {
        return (date != null) ? date.format(DateTimeFormatter.ISO_DATE) : null;
    }

    private String generateQueryContextForExporter(String queryContext, String projectCode) {
        String context = ProjectManagerConst.EXPORTER_QUERY_CONTEXT_PROJECT_ID + '=' + projectCode;
        if (StringUtils.hasText(queryContext)) {
            context += ProjectManagerConst.EXPORTER_QUERY_CONTEXT_SEPARATOR + queryContext;
        }
        return Base64Utils.encode(context);
    }

    private void resetProjectBridgeheadDataShield(ProjectBridgeheadAndType projectBridgeheadAndType) {
        ProjectBridgehead projectBridgehead = projectBridgeheadAndType.projectBridgehead();
        ProjectType projectType = projectBridgeheadAndType.projectType();
        if (projectBridgehead.getProject()
                .fetchOutput(projectType)
                .orElseThrow(() -> new IllegalStateException("Missing output for " + projectType))
                .getProjectType() == ProjectType.DATASHIELD) {
            Optional<ProjectBridgeheadDataShield> projectBridgeheadInDataSHIELD =
                    projectBridgeheadService.fetchDataShield(projectBridgehead);
            ProjectBridgeheadDataShield result;
            if (projectBridgeheadInDataSHIELD.isEmpty()) {
                result = new ProjectBridgeheadDataShield();
                result.setProjectBridgehead(projectBridgehead);
            } else {
                result = projectBridgeheadInDataSHIELD.get();
            }
            result.setRemoved(false);
            projectBridgeheadService.saveDataShield(result);
        }
    }

    public Mono<ExporterServiceResult> checkIfQueryIsAlreadySentOrExecuted(ProjectBridgeheadAndType projectBridgeheadAndType) {
        Optional<BeamRequest> focusQuery = extractFocusQuery(projectBridgeheadAndType);
        ProjectBridgehead projectBridgehead = projectBridgeheadAndType.projectBridgehead();
        ProjectType projectType = projectBridgeheadAndType.projectType();
        if (focusQuery.isEmpty()) {
            throw new RuntimeException("Focus Query not found for project " + projectBridgehead.getProject().getCode() + " and bridgehead " + projectBridgehead.getBridgehead());
        }
        log.info("Checking if query is already sent or executed for project {} and bridgehead {}", projectBridgehead.getProject().getCode(), projectBridgehead.getBridgehead());
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(ProjectManagerConst.BEAM_TASK_PATH + "/" + extractTaskId(focusQuery.get()) + ProjectManagerConst.BEAM_TASK_RESULTS_PATH)
                        .queryParam(ProjectManagerConst.BEAM_TASK_WAIT_TIME_PARAM, beamWaitTime)
                        .queryParam(ProjectManagerConst.BEAM_TASK_WAIT_COUNT_PARAM, beamWaitCount).build())
                .header(HttpHeaders.AUTHORIZATION, fetchAuthorization())
                .exchangeToMono(clientResponse -> {
                    log.debug("Received HTTP GET response with status {}", clientResponse.statusCode());
                    if (clientResponse.statusCode().equals(HttpStatus.OK) || clientResponse.statusCode().equals(HttpStatus.PARTIAL_CONTENT)) {
                        Optional<OperationType> operationType = switch (projectBridgehead
                                .fetchExecution(projectType)
                                .orElseThrow(() -> new IllegalStateException("Missing execution for " + projectType))
                                .getQueryState()) {
                            case SENDING -> Optional.of(OperationType.CHECK_SEND_QUERY);
                            case SENDING_AND_EXECUTING -> Optional.of(OperationType.CHECK_SEND_AND_EXECUTE_QUERY);
                            default -> Optional.empty();
                        };
                        operationType.ifPresent(type -> createBridgeheadNotification(
                                HttpStatus.OK, projectBridgehead, projectBridgehead
                                        .fetchExecution(projectType)
                                        .orElseThrow(() -> new IllegalStateException("Missing execution for " + projectType))
                                        .getExporterUser(),
                                type, "Checking if query is already sent or executed"));
                        return clientResponse.bodyToMono(BeamRequest[].class).filter(focusQueries -> focusQueries.length > 0).flatMap(newBeamRequest -> {
                            if (projectBridgehead
                                    .fetchExecution(projectType)
                                    .orElseThrow(() -> new IllegalStateException("Missing execution for " + projectType))
                                    .getQueryState() == QueryState.EXPORT_RUNNING_2) {
                                if (newBeamRequest[0].getBody() == null) {
                                    return Mono.empty();
                                }
                                Optional<String> decodedBody = Base64Utils.decodeIfNecessary(newBeamRequest[0].getBody());
                                if (decodedBody.isEmpty() || !decodedBody.get().contains("OK")) {
                                    if (decodedBody.isEmpty() || decodedBody.get().contains("ERROR")) {
                                        modifyProjectBridgeheadState(projectBridgeheadAndType, QueryState.ERROR);
                                    } else {
                                        modifyProjectBridgeheadState(projectBridgeheadAndType, QueryState.EXPORT_RUNNING_1);
                                    }
                                    return Mono.empty();
                                }
                            }
                            return Mono.just(new ExporterServiceResult(projectBridgeheadAndType, beamService.serializeFocusQuery(newBeamRequest[0])));
                        });
                    } else {
                        log.error("Http Error {} checking task {} for project {} and bridgehead {}",
                                clientResponse.statusCode(), extractTaskId(focusQuery.get()),
                                projectBridgehead.getProject().getCode(), projectBridgehead.getBridgehead());
                        if (isQueryStateToBeChangedToError((HttpStatus) clientResponse.statusCode(), projectBridgehead)) {
                            modifyProjectBridgeheadState(projectBridgeheadAndType, QueryState.ERROR);
                        }
                        return clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            logError(errorBody);
                            return Mono.error(new RuntimeException(errorBody));
                        });
                    }
                });
    }

    private void modifyProjectBridgeheadState(ProjectBridgeheadAndType projectBridgeheadAndType, QueryState newState) {
        ProjectBridgehead projectBridgehead = projectBridgeheadAndType.projectBridgehead();
        projectBridgehead.fetchExecution(projectBridgeheadAndType.projectType()).ifPresent(execution -> {
            execution.setQueryState(newState);
            execution.setModifiedAt(Instant.now());
            if (newState == QueryState.ERROR) {
                execution.setExporterDispatchCounter(execution.getExporterDispatchCounter() + 1);
                sendEmail(projectBridgehead, EmailTemplateType.ERROR_WHILE_SAVING_QUERY_IN_EXPORTER);
            }
            projectBridgeheadService.saveBridgehead(projectBridgehead);
        });
    }

    private void sendEmail(ProjectBridgehead projectBridgehead, @SuppressWarnings("SameParameterValue") EmailTemplateType templateType) {
        userService
                .fetchBridgeheadAdmin(projectBridgehead)
                .forEach(bridgeheadAdmin -> emailService.sendEmail(
                        bridgeheadAdmin.getEmail(),
                        Optional.of(projectBridgehead.getProject()),
                        Optional.of(projectBridgehead),
                        ProjectRole.BRIDGEHEAD_ADMIN,
                        templateType,
                        emailKeyValuesFactory.newInstance().add(projectBridgehead)));
    }

    private String extractTaskId(BeamRequest beamRequest) {
        return (beamRequest.getId() != null) ? beamRequest.getId() : beamRequest.getTask();
    }

    private Optional<BeamRequest> extractFocusQuery(ProjectBridgeheadAndType projectBridgeheadAndType) {
        return projectBridgeheadAndType.projectBridgehead().fetchExecution(projectBridgeheadAndType.projectType())
                .map(ProjectBridgeheadExecution::getExporterResponse)
                .map(beamService::deserializeFocusResponse)
                .filter(focusQueries -> focusQueries.length > 0)
                .map(focusQueries -> focusQueries[0]);
    }

    private boolean isQueryStateToBeChangedToError(HttpStatus httpStatus, ProjectBridgehead projectBridgehead) {
        if (httpStatus == HttpStatus.NOT_FOUND) {
            return Duration.between(projectBridgehead.getModifiedAt(), Instant.now()).toMinutes() > maxTimeToWaitFocusTaskInMinutes;
        }
        return httpStatus != HttpStatus.NO_CONTENT;
    }

    public Optional<String> fetchExporterExecutionIdFromExporterResponse(String exporterResponse) {
        if (exporterResponse != null) {
            Optional<BeamRequest[]> focusQuery = deserializeFocusResponse(exporterResponse);
            if (focusQuery.isPresent() && focusQuery.get().length > 0 && focusQuery.get()[0].getBody() != null) {
                Optional<String> body = Base64Utils.decodeIfNecessary(focusQuery.get()[0].getBody());
                return body.isPresent() ? fetchQueryExecutionIdFromQueryExecutionIdUrl(body.get()) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Optional<BeamRequest[]> deserializeFocusResponse(String exporterResponse) {
        try {
            return Optional.of(beamService.deserializeFocusResponse(exporterResponse));
        } catch (BeamServiceException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            return Optional.empty();
        }
    }

    private Optional<String> fetchQueryExecutionIdFromQueryExecutionIdUrl(String queryExecutionIdUrl) {
        if (queryExecutionIdUrl != null) {
            String searchedString = ProjectManagerConst.EXPORTER_PARAM_QUERY_EXECUTION_ID + "=";
            int index = queryExecutionIdUrl.indexOf(searchedString);
            if (index >= 0 && queryExecutionIdUrl.length() > index + searchedString.length()) {
                String queryExecutionId = queryExecutionIdUrl.substring(index + searchedString.length());
                index = queryExecutionId.indexOf("\"");
                return Optional.of(index > 0 ? queryExecutionId.substring(0, index) : queryExecutionId);
            }
        }
        return Optional.empty();
    }

}
