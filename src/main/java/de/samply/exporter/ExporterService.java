package de.samply.exporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.samply.app.ProjectManagerConst;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadDataShield;
import de.samply.db.model.ProjectCoder;
import de.samply.db.model.Query;
import de.samply.db.repository.BridgeheadAdminUserRepository;
import de.samply.db.repository.ProjectBridgeheadDataShieldRepository;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectCoderRepository;
import de.samply.email.EmailKeyValuesFactory;
import de.samply.email.EmailService;
import de.samply.email.EmailTemplateType;
import de.samply.exporter.focus.BeamRequest;
import de.samply.exporter.focus.BeamService;
import de.samply.exporter.focus.BeamServiceException;
import de.samply.exporter.focus.TaskType;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.project.ProjectType;
import de.samply.query.QueryState;
import de.samply.security.SessionUser;
import de.samply.user.roles.ProjectRole;
import de.samply.utils.Base64Utils;
import de.samply.utils.MessageStatus;
import de.samply.utils.WebClientFactory;
import jakarta.validation.constraints.NotNull;
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
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class ExporterService {


    private final BeamService beamService;
    private final WebClient webClient;
    private final SessionUser sessionUser;
    private final ProjectBridgeheadDataShieldRepository projectBridgeheadDataShieldRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final ProjectCoderRepository projectCoderRepository;
    private final NotificationService notificationService;
    private final Set<String> exportTemplates;
    private final Set<String> datashieldTemplates;
    private final Set<String> researchEnvironmentTemplates;
    private final String focusProjectManagerId;
    private final String exporterApiKey;
    private final String coderBeamIdSuffix;
    private final String testCoderFileBeamId;

    private final String beamWaitTime;
    private final String beamWaitCount;
    private final int maxTimeToWaitFocusTaskInMinutes;
    private final EmailService emailService;
    private final BridgeheadAdminUserRepository bridgeheadAdminUserRepository;
    private final EmailKeyValuesFactory emailKeyValuesFactory;
    private ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    public ExporterService(
            @Value(ProjectManagerConst.BEAM_API_KEY_SV) String exporterApiKey,
            @Value(ProjectManagerConst.BEAM_PROJECT_MANAGER_ID_SV) String focusProjectManagerId,
            @Value(ProjectManagerConst.BEAM_URL_SV) String focusUrl,
            @Value(ProjectManagerConst.EXPORT_TEMPLATES_SV) Set<String> exportTemplates,
            @Value(ProjectManagerConst.DATASHIELD_TEMPLATES_SV) Set<String> datashieldTemplates,
            @Value(ProjectManagerConst.RESEARCH_ENVIRONMENT_TEMPLATES_SV) Set<String> researchEnvironmentTemplates,
            @Value(ProjectManagerConst.BEAM_TTL_SV) String beamWaitTime,
            @Value(ProjectManagerConst.BEAM_WAIT_COUNT_SV) String beamWaitCount,
            @Value(ProjectManagerConst.MAX_TIME_TO_WAIT_FOCUS_TASK_IN_MINUTES_SV) int maxTimeToWaitFocusTaskInMinutes,
            @Value(ProjectManagerConst.CODER_BEAM_ID_SUFFIX_SV) String coderBeamIdSuffix,
            @Value(ProjectManagerConst.CODER_TEST_FILE_BEAM_ID_SV) String testCoderFileBeamId,
            SessionUser sessionUser,
            BeamService beamService,
            ProjectBridgeheadDataShieldRepository projectBridgeheadDataShieldRepository,
            NotificationService notificationService,
            WebClientFactory webClientFactory,
            ProjectBridgeheadRepository projectBridgeheadRepository,
            ProjectCoderRepository projectCoderRepository,
            EmailService emailService,
            BridgeheadAdminUserRepository bridgeheadAdminUserRepository,
            EmailKeyValuesFactory emailKeyValuesFactory) {
        this.sessionUser = sessionUser;
        this.beamService = beamService;
        this.projectBridgeheadDataShieldRepository = projectBridgeheadDataShieldRepository;
        this.notificationService = notificationService;
        this.exportTemplates = exportTemplates;
        this.datashieldTemplates = datashieldTemplates;
        this.focusProjectManagerId = focusProjectManagerId;
        this.beamWaitTime = beamWaitTime;
        this.beamWaitCount = beamWaitCount;
        this.maxTimeToWaitFocusTaskInMinutes = maxTimeToWaitFocusTaskInMinutes;
        this.researchEnvironmentTemplates = researchEnvironmentTemplates;
        this.coderBeamIdSuffix = coderBeamIdSuffix;
        this.testCoderFileBeamId = testCoderFileBeamId;
        this.projectCoderRepository = projectCoderRepository;
        this.emailService = emailService;
        this.bridgeheadAdminUserRepository = bridgeheadAdminUserRepository;
        this.emailKeyValuesFactory = emailKeyValuesFactory;
        this.webClient = webClientFactory.createWebClient(focusUrl);
        this.exporterApiKey = exporterApiKey;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
    }

    public Mono<ExporterServiceResult> sendQueryToBridgehead(ProjectBridgehead projectBridgehead) throws ExporterServiceException {
        String description = "Sending query of project " + projectBridgehead.getProject().getCode() + " to bridgehead " + projectBridgehead.getBridgehead() + " ...";
        log.info(description);
        TaskType taskType = TaskType.CREATE;
        return postRequest(projectBridgehead, generateFocusBody(projectBridgehead, taskType), taskType, description)
                .doOnSuccess(exporterServiceResult -> log.info("Query sent"));
    }

    public Mono<ExporterServiceResult> sendQueryToBridgeheadAndExecute(ProjectBridgehead projectBridgehead) throws ExporterServiceException {
        String description = "Sending query of project " + projectBridgehead.getProject().getCode() + " to bridgehead " + projectBridgehead.getBridgehead() + " to be executed...";
        log.info(description);
        TaskType taskType = TaskType.EXECUTE;
        return postRequest(projectBridgehead, generateFocusBody(projectBridgehead, taskType), taskType, description)
                .doOnSuccess(exporterServiceResult -> log.info("Query sent"));
    }

    public Mono<ExporterServiceResult> checkExecutionStatus(ProjectBridgehead projectBridgehead) {
        String description = "Checking export execution status of project " + projectBridgehead.getProject().getCode() + " in bridgehead " + projectBridgehead.getBridgehead() + "...";
        log.info(description);
        TaskType taskType = TaskType.STATUS;
        return postRequest(projectBridgehead, generateFocusBody(projectBridgehead, taskType), taskType, description)
                .doOnSuccess(exporterServiceResult -> log.info("Status checked"));
    }

    @Async()
    public void transferFileToResearchEnvironment(@NotNull String projectCode, @NotNull String bridgehead) {
        List<ProjectCoder> projectCoder = this.projectCoderRepository.findByBridgeheadAndProjectCodeAndEmailOrderedByCreatedAtDesc(bridgehead, projectCode, sessionUser.getEmail());
        if (projectCoder.isEmpty()) {
            throw new ExporterServiceException("Project " + projectCode + " for bridgehead " + bridgehead + " for user " + sessionUser.getEmail() + " not found");
        }
        transferFileToResearchEnvironment(projectCoder.get(0)).block();
    }

    public boolean isExportFileTransferredToResearchEnvironment(@NotNull String projectCode, @NotNull String bridgehead) {
        List<ProjectCoder> projectCoder = this.projectCoderRepository.findByBridgeheadAndProjectCodeAndEmailOrderedByCreatedAtDesc(bridgehead, projectCode, sessionUser.getEmail());
        if (projectCoder.isEmpty()) {
            throw new ExporterServiceException("Project " + projectCode + " for bridgehead " + bridgehead + " for user " + sessionUser.getEmail() + " not found");
        }
        return projectCoder.get(0).isExportTransferred();
    }

    @Async
    public Mono<Void> transferFileToResearchEnvironment(ProjectCoder projectCoder) {
        String description = "Transferring file to Research Environment for project " + projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode() + " in bridgehead " + projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead();
        log.info(description);
        return postRequest(projectCoder.getProjectBridgeheadUser().getProjectBridgehead(), generateTransferFileBeamRequest(projectCoder), TaskType.FILE_TRANSFER, description)
                .doOnError(throwable -> {
                    MessageStatus messageStatus = MessageStatus.newInstance(throwable, "Error transferring file to Research Environment");
                    notificationService.createNotification(
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode(),
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                            projectCoder.getProjectBridgeheadUser().getEmail(),
                            OperationType.TRANSFER_FILES_TO_RESEARCH_ENVIRONMENT,
                            messageStatus.message(),
                            ExceptionUtils.getStackTrace(throwable),
                            messageStatus.status()
                    );
                    log.error(ExceptionUtils.getStackTrace(throwable));
                })
                .doOnSuccess(result -> {
                    log.info("Files transferred correctly");
                    projectCoder.setExportTransferred(true);
                    this.projectCoderRepository.save(projectCoder);
                    notificationService.createNotification(
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode(),
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                            projectCoder.getProjectBridgeheadUser().getEmail(),
                            OperationType.TRANSFER_FILES_TO_RESEARCH_ENVIRONMENT,
                            "File transferred to Research Environment",
                            null,
                            HttpStatus.OK
                    );
                }).then();
    }

    private BeamRequest generateTransferFileBeamRequest(ProjectCoder projectCoder) {
        return beamService.generateExporterFileTransferBeamRequest(projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getExporterExecutionId(), fetchCoderFileBeamId(projectCoder));
    }

    private String fetchCoderFileBeamId(ProjectCoder projectCoder) {
        if (testCoderFileBeamId != null) {
            return testCoderFileBeamId;
        }
        return projectCoder.getAppId() + ((coderBeamIdSuffix.startsWith(".")) ? "" : ".") + coderBeamIdSuffix;
    }

    private Mono<ExporterServiceResult> postRequest(ProjectBridgehead projectBridgehead, BeamRequest beamRequest, TaskType taskType, String description) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path(ProjectManagerConst.BEAM_TASK_PATH).build())
                .header(HttpHeaders.AUTHORIZATION, fetchAuthorization())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(beamRequest)
                .exchangeToMono(clientResponse -> {
                    log.debug("Received HTTP POST response with status {}", clientResponse.statusCode());
                    if (clientResponse.statusCode().equals(HttpStatus.OK) || clientResponse.statusCode().equals(HttpStatus.CREATED)) {

                        fetchBridgeheadOperationType(taskType).ifPresent(operationType ->
                                createBridgeheadNotification((HttpStatus) clientResponse.statusCode(), null, projectBridgehead, projectBridgehead.getExporterUser(), operationType, description));
                        resetProjectBridgeheadDataShield(projectBridgehead);
                        return Mono.just(new ExporterServiceResult(projectBridgehead, beamService.serializeFocusQuery(beamRequest)));
                    } else {
                        log.error("Http Error " + clientResponse.statusCode() + " posting task " + beamRequest.getId() + " : " + beamRequest.getBody() +
                                " for project " + projectBridgehead.getProject().getCode() + " and bridgehead " + projectBridgehead.getBridgehead());
                        return clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Error: {}", errorBody);
                            return Mono.error(new RuntimeException(errorBody));
                        });
                    }
                });
    }

    private String fetchAuthorization() {
        return ProjectManagerConst.API_KEY + ' ' + focusProjectManagerId + ' ' + exporterApiKey;
    }

    private void createBridgeheadNotification(
            HttpStatus status, String error, ProjectBridgehead projectBridgehead, String email, OperationType operationType, String description) {
        notificationService.createNotification(
                projectBridgehead.getProject().getCode(), projectBridgehead.getBridgehead(), email, operationType, description, error, status);
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

    private String generateExportQueryInBase64ForExporterRequest(ProjectBridgehead projectBridgehead)
            throws ExporterServiceException {
        Query query = projectBridgehead.getProject().getQuery();
        Map<String, String> result = Stream.of(
                        new String[][]{
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY, query.getQuery()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_FORMAT, query.getQueryFormat().name()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_LABEL, fetchLabel(projectBridgehead)},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_DESCRIPTION, query.getDescription()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTEXT, generateQueryContextForExporter(query.getContext(), projectBridgehead.getProject().getCode())},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTACT_ID, projectBridgehead.getProject().getCreatorEmail()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_EXECUTION_CONTACT_ID, projectBridgehead.getExporterUser()},
                                {ProjectManagerConst.EXPORTER_PARAM_OUTPUT_FORMAT, query.getOutputFormat().name()},
                                {ProjectManagerConst.EXPORTER_PARAM_TEMPLATE_ID, query.getTemplateId()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_EXPIRATION_DATE, convertToString(projectBridgehead.getProject().getExpiresAt())}
                        }
                )
                .filter(entry -> StringUtils.hasText(entry[1])) // Filter entries with non-null and non-empty values
                .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1])); // Collect to a Map
        return convertToBase64String(result);
    }

    private String generateExportStatusInBase64ForExporterRequest(ProjectBridgehead projectBridgehead) {
        Map<String, String> result = Map.of(
                ProjectManagerConst.EXPORTER_PARAM_QUERY_EXECUTION_ID, projectBridgehead.getExporterExecutionId()
        );
        return convertToBase64String(result);
    }

    private String generateExporterQueryInBase64ForExporterCreateQuery(ProjectBridgehead projectBridgehead)
            throws ExporterServiceException {
        Query query = projectBridgehead.getProject().getQuery();
        Map<String, String> result = Stream.of(
                        new String[][]{
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY, query.getQuery()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_FORMAT, query.getQueryFormat().name()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_LABEL, fetchLabel(projectBridgehead)},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_DESCRIPTION, query.getDescription()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTEXT, generateQueryContextForExporter(query.getContext(), projectBridgehead.getProject().getCode())},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTACT_ID, projectBridgehead.getProject().getCreatorEmail()},
                                {ProjectManagerConst.EXPORTER_PARAM_DEFAULT_OUTPUT_FORMAT, query.getOutputFormat().name()},
                                {ProjectManagerConst.EXPORTER_PARAM_DEFAULT_TEMPLATE_ID, query.getTemplateId()},
                                {ProjectManagerConst.EXPORTER_PARAM_QUERY_EXPIRATION_DATE, convertToString(projectBridgehead.getProject().getExpiresAt())}
                        }
                )
                .filter(entry -> StringUtils.hasText(entry[1])) // Filter entries with non-empty values
                .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1])); // Collect to a Map
        return convertToBase64String(result);
    }

    private String fetchLabel(@NotNull ProjectBridgehead projectBridgehead) {
        String label = "[DRN: " + projectBridgehead.getProject().getCode() + "] " + projectBridgehead.getProject().getQuery().getLabel();
        return (projectBridgehead.getExporterDispatchCounter() == 0) ? label : label + " (Attempt: " + (projectBridgehead.getExporterDispatchCounter() + 1) + ")";
    }

    private BeamRequest generateFocusBody(ProjectBridgehead projectBridgehead, TaskType taskType) throws ExporterServiceException {
        try {
            return generateFocusQueryWithoutExceptionHandling(projectBridgehead, taskType);
        } catch (BeamServiceException e) {
            throw new ExporterServiceException(e);
        }
    }

    private BeamRequest generateFocusQueryWithoutExceptionHandling(ProjectBridgehead projectBridgehead, TaskType taskType) throws BeamServiceException {
        String exporterQueryInBase64 = switch (taskType) {
            case CREATE -> generateExporterQueryInBase64ForExporterCreateQuery(projectBridgehead);
            case EXECUTE -> generateExportQueryInBase64ForExporterRequest(projectBridgehead);
            case STATUS -> generateExportStatusInBase64ForExporterRequest(projectBridgehead);
            default -> null;
        };
        return beamService.generateFocusBeamRequest(exporterQueryInBase64, taskType, projectBridgehead.getBridgehead());
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

    public Set<String> getExporterTemplates(@NotNull ProjectType projectType) {
        return switch (projectType) {
            case EXPORT -> exportTemplates;
            case DATASHIELD -> datashieldTemplates;
            case RESEARCH_ENVIRONMENT -> researchEnvironmentTemplates;
        };
    }

    private void resetProjectBridgeheadDataShield(ProjectBridgehead projectBridgehead) {
        if (projectBridgehead.getProject().getType() == ProjectType.DATASHIELD) {
            Optional<ProjectBridgeheadDataShield> projectBridgeheadInDataSHIELD = this.projectBridgeheadDataShieldRepository.findByProjectBridgehead(projectBridgehead);
            ProjectBridgeheadDataShield result;
            if (projectBridgeheadInDataSHIELD.isEmpty()) {
                result = new ProjectBridgeheadDataShield();
                result.setProjectBridgehead(projectBridgehead);
            } else {
                result = projectBridgeheadInDataSHIELD.get();
            }
            result.setRemoved(false);
            this.projectBridgeheadDataShieldRepository.save(result);
        }
    }

    public Mono<ExporterServiceResult> checkIfQueryIsAlreadySentOrExecuted(ProjectBridgehead projectBridgehead) {
        Optional<BeamRequest> focusQuery = extractFocusQuery(projectBridgehead);
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
                        Optional<OperationType> operationType = switch (projectBridgehead.getQueryState()) {
                            case SENDING -> Optional.of(OperationType.CHECK_SEND_QUERY);
                            case SENDING_AND_EXECUTING -> Optional.of(OperationType.CHECK_SEND_AND_EXECUTE_QUERY);
                            default -> Optional.empty();
                        };
                        operationType.ifPresent(type -> createBridgeheadNotification(
                                HttpStatus.OK, null, projectBridgehead, projectBridgehead.getExporterUser(),
                                type, "Checking if query is already sent or executed"));
                        return clientResponse.bodyToMono(BeamRequest[].class).filter(focusQueries -> focusQueries != null && focusQueries.length > 0).flatMap(newBeamRequest -> {
                            if (projectBridgehead.getQueryState() == QueryState.EXPORT_RUNNING_2) {
                                if (newBeamRequest[0].getBody() == null) {
                                    return Mono.empty();
                                }
                                Optional<String> decodedBody = Base64Utils.decodeIfNecessary(newBeamRequest[0].getBody());
                                if (decodedBody.isEmpty() || !decodedBody.get().contains("OK")) {
                                    if (decodedBody.isEmpty() || decodedBody.get().contains("ERROR")) {
                                        modifyProjectBridgeheadState(projectBridgehead, QueryState.ERROR);
                                    } else {
                                        modifyProjectBridgeheadState(projectBridgehead, QueryState.EXPORT_RUNNING_1);
                                    }
                                    return Mono.empty();
                                }
                            }
                            return Mono.just(new ExporterServiceResult(projectBridgehead, beamService.serializeFocusQuery(newBeamRequest[0])));
                        });
                    } else {
                        log.error("Http Error " + clientResponse.statusCode() + " checking task " + extractTaskId(focusQuery.get()) +
                                " for project " + projectBridgehead.getProject().getCode() + " and bridgehead " + projectBridgehead.getBridgehead());
                        if (isQueryStateToBeChangedToError((HttpStatus) clientResponse.statusCode(), projectBridgehead)) {
                            modifyProjectBridgeheadState(projectBridgehead, QueryState.ERROR);
                        }
                        return clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Error: {}", errorBody);
                            return Mono.error(new RuntimeException(errorBody));
                        });
                    }
                });
    }

    private void modifyProjectBridgeheadState(ProjectBridgehead projectBridgehead, QueryState newState) {
        projectBridgehead.setQueryState(newState);
        projectBridgehead.setModifiedAt(Instant.now());
        if (newState == QueryState.ERROR) {
            projectBridgehead.setExporterDispatchCounter(projectBridgehead.getExporterDispatchCounter() + 1);
            sendEmail(projectBridgehead, EmailTemplateType.ERROR_WHILE_SAVING_QUERY_IN_EXPORTER);
        }
        projectBridgeheadRepository.save(projectBridgehead);
    }

    private void sendEmail(ProjectBridgehead projectBridgehead, EmailTemplateType templateType) {
        bridgeheadAdminUserRepository.findByBridgehead(projectBridgehead.getBridgehead()).forEach(bridgeheadAdmin -> {
            emailService.sendEmail(bridgeheadAdmin.getEmail(), Optional.of(projectBridgehead.getProject().getCode()), Optional.of(projectBridgehead.getBridgehead()), ProjectRole.BRIDGEHEAD_ADMIN, templateType, emailKeyValuesFactory.newInstance().add(projectBridgehead));
        });
    }

    private String extractTaskId(BeamRequest beamRequest) {
        return (beamRequest.getId() != null) ? beamRequest.getId() : beamRequest.getTask();
    }

    private Optional<BeamRequest> extractFocusQuery(ProjectBridgehead projectBridgehead) {
        if (projectBridgehead.getExporterResponse() != null) {
            BeamRequest[] focusQueries = beamService.deserializeFocusResponse(projectBridgehead.getExporterResponse());
            if (focusQueries != null && focusQueries.length > 0) {
                return Optional.of(focusQueries[0]);
            }
        }
        return Optional.empty();
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
