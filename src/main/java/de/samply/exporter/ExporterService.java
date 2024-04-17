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
import de.samply.exporter.focus.FocusQuery;
import de.samply.exporter.focus.FocusService;
import de.samply.exporter.focus.FocusServiceException;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.project.ProjectType;
import de.samply.query.QueryState;
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
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class ExporterService {
    private final ProjectBridgeheadRepository projectBridgeheadRepository;

    private final FocusService focusService;
    private final WebClient webClient;
    private final int webClientMaxNumberOfRetries;
    private final int webClientTimeInSecondsAfterRetryWithFailure;
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
            @Value(ProjectManagerConst.WEBCLIENT_MAX_NUMBER_OF_RETRIES_SV) Integer webClientMaxNumberOfRetries,
            @Value(ProjectManagerConst.WEBCLIENT_TIME_IN_SECONDS_AFTER_RETRY_WITH_FAILURE_SV) Integer webClientTimeInSecondsAfterRetryWithFailure,
            FocusService focusService,
            ProjectBridgeheadDataShieldRepository projectBridgeheadDataShieldRepository,
            NotificationService notificationService,
            WebClientFactory webClientFactory,
            ProjectBridgeheadRepository projectBridgeheadRepository) {
        this.focusService = focusService;
        this.webClientMaxNumberOfRetries = webClientMaxNumberOfRetries;
        this.webClientTimeInSecondsAfterRetryWithFailure = webClientTimeInSecondsAfterRetryWithFailure;
        this.projectBridgeheadDataShieldRepository = projectBridgeheadDataShieldRepository;
        this.notificationService = notificationService;
        this.exportTemplates = exportTemplates;
        this.datashieldTemplates = datashieldTemplates;
        this.focusProjectManagerId = focusProjectManagerId;
        this.webClient = webClientFactory.createWebClient(focusUrl);
        this.focusApiKey = focusApiKey;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
    }

    public Mono<ExporterServiceResult> sendQueryToBridgehead(ProjectBridgehead projectBridgehead) throws ExporterServiceException {
        log.info("Sending query of project " + projectBridgehead.getProject().getCode() + " to bridgehead " + projectBridgehead.getBridgehead() + " ...");
        return postRequest(projectBridgehead, generateFocusBody(projectBridgehead, false), true);
    }

    public Mono<ExporterServiceResult> sendQueryToBridgeheadAndExecute(ProjectBridgehead projectBridgehead) throws ExporterServiceException {
        log.info("Sending query of project " + projectBridgehead.getProject().getCode() + " to bridgehead " + projectBridgehead.getBridgehead() + " to be executed...");
        return postRequest(projectBridgehead, generateFocusBody(projectBridgehead, true), true);
    }

    private Mono<ExporterServiceResult> postRequest(ProjectBridgehead projectBridgehead, FocusQuery focusQuery, boolean toBeExecuted) throws ExporterServiceException {
        Mono<ExporterServiceResult> result = postRequest(projectBridgehead, focusQuery, toBeExecuted, 0);
        result.subscribe(r -> resetProjectBridgeheadDataShield(projectBridgehead));
        return result;
    }

    private Mono<ExporterServiceResult> postRequest(ProjectBridgehead projectBridgehead, FocusQuery focusQuery, boolean toBeExecuted, int numberOfRetries) {
        Mono<String> result = webClient.post().uri(uriBuilder -> uriBuilder.path(ProjectManagerConst.FOCUS_TASK).build())
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
                        createBridgeheadNotification((HttpStatus) ex.getStatusCode(), error, projectBridgehead, projectBridgehead.getExporterUser(), toBeExecuted);
                        projectBridgehead.setQueryState(QueryState.ERROR);
                        projectBridgeheadRepository.save(projectBridgehead);
                    } else {
                        waitUntilNextRetry();
                        focusQuery.setId(focusService.generateId()); // Generate new Focus Query ID
                        postRequest(projectBridgehead, focusQuery, toBeExecuted, numberOfRetries + 1);
                    }
                });
        result.subscribe(r -> createBridgeheadNotification(HttpStatus.OK, null, projectBridgehead, projectBridgehead.getExporterUser(), toBeExecuted));
        return result.flatMap(r -> Mono.just(new ExporterServiceResult(projectBridgehead, r)));
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
            HttpStatus status, String error, ProjectBridgehead projectBridgehead, String email, boolean toBeExecuted) {
        notificationService.createNotification(
                projectBridgehead.getProject().getCode(), projectBridgehead.getBridgehead(), email,
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

    private String generateExportQueryInBase64ForExporterRequest(ProjectBridgehead projectBridgehead)
            throws ExporterServiceException {
        Query query = projectBridgehead.getProject().getQuery();
        Map<String, String> result = Map.of(
                ProjectManagerConst.EXPORTER_PARAM_QUERY, query.getQuery(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_FORMAT, query.getQueryFormat().name(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_LABEL, query.getLabel(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_DESCRIPTION, query.getDescription(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTEXT, generateQueryContextForExporter(query.getContext(), projectBridgehead.getProject().getCode()),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTACT_ID, projectBridgehead.getProject().getCreatorEmail(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_EXECUTION_CONTACT_ID, projectBridgehead.getExporterUser(),
                ProjectManagerConst.EXPORTER_PARAM_OUTPUT_FORMAT, query.getOutputFormat().name(),
                ProjectManagerConst.EXPORTER_PARAM_TEMPLATE_ID, query.getTemplateId(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_EXPIRATION_DATE, convertToString(projectBridgehead.getProject().getExpiresAt()));
        result.values().removeIf(value -> !StringUtils.hasText(value));
        return convertToBase64String(result);
    }

    private String generateExporterQueryInBase64ForExporterCreateQuery(Project project)
            throws ExporterServiceException {
        Query query = project.getQuery();
        Map<String, String> result = Map.of(
                ProjectManagerConst.EXPORTER_PARAM_QUERY, query.getQuery(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_FORMAT, query.getQueryFormat().name(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_LABEL, query.getLabel(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_DESCRIPTION, query.getDescription(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTEXT, generateQueryContextForExporter(query.getContext(), project.getCode()),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTACT_ID, project.getCreatorEmail(),
                ProjectManagerConst.EXPORTER_PARAM_DEFAULT_OUTPUT_FORMAT, query.getOutputFormat().name(),
                ProjectManagerConst.EXPORTER_PARAM_DEFAULT_TEMPLATE_ID, query.getTemplateId(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_EXPIRATION_DATE, convertToString(project.getExpiresAt()));
        result.values().removeIf(value -> !StringUtils.hasText(value));
        return convertToBase64String(result);
    }

    private FocusQuery generateFocusBody(ProjectBridgehead projectBridgehead, boolean toBeExecuted) throws ExporterServiceException {
        try {
            return generateFocusQueryWithoutExceptionHandling(projectBridgehead, toBeExecuted);
        } catch (FocusServiceException e) {
            throw new ExporterServiceException(e);
        }
    }

    private FocusQuery generateFocusQueryWithoutExceptionHandling(ProjectBridgehead projectBridgehead, boolean toBeExecuted) throws FocusServiceException {
        String exporterQueryInBase64 = (toBeExecuted) ? generateExportQueryInBase64ForExporterRequest(projectBridgehead) :
                generateExporterQueryInBase64ForExporterCreateQuery(projectBridgehead.getProject());
        return focusService.generateFocusQuery(exporterQueryInBase64, toBeExecuted, projectBridgehead.getBridgehead());
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

    public Mono<ExporterServiceResult> checkIfQueryIsAlreadySent(ProjectBridgehead projectBridgehead) throws ExporterServiceException {
        //TODO
        return Mono.just(new ExporterServiceResult(projectBridgehead, "TODO"));
    }

    public Mono<ExporterServiceResult> checkIfQueryIsAlreadySentAndExecuted(ProjectBridgehead projectBridgehead) throws ExporterServiceException {
        //TODO
        return Mono.just(new ExporterServiceResult(projectBridgehead, "TODO"));
    }

}
