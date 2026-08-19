package de.samply.feasibility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import de.samply.beam.BeamRequest;
import de.samply.beam.BeamService;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.utils.Base64Utils;
import de.samply.utils.WebClientFactory;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class FeasibilityService {

    private static final String SUCCEEDED_STATUS = "succeeded";
    private static final String CLAIMED_STATUS = "claimed";
    private static final String TEMP_FAILED_STATUS = "tempfailed";

    private final WebClient webClient;
    private final BeamService beamService;
    private final ObjectMapper objectMapper;
    private final String projectManagerId;
    private final String beamApiKey;
    private final String beamTtl;
    private final String resultWaitTime;
    private final int resultMaxTries;
    private final String focusProject;
    private final boolean enabled;

    @Autowired
    public FeasibilityService(
            @Value(ProjectManagerConst.BEAM_URL_SV) String beamUrl,
            @Value(ProjectManagerConst.BEAM_PROJECT_MANAGER_ID_SV) String projectManagerId,
            @Value(ProjectManagerConst.BEAM_API_KEY_SV) String beamApiKey,
            @Value(ProjectManagerConst.FEASIBILITY_BEAM_TTL_SV) String beamTtl,
            @Value(ProjectManagerConst.FEASIBILITY_BEAM_RESULT_WAIT_TIME_SV) String resultWaitTime,
            @Value(ProjectManagerConst.FEASIBILITY_BEAM_RESULT_MAX_TRIES_SV) int resultMaxTries,
            @Value(ProjectManagerConst.FOCUS_LENS_PROJECT_SV) String focusProject,
            @Value(ProjectManagerConst.ENABLE_FEASIBILITY_SV) boolean enabled,
            WebClientFactory webClientFactory,
            BeamService beamService) {
        this(webClientFactory.createWebClient(beamUrl), beamService, projectManagerId,
                beamApiKey, beamTtl, resultWaitTime, resultMaxTries, focusProject, enabled);
    }

    FeasibilityService(WebClient webClient, BeamService beamService,
                       String projectManagerId, String beamApiKey, String beamTtl,
                       String resultWaitTime, int resultMaxTries, String focusProject,
                       boolean enabled) {
        if (resultMaxTries < 1) {
            throw new IllegalArgumentException("Feasibility Beam result max tries must be at least 1");
        }
        this.webClient = webClient;
        this.beamService = beamService;
        this.objectMapper = new ObjectMapper();
        this.projectManagerId = projectManagerId;
        this.beamApiKey = beamApiKey;
        this.beamTtl = beamTtl;
        this.resultWaitTime = resultWaitTime;
        this.resultMaxTries = resultMaxTries;
        this.focusProject = focusProject;
        this.enabled = enabled;
    }

    public Mono<JsonNode> fetchFeasibility(@NotNull Project project,
                                           @NotNull ProjectBridgehead bridgehead) {
        if (!enabled) {
            return Mono.empty();
        }
        BeamRequest request = beamService.generateFeasibilityBeamRequest(
                project.getQuery().getQuery(), bridgehead.getBridgehead(), focusProject, beamTtl);

        return createTask(request)
                .thenMany(Mono.defer(() -> fetchResult(request.getId()))
                        .repeat(resultMaxTries - 1L))
                .next()
                .switchIfEmpty(Mono.error(new FeasibilityServiceException(
                        "Beam returned no feasibility result for task " + request.getId() +
                                " after " + resultMaxTries + " tries")));
    }

    private Mono<Void> createTask(BeamRequest request) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path(ProjectManagerConst.BEAM_TASK_PATH).build())
                .header(HttpHeaders.AUTHORIZATION, authorization())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.releaseBody();
                    }
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> Mono.error(new FeasibilityServiceException(
                                    "Beam rejected feasibility task with status " + response.statusCode() + responseDetails(body))));
                });
    }

    private Mono<JsonNode> fetchResult(String taskId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(ProjectManagerConst.BEAM_TASK_PATH + "/" + taskId + ProjectManagerConst.BEAM_TASK_RESULTS_PATH)
                        .queryParam(ProjectManagerConst.BEAM_TASK_WAIT_TIME_PARAM, resultWaitTime)
                        .queryParam(ProjectManagerConst.BEAM_TASK_WAIT_COUNT_PARAM, 1)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authorization())
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(BeamRequest[].class)
                                .defaultIfEmpty(new BeamRequest[0])
                                .flatMap(results -> extractResult(taskId, results));
                    }
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> Mono.error(new FeasibilityServiceException(
                                    "Beam feasibility result request failed with status " + response.statusCode() + responseDetails(body))));
                });
    }

    private Mono<JsonNode> extractResult(String taskId, BeamRequest[] results) {
        return Arrays.stream(results)
                .filter(result -> SUCCEEDED_STATUS.equalsIgnoreCase(result.getStatus()))
                .findFirst()
                .map(this::decodeResult)
                .orElseGet(() -> failureResult(taskId, results));
    }

    private Mono<JsonNode> failureResult(String taskId, BeamRequest[] results) {
        String failures = Arrays.stream(results)
                .filter(result -> result.getStatus() != null &&
                        !CLAIMED_STATUS.equalsIgnoreCase(result.getStatus()) &&
                        !TEMP_FAILED_STATUS.equalsIgnoreCase(result.getStatus()))
                .map(result -> result.getStatus() + responseDetails(
                        Base64Utils.decodeIfNecessary(result.getBody()).orElse("")))
                .collect(Collectors.joining("; "));
        if (!failures.isBlank()) {
            return Mono.error(new FeasibilityServiceException(
                    "Focus feasibility task " + taskId + " failed: " + failures));
        }
        return Mono.empty();
    }

    private Mono<JsonNode> decodeResult(BeamRequest result) {
        return Mono.fromCallable(() -> {
            String decodedBody = Base64Utils.decodeIfNecessary(result.getBody())
                    .orElseThrow(() -> new FeasibilityServiceException("Focus feasibility result body is missing"));
            try {
                return objectMapper.readTree(decodedBody);
            } catch (JsonProcessingException exception) {
                throw new FeasibilityServiceException("Focus feasibility result is not valid JSON", exception);
            }
        });
    }

    private String authorization() {
        return ProjectManagerConst.API_KEY + ' ' + projectManagerId + ' ' + beamApiKey;
    }

    private String responseDetails(String body) {
        return body.isBlank() ? "" : ": " + body;
    }
}
