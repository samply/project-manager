package de.samply.exporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.bridgehead.BridgeheadOperationType;
import de.samply.db.model.BridgeheadOperation;
import de.samply.db.model.Project;
import de.samply.db.model.Query;
import de.samply.db.repository.BridgeheadOperationRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.security.SessionUser;
import de.samply.utils.Base64Utils;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class ExporterService {

    private final BridgeheadConfiguration bridgeheadConfiguration;
    private final Map<String, WebClient> bridgeheadWebClientMap = new HashMap<>();
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
    private final BridgeheadOperationRepository bridgeheadOperationRepository;
    private ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    public ExporterService(
            @Value(ProjectManagerConst.WEBCLIENT_REQUEST_TIMEOUT_IN_SECONDS_SV) Integer webClientRequestTimeoutInSeconds,
            @Value(ProjectManagerConst.WEBCLIENT_CONNECTION_TIMEOUT_IN_SECONDS_SV) Integer webClientConnectionTimeoutInSeconds,
            @Value(ProjectManagerConst.WEBCLIENT_TCP_KEEP_IDLE_IN_SECONDS_SV) Integer webClientTcpKeepIdleInSeconds,
            @Value(ProjectManagerConst.WEBCLIENT_TCP_KEEP_INTERVAL_IN_SECONDS_SV) Integer webClientTcpKeepIntervalInSeconds,
            @Value(ProjectManagerConst.WEBCLIENT_TCP_KEEP_CONNECTION_NUMBER_OF_TRIES_SV) Integer webClientTcpKeepConnetionNumberOfTries,
            @Value(ProjectManagerConst.WEBCLIENT_MAX_NUMBER_OF_RETRIES_SV) Integer webClientMaxNumberOfRetries,
            @Value(ProjectManagerConst.WEBCLIENT_TIME_IN_SECONDS_AFTER_RETRY_WITH_FAILURE_SV) Integer webClientTimeInSecondsAfterRetryWithFailure,
            @Value(ProjectManagerConst.WEBCLIENT_BUFFER_SIZE_IN_BYTES_SV) Integer webClientBufferSizeInBytes,
            BridgeheadConfiguration bridgeheadConfiguration,
            ProjectRepository projectRepository,
            SessionUser sessionUser,
            BridgeheadOperationRepository bridgeheadOperationRepository) {
        this.bridgeheadConfiguration = bridgeheadConfiguration;
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
        this.bridgeheadOperationRepository = bridgeheadOperationRepository;
    }

    private WebClient createWebClient(String exporterUrl) {
        return WebClient.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(webClientBufferSizeInBytes))
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(webClientRequestTimeoutInSeconds))
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, webClientConnectionTimeoutInSeconds * 1000)
                                .option(ChannelOption.SO_KEEPALIVE, true)
                                .option(EpollChannelOption.TCP_KEEPIDLE, webClientTcpKeepIdleInSeconds)
                                .option(EpollChannelOption.TCP_KEEPINTVL, webClientTcpKeepIntervalInSeconds)
                                .option(EpollChannelOption.TCP_KEEPCNT, webClientTcpKeepConnetionNumberOfTries)
                ))
                .baseUrl(exporterUrl).build();
    }

    public void sendQueryToBridgehead(@NotNull String projectCode, @NotNull String bridgehead) throws ExporterServiceException {
        log.info("Sending query of project " + projectCode + " to bridgehead " + bridgehead + " ...");
        postRequest(bridgehead, projectCode, ProjectManagerConst.EXPORTER_CREATE_QUERY, generateBodyConfigurationForExportRequest(projectCode));
    }

    public void sendQueryToBridgeheadAndExecute(@NotNull String projectCode, @NotNull String bridgehead) throws ExporterServiceException {
        log.info("Sending query of project " + projectCode + " to bridgehead " + bridgehead + " to be executed...");
        postRequest(bridgehead, projectCode, ProjectManagerConst.EXPORTER_REQUEST, generateBodyConfigurationForExportCreateQuery(projectCode));
    }

    private void postRequest(String bridgehead, String projectCode, String restService, Map<String, String> bodyParameters) {
        AtomicInteger retryCount = new AtomicInteger(0);
        String email = sessionUser.getEmail();
        fetchWebClient(bridgehead).post().uri(uriBuilder -> uriBuilder.path(restService).build())
                .header(ProjectManagerConst.HTTP_HEADER_API_KEY, bridgeheadConfiguration.getExporterApiKey(bridgehead))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createBody(bodyParameters))
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(
                        Retry.fixedDelay(webClientMaxNumberOfRetries, Duration.ofSeconds(webClientTimeInSecondsAfterRetryWithFailure))
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
                    if (retryCount.get() >= webClientMaxNumberOfRetries) {
                        createBridgeheadOperation(restService, (HttpStatus) ex.getStatusCode(), error, bridgehead, projectCode, email);
                    }
                })
                .subscribe(result -> createBridgeheadOperation(restService, HttpStatus.OK, null, bridgehead, projectCode, email));
    }

    private void createBridgeheadOperation(
            String restService, HttpStatus status, String error, String bridgehead, String projectCode, String email) {
        BridgeheadOperation bridgeheadOperation = new BridgeheadOperation();
        bridgeheadOperation.setBridgehead(bridgehead);
        bridgeheadOperation.setProject(projectRepository.findByCode(projectCode).get());
        bridgeheadOperation.setTimestamp(LocalDate.now());
        bridgeheadOperation.setType(fetchBridgeheadOperationType(restService));
        bridgeheadOperation.setHttpStatus(status);
        bridgeheadOperation.setError(error);
        bridgeheadOperation.setUserEmail(email);
        bridgeheadOperationRepository.save(bridgeheadOperation);
    }

    private BridgeheadOperationType fetchBridgeheadOperationType(String restService) {
        return switch (restService) {
            case ProjectManagerConst.EXPORTER_REQUEST -> BridgeheadOperationType.SEND_QUERY_TO_BRIDGEHEAD_AND_EXECUTE;
            case ProjectManagerConst.EXPORTER_CREATE_QUERY -> BridgeheadOperationType.SEND_QUERY_TO_BRIDGEHEAD;
            default -> throw new IllegalStateException("Unexpected value: " + restService);
        };
    }

    private WebClient fetchWebClient(String bridgehead) {
        WebClient webClient = bridgeheadWebClientMap.get(bridgehead);
        if (webClient == null) {
            webClient = createWebClient(bridgeheadConfiguration.getExporterUrl(bridgehead));
            bridgeheadWebClientMap.put(bridgehead, webClient);
        }
        return webClient;
    }

    private String createBody(Map<String, String> bodyParameters) {
        try {
            return Base64Utils.encode(objectMapper.writeValueAsString(bodyParameters));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> generateBodyConfigurationForExportRequest(String projectCode)
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
                ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTEXT, generateQueryContextForExporter(projectCode),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTACT_ID, project.get().getCreatorEmail(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_EXECUTION_CONTACT_ID, sessionUser.getEmail(),
                ProjectManagerConst.EXPORTER_PARAM_OUTPUT_FORMAT, query.getOutputFormat().name(),
                ProjectManagerConst.EXPORTER_PARAM_TEMPLATE_ID, query.getTemplateId(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_EXPIRATION_DATE, convertToString(project.get().getExpiresAt()));
        result.values().removeIf(value -> !StringUtils.hasText(value));
        return result;
    }

    private Map<String, String> generateBodyConfigurationForExportCreateQuery(String projectCode)
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
                ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTEXT, generateQueryContextForExporter(projectCode),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_CONTACT_ID, project.get().getCreatorEmail(),
                ProjectManagerConst.EXPORTER_PARAM_DEFAULT_OUTPUT_FORMAT, query.getOutputFormat().name(),
                ProjectManagerConst.EXPORTER_PARAM_DEFAULT_TEMPLATE_ID, query.getTemplateId(),
                ProjectManagerConst.EXPORTER_PARAM_QUERY_EXPIRATION_DATE, convertToString(project.get().getExpiresAt()));
        result.values().removeIf(value -> !StringUtils.hasText(value));
        return result;
    }

    private String convertToString(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_DATE);
    }

    private String generateQueryContextForExporter(String projectCode) {
        String context = ProjectManagerConst.EXPORTER_QUERY_CONTEXT_PROJECT_ID + '=' + projectCode;
        return Base64Utils.encode(context);
    }


}
