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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
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
    private final ProjectBridgeheadDataShieldRepository projectBridgeheadDataShieldRepository;
    private final NotificationService notificationService;
    private final Set<String> exportTemplates;
    private final Set<String> datashieldTemplates;
    private final String focusProjectManagerId;
    private final String focusApiKey;

    private final String focusWaitTime;
    private final String focusWaitCount;
    private ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    public ExporterService(
            @Value(ProjectManagerConst.FOCUS_API_KEY_SV) String focusApiKey,
            @Value(ProjectManagerConst.FOCUS_PROJECT_MANAGER_ID_SV) String focusProjectManagerId,
            @Value(ProjectManagerConst.FOCUS_URL_SV) String focusUrl,
            @Value(ProjectManagerConst.EXPORT_TEMPLATES_SV) Set<String> exportTemplates,
            @Value(ProjectManagerConst.DATASHIELD_TEMPLATES_SV) Set<String> datashieldTemplates,
            @Value(ProjectManagerConst.FOCUS_TTL_SV) String focusWaitTime,
            @Value(ProjectManagerConst.FOCUS_FAILURE_STRATEGY_MAX_TRIES_SV) String focusWaitCount,
            FocusService focusService,
            ProjectBridgeheadDataShieldRepository projectBridgeheadDataShieldRepository,
            NotificationService notificationService,
            WebClientFactory webClientFactory,
            ProjectBridgeheadRepository projectBridgeheadRepository) {
        this.focusService = focusService;
        this.projectBridgeheadDataShieldRepository = projectBridgeheadDataShieldRepository;
        this.notificationService = notificationService;
        this.exportTemplates = exportTemplates;
        this.datashieldTemplates = datashieldTemplates;
        this.focusProjectManagerId = focusProjectManagerId;
        this.focusWaitTime = focusWaitTime;
        this.focusWaitCount = focusWaitCount;
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

    private Mono<ExporterServiceResult> postRequest(ProjectBridgehead projectBridgehead, FocusQuery focusQuery, boolean toBeExecuted) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path(ProjectManagerConst.FOCUS_TASK_PATH).build())
                .header(HttpHeaders.AUTHORIZATION, fetchAuthorization())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(focusQuery)
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().equals(HttpStatus.CREATED)) {
                        createBridgeheadNotification(HttpStatus.OK, null, projectBridgehead, projectBridgehead.getExporterUser(), fetchBridgeheadOperationType(toBeExecuted));
                        resetProjectBridgeheadDataShield(projectBridgehead);
                        return Mono.just(new ExporterServiceResult(projectBridgehead, focusQuery.getId()));
                    } else {
                        log.error("Http Error " + clientResponse.statusCode() + " posting task " + focusQuery.getId() + " : " + focusQuery.getBody() +
                                " for project " + projectBridgehead.getProject().getCode() + " and bridgehead " + projectBridgehead.getBridgehead());
                        return clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Error: {}", errorBody);
                            return Mono.error(new RuntimeException(errorBody));
                        });
                    }
                });
    }

    private String fetchAuthorization() {
        return ProjectManagerConst.API_KEY + ' ' + focusProjectManagerId + ' ' + focusApiKey;
    }

    private void createBridgeheadNotification(
            HttpStatus status, String error, ProjectBridgehead projectBridgehead, String email, OperationType operationType) {
        notificationService.createNotification(
                projectBridgehead.getProject().getCode(), projectBridgehead.getBridgehead(), email, operationType, null, error, status);
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
            context += ProjectManagerConst.EXPORTER_QUERY_CONTEXT_SEPARATOR + queryContext;
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

    public Mono<ExporterServiceResult> checkIfQueryIsAlreadySent(ProjectBridgehead projectBridgehead) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(ProjectManagerConst.FOCUS_TASK_PATH + "/" + projectBridgehead.getExporterResponse() + ProjectManagerConst.FOCUS_TASK_RESULTS_PATH)
                        .queryParam(ProjectManagerConst.FOCUS_TASK_WAIT_TIME_PARAM, focusWaitTime)
                        .queryParam(ProjectManagerConst.FOCUS_TASK_WAIT_COUNT_PARAM, focusWaitCount).build())
                .header(HttpHeaders.AUTHORIZATION, fetchAuthorization())
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().equals(HttpStatus.OK) || clientResponse.statusCode().equals(HttpStatus.PARTIAL_CONTENT)) {
                        OperationType operationType = projectBridgehead.getQueryState() == QueryState.SENDING ? OperationType.CHECK_SEND_QUERY : OperationType.CHECK_SEND_AND_EXECUTE_QUERY;
                        createBridgeheadNotification(HttpStatus.OK, null, projectBridgehead, projectBridgehead.getExporterUser(), operationType);
                        return clientResponse.bodyToMono(String.class).flatMap(body -> Mono.just(new ExporterServiceResult(projectBridgehead, body)));
                    } else {
                        log.error("Http Error " + clientResponse.statusCode() + " checking task " + projectBridgehead.getExporterResponse() +
                                " for project " + projectBridgehead.getProject().getCode() + " and bridgehead " + projectBridgehead.getBridgehead());
                        if (!clientResponse.statusCode().equals(HttpStatus.NO_CONTENT)) {
                            projectBridgehead.setQueryState(QueryState.ERROR);
                            projectBridgehead.setModifiedAt(Instant.now());
                            projectBridgeheadRepository.save(projectBridgehead);
                        }
                        return clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Error: {}", errorBody);
                            return Mono.error(new RuntimeException(errorBody));
                        });
                    }
                });
    }

    public Optional<String> fetchExporterExecutionIdFromExporterResponse(String exporterResponse) {
        if (exporterResponse != null) {
            Optional<FocusQuery[]> focusQuery = deserializeFocusResponse(exporterResponse);
            if (focusQuery.isPresent() && focusQuery.get().length > 0 && focusQuery.get()[0].getBody() != null) {
                return fetchQueryExecutionIdFromQueryExecutionIdUrl(Base64Utils.decode(focusQuery.get()[0].getBody()));
            }
        }
        return Optional.empty();
    }

    private Optional<FocusQuery[]> deserializeFocusResponse(String exporterResponse) {
        try {
            return Optional.of(focusService.deserializeFocusResponse(exporterResponse));
        } catch (FocusServiceException e) {
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
