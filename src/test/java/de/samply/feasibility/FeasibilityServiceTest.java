package de.samply.feasibility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.Query;
import de.samply.beam.BeamService;
import de.samply.utils.Base64Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FeasibilityServiceTest {

    private static final String PROJECT_MANAGER_ID = "project-manager.central.broker";
    private static final String BEAM_API_KEY = "beam-secret";
    private static final String BRIDGEHEAD = "berlin";
    private static final String FOCUS_BEAM_ID = "focus.berlin.broker";
    private static final String FOCUS_PROJECT = "dktk";
    private static final String QUERY = "encoded-ast-data-query";
    private static final String FEASIBILITY_TTL = "360s";
    private static final String RESULT_WAIT_TIME = "20s";
    private static final int RESULT_MAX_TRIES = 3;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<JsonNode> postedTask = new AtomicReference<>();
    private final AtomicReference<String> postAuthorization = new AtomicReference<>();
    private final AtomicReference<String> getAuthorization = new AtomicReference<>();
    private final AtomicReference<String> resultRequestUri = new AtomicReference<>();
    private final AtomicInteger resultRequestCount = new AtomicInteger();

    private HttpServer server;
    private int postStatus;
    private String postResponse;
    private int getStatus;
    private String getResponse;
    private int emptyResultResponses;
    private int temporaryFailureResponses;

    @BeforeEach
    void setUp() throws IOException {
        postStatus = 201;
        postResponse = "";
        getStatus = 200;
        getResponse = successfulBeamResult("{\"totals\":{\"patients\":42},\"stratifiers\":{}}");
        emptyResultResponses = 0;
        temporaryFailureResponses = 0;

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/tasks", this::handleBeamRequest);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendsLensStyleBeamTaskAndReturnsDecodedFocusResult() {
        FeasibilityService service = createService();

        StepVerifier.create(service.fetchFeasibility(project(), projectBridgehead()))
                .assertNext(result -> assertThat(result.at("/totals/patients").asInt()).isEqualTo(42))
                .verifyComplete();

        JsonNode request = postedTask.get();
        assertThat(request.get("id").asText()).isNotBlank();
        assertThat(request.get("body").asText()).isEqualTo(QUERY);
        assertThat(request.get("from").asText()).isEqualTo(PROJECT_MANAGER_ID);
        assertThat(request.at("/to/0").asText()).isEqualTo(FOCUS_BEAM_ID);
        assertThat(request.get("ttl").asText()).isEqualTo(FEASIBILITY_TTL);
        assertThat(request.at("/metadata/project").asText()).isEqualTo(FOCUS_PROJECT);
        assertThat(request.at("/metadata/transform").asText()).isEqualTo("LENS");
        assertThat(request.get("metadata").has("task_type")).isFalse();
        assertThat(request.at("/failure_strategy/retry/backoff_millisecs").asInt()).isEqualTo(1000);
        assertThat(request.at("/failure_strategy/retry/max_tries").asInt()).isEqualTo(5);
        assertThat(postAuthorization.get()).isEqualTo("ApiKey " + PROJECT_MANAGER_ID + " " + BEAM_API_KEY);
        assertThat(getAuthorization.get()).isEqualTo(postAuthorization.get());
        assertThat(resultRequestUri.get())
                .startsWith("/v1/tasks/" + request.get("id").asText() + "/results?")
                .contains("wait_time=" + RESULT_WAIT_TIME)
                .contains("wait_count=1");
    }

    @Test
    void retriesResultRequestUntilFocusAnswers() {
        emptyResultResponses = 2;

        StepVerifier.create(createService().fetchFeasibility(project(), projectBridgehead()))
                .assertNext(result -> assertThat(result.at("/totals/patients").asInt()).isEqualTo(42))
                .verifyComplete();

        assertThat(resultRequestCount.get()).isEqualTo(3);
    }

    @Test
    void retriesTemporaryBeamFailureUntilFocusAnswers() {
        temporaryFailureResponses = 1;

        StepVerifier.create(createService().fetchFeasibility(project(), projectBridgehead()))
                .assertNext(result -> assertThat(result.at("/totals/patients").asInt()).isEqualTo(42))
                .verifyComplete();

        assertThat(resultRequestCount.get()).isEqualTo(2);
    }

    @Test
    void stopsAfterConfiguredNumberOfResultRequests() {
        emptyResultResponses = RESULT_MAX_TRIES;

        StepVerifier.create(createService().fetchFeasibility(project(), projectBridgehead()))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(FeasibilityServiceException.class);
                    assertThat(error.getMessage()).contains("after " + RESULT_MAX_TRIES + " tries");
                })
                .verify();

        assertThat(resultRequestCount.get()).isEqualTo(RESULT_MAX_TRIES);
    }

    @Test
    void stopsWhenBeamRejectsTaskCreation() {
        postStatus = 502;
        postResponse = "beam unavailable";

        StepVerifier.create(createService().fetchFeasibility(project(), projectBridgehead()))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(FeasibilityServiceException.class);
                    assertThat(error.getMessage()).contains("502 BAD_GATEWAY", "beam unavailable");
                })
                .verify();

        assertThat(resultRequestCount.get()).isZero();
    }

    @Test
    void rejectsBeamResponseWithoutSuccessfulResult() {
        getResponse = "[{\"body\":\"" + Base64Utils.encode("Focus rejected the query") +
                "\",\"from\":\"focus.berlin.broker\",\"status\":\"permafailed\"}]";

        StepVerifier.create(createService().fetchFeasibility(project(), projectBridgehead()))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(FeasibilityServiceException.class);
                    assertThat(error.getMessage()).contains("permafailed", "Focus rejected the query");
                })
                .verify();
    }

    @Test
    void rejectsInvalidFocusResultJson() {
        getResponse = successfulBeamResult("not-json");

        StepVerifier.create(createService().fetchFeasibility(project(), projectBridgehead()))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(FeasibilityServiceException.class);
                    assertThat(error.getMessage()).contains("not valid JSON");
                })
                .verify();
    }

    @Test
    void doesNotContactBeamWhenFeasibilityIsDisabled() {
        StepVerifier.create(createService(false).fetchFeasibility(project(), projectBridgehead()))
                .verifyComplete();

        assertThat(postedTask.get()).isNull();
        assertThat(resultRequestCount.get()).isZero();
    }

    private FeasibilityService createService() {
        return createService(true);
    }

    private FeasibilityService createService(boolean enabled) {
        BridgeheadConfiguration bridgeheadConfiguration = new BridgeheadConfiguration();
        BridgeheadConfiguration.BridgeheadConfig config = new BridgeheadConfiguration.BridgeheadConfig();
        config.setFocusBeamId(FOCUS_BEAM_ID);
        bridgeheadConfiguration.setConfig(Map.of(BRIDGEHEAD, config));

        BeamService beamService = new BeamService(PROJECT_MANAGER_ID, "30s", 1000, 5,
                bridgeheadConfiguration);
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .build();
        return new FeasibilityService(webClient, beamService, PROJECT_MANAGER_ID,
                BEAM_API_KEY, FEASIBILITY_TTL, RESULT_WAIT_TIME, RESULT_MAX_TRIES,
                FOCUS_PROJECT, enabled);
    }

    private Project project() {
        Query query = new Query();
        query.setQuery(QUERY);
        Project project = new Project();
        project.setQuery(query);
        return project;
    }

    private ProjectBridgehead projectBridgehead() {
        ProjectBridgehead projectBridgehead = new ProjectBridgehead();
        projectBridgehead.setBridgehead(BRIDGEHEAD);
        return projectBridgehead;
    }

    private String successfulBeamResult(String focusResult) {
        return "[{\"body\":\"" + Base64Utils.encode(focusResult) +
                "\",\"from\":\"" + FOCUS_BEAM_ID +
                "\",\"status\":\"succeeded\",\"task\":\"task-id\",\"to\":[\"" +
                PROJECT_MANAGER_ID + "\"]}]";
    }

    private void handleBeamRequest(HttpExchange exchange) throws IOException {
        try (exchange) {
            if ("POST".equals(exchange.getRequestMethod())) {
                postAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                postedTask.set(objectMapper.readTree(exchange.getRequestBody()));
                sendResponse(exchange, postStatus, postResponse);
                return;
            }
            if ("GET".equals(exchange.getRequestMethod())) {
                int requestNumber = resultRequestCount.incrementAndGet();
                getAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                resultRequestUri.set(exchange.getRequestURI().toString());
                String response = requestNumber <= emptyResultResponses
                        ? "[]"
                        : requestNumber <= emptyResultResponses + temporaryFailureResponses
                        ? "[{\"status\":\"tempfailed\"}]"
                        : getResponse;
                sendResponse(exchange, getStatus, response);
                return;
            }
            sendResponse(exchange, 405, "");
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String response) throws IOException {
        byte[] body = response.getBytes(StandardCharsets.UTF_8);
        if (!response.isEmpty()) {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
        }
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }
}
