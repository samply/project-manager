package de.samply.exporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadDataShield;
import de.samply.db.model.Query;
import de.samply.db.repository.ProjectBridgeheadDataShieldRepository;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.exporter.focus.FocusQuery;
import de.samply.exporter.focus.FocusService;
import de.samply.exporter.focus.FocusServiceException;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.project.ProjectType;
import de.samply.security.SessionUser;
import de.samply.utils.Base64Utils;
import de.samply.utils.WebClientFactory;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class ExporterService {

    private final FocusService focusService;
    private final WebClient webClient;
    private final int webClientMaxNumberOfRetries;
    private final int webClientTimeInSecondsAfterRetryWithFailure;
    private final int webClientRequestTimeoutInSeconds;
    private final int webClientConnectionTimeoutInSeconds;
    private final int webClientTcpKeepIdleInSeconds;
    private final int webClientTcpKeepIntervalInSeconds;
    private final int webClientTcpKeepConnetionNumberOfTries;
    private final int webClientBufferSizeInBytes;
    private final SessionUser sessionUser;
    private final ProjectRepository projectRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final ProjectBridgeheadDataShieldRepository projectBridgeheadDataShieldRepository;
    private final NotificationService notificationService;
    private final Set<String> exportTemplates;
    private final Set<String> datashieldTemplates;
    private final String focusProjectManagerId;
    private final String focusApiKey;
    private ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    public ExporterService(
            @Value(ProjectManagerConst.FOCUS_API_KEY_SV) String focusApiKey,
            @Value(ProjectManagerConst.FOCUS_PROJECT_MANAGER_ID_SV) String focusProjectManagerId,
            @Value(ProjectManagerConst.FOCUS_URL_SV) String focusUrl,
            @Value(ProjectManagerConst.EXPORT_TEMPLATES_SV) Set<String> exportTemplates,
            @Value(ProjectManagerConst.DATASHIELD_TEMPLATES_SV) Set<String> datashieldTemplates,
            @Value(ProjectManagerConst.WEBCLIENT_REQUEST_TIMEOUT_IN_SECONDS_SV) Integer webClientRequestTimeoutInSeconds,
            @Value(ProjectManagerConst.WEBCLIENT_CONNECTION_TIMEOUT_IN_SECONDS_SV) Integer webClientConnectionTimeoutInSeconds,
            @Value(ProjectManagerConst.WEBCLIENT_TCP_KEEP_IDLE_IN_SECONDS_SV) Integer webClientTcpKeepIdleInSeconds,
            @Value(ProjectManagerConst.WEBCLIENT_TCP_KEEP_INTERVAL_IN_SECONDS_SV) Integer webClientTcpKeepIntervalInSeconds,
            @Value(ProjectManagerConst.WEBCLIENT_TCP_KEEP_CONNECTION_NUMBER_OF_TRIES_SV) Integer webClientTcpKeepConnetionNumberOfTries,
            @Value(ProjectManagerConst.WEBCLIENT_MAX_NUMBER_OF_RETRIES_SV) Integer webClientMaxNumberOfRetries,
            @Value(ProjectManagerConst.WEBCLIENT_TIME_IN_SECONDS_AFTER_RETRY_WITH_FAILURE_SV) Integer webClientTimeInSecondsAfterRetryWithFailure,
            @Value(ProjectManagerConst.WEBCLIENT_BUFFER_SIZE_IN_BYTES_SV) Integer webClientBufferSizeInBytes,
            FocusService focusService,
            ProjectRepository projectRepository,
            SessionUser sessionUser,
            ProjectBridgeheadRepository projectBridgeheadRepository,
            ProjectBridgeheadDataShieldRepository projectBridgeheadDataShieldRepository,
            NotificationService notificationService,
            WebClientFactory webClientFactory) {
        this.focusService = focusService;
        this.webClientMaxNumberOfRetries = webClientMaxNumberOfRetries;
        this.webClientTimeInSecondsAfterRetryWithFailure = webClientTimeInSecondsAfterRetryWithFailure;
        this.webClientRequestTimeoutInSeconds = webClientRequestTimeoutInSeconds;
        this.webClientConnectionTimeoutInSeconds = webClientConnectionTimeoutInSeconds;
        this.webClientTcpKeepIdleInSeconds = webClientTcpKeepIdleInSeconds;
        this.webClientTcpKeepIntervalInSeconds = webClientTcpKeepIntervalInSeconds;
        this.webClientTcpKeepConnetionNumberOfTries = webClientTcpKeepConnetionNumberOfTries;
        this.webClientBufferSizeInBytes = webClientBufferSizeInBytes;
        this.sessionUser = sessionUser;
        this.projectRepository = projectRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.projectBridgeheadDataShieldRepository = projectBridgeheadDataShieldRepository;
        this.notificationService = notificationService;
        this.exportTemplates = exportTemplates;
        this.datashieldTemplates = datashieldTemplates;
        this.focusProjectManagerId = focusProjectManagerId;
        this.webClient = webClientFactory.createWebClient(focusUrl);
        this.focusApiKey = focusApiKey;
    }

    public void sendQueryToBridgehead(@NotNull String projectCode, @NotNull String bridgehead) throws ExporterServiceException {
        log.info("Sending query of project " + projectCode + " to bridgehead " + bridgehead + " ...");
        postRequest(bridgehead, projectCode, generateFocusBody(projectCode, bridgehead, false), true);
    }

    public void sendQueryToBridgeheadAndExecute(@NotNull String projectCode, @NotNull String bridgehead) throws ExporterServiceException {
        log.info("Sending query of project " + projectCode + " to bridgehead " + bridgehead + " to be executed...");
        postRequest(bridgehead, projectCode, generateFocusBody(projectCode, bridgehead, true), true);
    }

    private void postRequest(String bridgehead, String projectCode, FocusQuery focusQuery, boolean toBeExecuted) throws ExporterServiceException {
        postRequest(bridgehead, projectCode, focusQuery, toBeExecuted, 0);
        resetProjectBridgeheadDataShield(projectCode, bridgehead);
    }

    private void postRequest(String bridgehead, String projectCode, FocusQuery focusQuery, boolean toBeExecuted, int numberOfRetries) {
        String email = sessionUser.getEmail();
        webClient.post().uri(uriBuilder -> uriBuilder.path(ProjectManagerConst.FOCUS_TASK).build())
                .header(HttpHeaders.AUTHORIZATION, fetchAuthorization())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(focusQuery)
                .retrieve()
                .bodyToMono(String.class)
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
                    // We don't use the normal retry functionality of webclient, because focus requires to change the focus query ID after every retry
                    if (numberOfRetries >= webClientMaxNumberOfRetries) {
                        createBridgeheadNotification((HttpStatus) ex.getStatusCode(), error, bridgehead, projectCode, email, toBeExecuted);
                    } else {
                        waitUntilNextRetry();
                        focusQuery.setId(focusService.generateId()); // Generate new Focus Query ID
                        postRequest(bridgehead, projectCode, focusQuery, toBeExecuted, numberOfRetries + 1);
                    }
                })
                .subscribe(result -> createBridgeheadNotification(HttpStatus.OK, null, bridgehead, projectCode, email, toBeExecuted));
    }

    private void waitUntilNextRetry() {
        try {
            Thread.sleep(webClientTimeInSecondsAfterRetryWithFailure * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String fetchAuthorization() {
        return ProjectManagerConst.API_KEY + ' ' + focusProjectManagerId + ' ' + focusApiKey;
    }

    private void createBridgeheadNotification(
            HttpStatus status, String error, String bridgehead, String projectCode, String email, boolean toBeExecuted) {
        notificationService.createNotification(
                projectCode, bridgehead, email,
                fetchBridgeheadOperationType(toBeExecuted), null, error, status);
    }

    private OperationType fetchBridgeheadOperationType(boolean toBeExecuted) {
        return (toBeExecuted) ? OperationType.SEND_QUERY_TO_BRIDGEHEAD_AND_EXECUTE : OperationType.SEND_QUERY_TO_BRIDGEHEAD;
    }

    private String convertToBase64String(Object jsonObject) {
        try {
            return Base64Utils.encode(objectMapper.writeValueAsString(jsonObject));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateExportQueryInBase64ForExporterRequest(String projectCode)
            throws ExporterServiceException {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new ExporterServiceException("Project " + projectCode + " not found");
        }
        Query query = project.get().getQuery();
        Map<String, String> result = Map.of(
                ProjectManagerConst.EXPORTER_PARAM_QUERY, query.getQuery(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_FORMAT, query.getQueryFormat().name(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_LABEL, query.getLabel(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_DESCRIPTION, query.getDescription(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTEXT, generateQueryContextForExporter(query.getContext(), projectCode),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTACT_ID, project.get().getCreatorEmail(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_EXECUTION_CONTACT_ID, sessionUser.getEmail(),
                ProjectManagerConst.EXPORTER_PARAM_OUTPUT_FORMAT, query.getOutputFormat().name(),
                ProjectManagerConst.EXPORTER_PARAM_TEMPLATE_ID, query.getTemplateId(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_EXPIRATION_DATE, convertToString(project.get().getExpiresAt()));
        result.values().removeIf(value -> !StringUtils.hasText(value));
        return convertToBase64String(result);
    }

    private String generateExporterQueryInBase64ForExporterCreateQuery(String projectCode)
            throws ExporterServiceException {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new ExporterServiceException("Project " + projectCode + " not found");
        }
        Query query = project.get().getQuery();
        Map<String, String> result = Map.of(
                ProjectManagerConst.EXPORTER_PARAM_QUERY, query.getQuery(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_FORMAT, query.getQueryFormat().name(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_LABEL, query.getLabel(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_DESCRIPTION, query.getDescription(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTEXT, generateQueryContextForExporter(query.getContext(), projectCode),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTACT_ID, project.get().getCreatorEmail(),
                ProjectManagerConst.EXPORTER_PARAM_DEFAULT_OUTPUT_FORMAT, query.getOutputFormat().name(),
                ProjectManagerConst.EXPORTER_PARAM_DEFAULT_TEMPLATE_ID, query.getTemplateId(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_EXPIRATION_DATE, convertToString(project.get().getExpiresAt()));
        result.values().removeIf(value -> !StringUtils.hasText(value));
        return convertToBase64String(result);
    }

    private FocusQuery generateFocusBody(String projectCode, String bridgehead, boolean toBeExecuted) throws ExporterServiceException {
        try {
            return generateFocusQueryWithoutExceptionHandling(projectCode, bridgehead, toBeExecuted);
        } catch (FocusServiceException e) {
            throw new ExporterServiceException(e);
        }
    }

    private FocusQuery generateFocusQueryWithoutExceptionHandling(String projectCode, String bridgehead, boolean toBeExecuted) throws ExporterServiceException, FocusServiceException {
        String exporterQueryInBase64 = (toBeExecuted) ? generateExportQueryInBase64ForExporterRequest(projectCode) :
                generateExporterQueryInBase64ForExporterCreateQuery(projectCode);
        return focusService.generateFocusQuery(exporterQueryInBase64, toBeExecuted, bridgehead);
    }


    private String convertToString(LocalDate date) {
        return (date != null) ? date.format(DateTimeFormatter.ISO_DATE) : null;
    }

    private String generateQueryContextForExporter(String queryContext, String projectCode) {
        String context = ProjectManagerConst.EXPORTER_QUERY_CONTEXT_PROJECT_ID + '=' + projectCode;
        if (StringUtils.hasText(queryContext)) {
            context += ',' + queryContext;
        }
        return Base64Utils.encode(context);
    }

    public Set<String> getExporterTemplates(@NotNull ProjectType projectType) {
        return switch (projectType) {
            case EXPORT -> exportTemplates;
            case DATASHIELD -> datashieldTemplates;
        };
    }

    private void resetProjectBridgeheadDataShield(@NotNull String projectCode, @NotNull String bridgehead) throws ExporterServiceException {
        Optional<Project> project = this.projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new ExporterServiceException("Project " + projectCode + " not found");
        }
        if (project.get().getType() == ProjectType.DATASHIELD) {
            Optional<ProjectBridgehead> projectBridgehead = this.projectBridgeheadRepository.findFirstByBridgeheadAndProject(bridgehead, project.get());
            if (projectBridgehead.isEmpty()) {
                throw new ExporterServiceException("Bridgehead " + bridgehead + " for project " + projectCode + " not found");
            }
            Optional<ProjectBridgeheadDataShield> projectBridgeheadInDataSHIELD = this.projectBridgeheadDataShieldRepository.findByProjectBridgehead(projectBridgehead.get());
            ProjectBridgeheadDataShield result;
            if (projectBridgeheadInDataSHIELD.isEmpty()) {
                result = new ProjectBridgeheadDataShield();
                result.setProjectBridgehead(projectBridgehead.get());
            } else {
                result = projectBridgeheadInDataSHIELD.get();
            }
            result.setRemoved(false);
            this.projectBridgeheadDataShieldRepository.save(result);
        }
    }

}
