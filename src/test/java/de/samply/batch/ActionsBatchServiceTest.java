package de.samply.batch;

import de.samply.annotations.FrontendAction;
import de.samply.annotations.ProjectCode;
import de.samply.annotations.RequestParameter;
import de.samply.annotations.RequestVariable;
import de.samply.annotations.RoleConstraints;
import de.samply.annotations.ProjectConstraints;
import de.samply.annotations.StateConstraints;
import de.samply.aop.ConstraintsService;
import de.samply.aop.ProjectConstraintsAspect;
import de.samply.aop.RoleConstraintsAspect;
import de.samply.aop.StateConstraintsAspect;
import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.db.model.Query;
import de.samply.document.DocumentService;
import de.samply.project.ProjectBridgeheadUserService;
import de.samply.project.ProjectConverter;
import de.samply.project.ProjectService;
import de.samply.project.state.ProjectState;
import de.samply.query.QueryFormat;
import de.samply.user.roles.ProjectRole;
import de.samply.user.roles.UserProjectRoles;
import org.springframework.format.FormatterRegistry;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import de.samply.resolvers.AnnotatedParametersWrapper;
import de.samply.resolvers.LanguageArgumentResolver;
import de.samply.resolvers.RequestBodyCache;
import de.samply.resolvers.RequestVariableAndParameterMethodArgumentResolver;
import de.samply.security.SessionUser;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.OrganisationRoleToProjectRoleMapper;
import de.samply.user.roles.UserOrganisationRoles;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Runs batches against a small controller, with the real argument resolver, request- and session-scoped beans and the
 * real role constraints aspect: what an entry answers must be what its endpoint answers on its own.
 */
@SpringJUnitWebConfig(ActionsBatchServiceTest.TestConfiguration.class)
class ActionsBatchServiceTest {

    private static final String READ = "READ";
    private static final String SLOW_PROJECT_CODE = "SLOW_PROJECT_CODE";
    private static final String ADMIN = "ADMIN";
    private static final String FAIL = "FAIL";
    private static final String THROW = "THROW";
    private static final String DOWNLOAD = "DOWNLOAD";
    private static final String WRITE = "WRITE";
    private static final String TEXT = "TEXT";
    private static final String MISSING = "MISSING";
    private static final String EMPTY = "EMPTY";
    private static final String ONLY_CREATOR = "ONLY_CREATOR";
    private static final String ONLY_IN_REVIEW = "ONLY_IN_REVIEW";
    private static final String ONLY_AST_DATA = "ONLY_AST_DATA";
    private static final String ALL_CONSTRAINTS = "ALL_CONSTRAINTS";
    private static final String PROJECT = "TEST-0001";
    private static final Map<String, Object> PROJECT_PARAMS = Map.of(ProjectManagerConst.PROJECT_CODE, PROJECT);

    @Autowired
    private ActionsBatchService actionsBatchService;
    @Autowired
    private SessionUser sessionUser;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private OrganisationRoleToProjectRoleMapper organisationRoleToProjectRoleMapper;

    private MockMvc mockMvc;
    private MockHttpSession session;

    @BeforeEach
    void bindBatchRequest() {
        Mockito.reset(projectService, organisationRoleToProjectRoleMapper);
        session = new MockHttpSession();
        MockHttpServletRequest batchRequest = new MockHttpServletRequest("POST", ProjectManagerConst.FETCH_ACTIONS_BATCH);
        batchRequest.setSession(session);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(batchRequest));
        authenticateWith(OrganisationRole.RESEARCHER);
        sessionUser.setUserOrganisationRoles(new UserOrganisationRoles());
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @AfterEach
    void unbindBatchRequest() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void answersEveryEntryWithTheResponseOfItsEndpoint() {
        Map<String, ActionsBatchResult> results = fetch(Map.of(
                "first", entry(READ, Map.of("value", "a")),
                "second", entry(READ, Map.of("value", 2))));

        assertThat(results.get("first").response().get("value").asText()).isEqualTo("a");
        assertThat(results.get("second").response().get("value").asText()).isEqualTo("2");
        assertThat(results.get("first").errorCode()).isNull();
    }

    @Test
    void keepsTheOrderOfTheRequests() {
        Map<String, ActionsBatchRequest> requests = new LinkedHashMap<>();
        for (int i = 0; i < 30; i++) {
            requests.put("entry" + i, entry(READ, Map.of("value", i)));
        }

        assertThat(fetch(requests).keySet()).containsExactlyElementsOf(requests.keySet());
    }

    @Test
    void entriesRunInParallelWithTheirOwnRequestScope() {
        Map<String, ActionsBatchRequest> requests = new LinkedHashMap<>();
        for (int i = 0; i < 8; i++) {
            requests.put("entry" + i, entry(SLOW_PROJECT_CODE, Map.of(ProjectManagerConst.PROJECT_CODE, "P" + i)));
        }

        long start = System.nanoTime();
        Map<String, ActionsBatchResult> results = fetch(requests);
        long durationMs = (System.nanoTime() - start) / 1_000_000;

        for (int i = 0; i < 8; i++) {
            assertThat(results.get("entry" + i).response().asText()).isEqualTo("P" + i);
        }
        // 8 entries of 200 ms on 4 threads: about 400 ms, and 1600 ms one after the other
        assertThat(durationMs).isLessThan(1200);
    }

    @Test
    void oneFailingEntryDoesNotAffectTheOthers() {
        Map<String, ActionsBatchResult> results = fetch(Map.of(
                "ok", entry(READ, Map.of("value", "a")),
                "failed", entry(FAIL, Map.of()),
                "thrown", entry(THROW, Map.of())));

        assertThat(results.get("ok").response().get("value").asText()).isEqualTo("a");
        for (String id : List.of("failed", "thrown")) {
            assertThat(results.get(id).response()).isNull();
            assertThat(results.get(id).errorCode()).isEqualTo(500);
            assertThat(results.get(id).errorMessage()).contains("IllegalStateException: boom");
            assertThat(results.get(id).errorStacktrace()).contains("\tat ");
        }
    }

    @Test
    void refusesWhatIsNotAJsonReadAction() {
        ActionsBatchRequest noAction = new ActionsBatchRequest(null, Map.of());
        Map<String, ActionsBatchResult> results = fetch(Map.of(
                "write", entry(WRITE, Map.of()),
                "unknown", entry("NO_SUCH_ACTION", Map.of()),
                "download", entry(DOWNLOAD, Map.of()),
                "noAction", noAction));

        assertThat(results.get("write").errorCode()).isEqualTo(405);
        assertThat(results.get("unknown").errorCode()).isEqualTo(404);
        assertThat(results.get("download").errorCode()).isEqualTo(406);
        assertThat(results.get("noAction").errorCode()).isEqualTo(400);
    }

    @Test
    void passesOnTextAndEmptyAnswers() {
        Map<String, ActionsBatchResult> results = fetch(Map.of(
                "text", entry(TEXT, Map.of()),
                "empty", entry(EMPTY, Map.of())));

        assertThat(results.get("text").response().asText()).isEqualTo("plain text");
        assertThat(results.get("empty").response()).isNull();
        assertThat(results.get("empty").errorCode()).isEqualTo(404);
    }

    @Test
    void refusesAnEntryLikeItsEndpoint() throws Exception {
        // The role constraints aspect of the endpoint refuses the entry: the batch checks nothing itself
        assertSameStatusAsEndpoint(ADMIN, "/admin", 405);

        sessionUser.getUserOrganisationRoles().addRoleNotDependentOnBridgehead(OrganisationRole.PROJECT_MANAGER_ADMIN);
        assertThat(fetch(Map.of("admin", entry(ADMIN, Map.of()))).get("admin").response().asBoolean()).isTrue();
        assertThat(mockMvc.perform(withSession("/admin")).andReturn().getResponse().getStatus()).isEqualTo(200);

        // A missing required parameter
        assertSameStatusAsEndpoint(MISSING, "/read", 400);
    }

    // The constraints of an endpoint must hold for its batch entry. Each test refuses the entry for one reason,
    // compares it with the endpoint called on its own, and then shows that the entry is allowed without that reason.

    @Test
    void refusesAnEntryOfAProjectTheUserHasNoRoleIn() throws Exception {
        givenProject(ProjectState.REVIEW, QueryFormat.AST_DATA);
        when(organisationRoleToProjectRoleMapper.map(any())).thenReturn(Optional.empty());
        assertSameStatusAsEndpoint(ONLY_CREATOR, "/only-creator", PROJECT_PARAMS, 405);

        givenProjectRole(ProjectRole.DEVELOPER);
        assertSameStatusAsEndpoint(ONLY_CREATOR, "/only-creator", PROJECT_PARAMS, 405);

        givenProjectRole(ProjectRole.CREATOR);
        assertSameStatusAsEndpoint(ONLY_CREATOR, "/only-creator", PROJECT_PARAMS, 200);
    }

    @Test
    void refusesAnEntryOfAProjectInAnotherState() throws Exception {
        givenProject(ProjectState.DRAFT, QueryFormat.AST_DATA);
        assertSameStatusAsEndpoint(ONLY_IN_REVIEW, "/only-in-review", PROJECT_PARAMS, 405);

        givenProject(ProjectState.REVIEW, QueryFormat.AST_DATA);
        assertSameStatusAsEndpoint(ONLY_IN_REVIEW, "/only-in-review", PROJECT_PARAMS, 200);
    }

    @Test
    void refusesAnEntryOfAProjectOfAnotherKind() throws Exception {
        givenProject(ProjectState.REVIEW, QueryFormat.CQL);
        assertSameStatusAsEndpoint(ONLY_AST_DATA, "/only-ast-data", PROJECT_PARAMS, 405);

        givenProject(ProjectState.REVIEW, QueryFormat.AST_DATA);
        assertSameStatusAsEndpoint(ONLY_AST_DATA, "/only-ast-data", PROJECT_PARAMS, 200);
    }

    @Test
    void refusesAnEntryOfAProjectThatDoesNotExist() throws Exception {
        when(projectService.fetchProject(PROJECT)).thenReturn(null);

        assertSameStatusAsEndpoint(ONLY_IN_REVIEW, "/only-in-review", PROJECT_PARAMS, 404);
        assertSameStatusAsEndpoint(ONLY_CREATOR, "/only-creator", PROJECT_PARAMS, 404);
    }

    @Test
    void anEntryNeedsEveryConstraintOfItsEndpoint() throws Exception {
        // Everything fulfilled
        sessionUser.getUserOrganisationRoles().addRoleNotDependentOnBridgehead(OrganisationRole.RESEARCHER);
        givenProject(ProjectState.REVIEW, QueryFormat.AST_DATA);
        givenProjectRole(ProjectRole.CREATOR);
        assertSameStatusAsEndpoint(ALL_CONSTRAINTS, "/all-constraints", PROJECT_PARAMS, 200);

        // Each constraint alone is enough to refuse
        givenProject(ProjectState.DRAFT, QueryFormat.AST_DATA);
        assertSameStatusAsEndpoint(ALL_CONSTRAINTS, "/all-constraints", PROJECT_PARAMS, 405);

        givenProject(ProjectState.REVIEW, QueryFormat.CQL);
        assertSameStatusAsEndpoint(ALL_CONSTRAINTS, "/all-constraints", PROJECT_PARAMS, 405);

        givenProject(ProjectState.REVIEW, QueryFormat.AST_DATA);
        givenProjectRole(ProjectRole.DEVELOPER);
        assertSameStatusAsEndpoint(ALL_CONSTRAINTS, "/all-constraints", PROJECT_PARAMS, 405);

        givenProjectRole(ProjectRole.CREATOR);
        sessionUser.setUserOrganisationRoles(new UserOrganisationRoles());
        assertSameStatusAsEndpoint(ALL_CONSTRAINTS, "/all-constraints", PROJECT_PARAMS, 405);
    }

    @Test
    void aRefusedEntryDoesNotRunItsEndpoint() throws Exception {
        // The answer of a refused entry carries nothing of the endpoint's response
        givenProject(ProjectState.DRAFT, QueryFormat.AST_DATA);

        ActionsBatchResult result = fetch(Map.of("entry", entry(ONLY_IN_REVIEW, PROJECT_PARAMS))).get("entry");

        assertThat(result.errorCode()).isEqualTo(405);
        assertThat(result.response()).isNull();
    }

    private void givenProject(ProjectState state, QueryFormat queryFormat) throws Exception {
        Query query = new Query();
        query.setQueryFormat(queryFormat);
        Project project = new Project();
        project.setCode(PROJECT);
        project.setState(state);
        project.setQuery(query);
        when(projectService.fetchProject(PROJECT)).thenReturn(project);
    }

    private void givenProjectRole(ProjectRole role) {
        UserProjectRoles roles = new UserProjectRoles();
        roles.addRoleNotDependentOnBridgehead(role);
        when(organisationRoleToProjectRoleMapper.map(any())).thenReturn(Optional.of(roles));
    }

    @Test
    void readsTheEntriesFromTheRequestBody() throws Exception {
        String body = """
                {"requests": {
                  "first": {"action": "READ", "params": {"value": "a"}},
                  "second": {"action": "EMPTY"},
                  "third": {"params": {"value": "a"}}
                }}""";

        var response = mockMvc.perform(post(ProjectManagerConst.FETCH_ACTIONS_BATCH).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEqualTo("first={\"value\":\"a\"};second=404;third=400;");
    }

    @Test
    void refusesABatchWithoutRequests() throws Exception {
        var response = mockMvc.perform(post(ProjectManagerConst.FETCH_ACTIONS_BATCH).session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(400);
    }

    private void assertSameStatusAsEndpoint(String action, String path, int expectedStatus) throws Exception {
        assertSameStatusAsEndpoint(MISSING.equals(action) ? READ : action, path, Map.of(), expectedStatus);
    }

    // The entry and the endpoint on its own answer with the expected status (an entry without error: 200)
    private void assertSameStatusAsEndpoint(String action, String path, Map<String, Object> params, int expectedStatus)
            throws Exception {
        ActionsBatchResult result = fetch(Map.of("entry", entry(action, params))).get("entry");
        int entryStatus = result.errorCode() != null ? result.errorCode() : 200;
        MockHttpServletRequestBuilder request = withSession(path);
        params.forEach((name, value) -> request.param(name, String.valueOf(value)));
        int endpointStatus = mockMvc.perform(request).andReturn().getResponse().getStatus();

        assertThat(endpointStatus).isEqualTo(expectedStatus);
        assertThat(entryStatus).isEqualTo(endpointStatus);
    }

    private MockHttpServletRequestBuilder withSession(String path) {
        return get(path).session(session);
    }

    private Map<String, ActionsBatchResult> fetch(Map<String, ActionsBatchRequest> requests) {
        return actionsBatchService.fetchActionsBatch(requests);
    }

    private ActionsBatchRequest entry(String action, Map<String, Object> params) {
        return new ActionsBatchRequest(action, params);
    }

    private void authenticateWith(OrganisationRole role) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", "password", role.name()));
    }

    @RestController
    static class TestController {

        private final AnnotatedParametersWrapper annotatedParametersWrapper;
        private final ObjectProvider<ActionsBatchService> actionsBatchService;

        TestController(AnnotatedParametersWrapper annotatedParametersWrapper,
                       ObjectProvider<ActionsBatchService> actionsBatchService) {
            this.annotatedParametersWrapper = annotatedParametersWrapper;
            this.actionsBatchService = actionsBatchService;
        }

        // The batch endpoint with the parameter of the real one; answers "id=response or error code" per entry
        @PostMapping(ProjectManagerConst.FETCH_ACTIONS_BATCH)
        public ResponseEntity<String> fetchActionsBatch(
                @RequestVariable(name = ProjectManagerConst.ACTIONS_BATCH_REQUESTS) Map<String, ActionsBatchRequest> requests
        ) {
            StringBuilder answer = new StringBuilder();
            actionsBatchService.getObject().fetchActionsBatch(requests).forEach((id, result) -> answer
                    .append(id).append('=')
                    .append(result.errorCode() != null ? result.errorCode() : result.response()).append(';'));
            return ResponseEntity.ok(answer.toString());
        }

        @FrontendAction(action = READ)
        @GetMapping("/read")
        public ResponseEntity<String> read(@RequestParameter(name = "value") String value) {
            return ResponseEntity.ok("{\"value\": \"" + value + "\"}");
        }

        // Answers with what the request-scoped wrapper holds after a while, when the other entries have run too
        @FrontendAction(action = SLOW_PROJECT_CODE)
        @GetMapping("/slow-project-code")
        public ResponseEntity<String> slowProjectCode(
                @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) String projectCode
        ) throws InterruptedException {
            Thread.sleep(200);
            return ResponseEntity.ok("\"" + annotatedParametersWrapper.getRaw(ProjectCode.class, String.class).orElse(null) + "\"");
        }

        @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
        @FrontendAction(action = ADMIN)
        @GetMapping("/admin")
        public ResponseEntity<String> admin() {
            return ResponseEntity.ok("true");
        }

        @FrontendAction(action = FAIL)
        @GetMapping("/fail")
        public ResponseEntity<String> fail() {
            return ResponseEntity.internalServerError().body(ExceptionUtils.getStackTrace(new IllegalStateException("boom")));
        }

        @FrontendAction(action = THROW)
        @GetMapping("/throw")
        public ResponseEntity<String> doThrow() {
            throw new IllegalStateException("boom");
        }

        @FrontendAction(action = DOWNLOAD)
        @GetMapping("/download")
        public ResponseEntity<ByteArrayResource> download() {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new ByteArrayResource(new byte[]{1, 2, 3}));
        }

        @FrontendAction(action = WRITE)
        @PostMapping("/write")
        public ResponseEntity<String> write() {
            return ResponseEntity.ok("true");
        }

        @FrontendAction(action = TEXT)
        @GetMapping("/text")
        public ResponseEntity<String> text() {
            return ResponseEntity.ok("plain text");
        }

        @FrontendAction(action = EMPTY)
        @GetMapping("/empty")
        public ResponseEntity<String> empty() {
            return ResponseEntity.notFound().build();
        }

        // One endpoint per kind of constraint, each on a project resolved from its code like in the real controller

        @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
        @FrontendAction(action = ONLY_CREATOR)
        @GetMapping("/only-creator")
        public ResponseEntity<String> onlyCreator(
                @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project) {
            return ResponseEntity.ok("true");
        }

        @StateConstraints(projectStates = {ProjectState.REVIEW})
        @FrontendAction(action = ONLY_IN_REVIEW)
        @GetMapping("/only-in-review")
        public ResponseEntity<String> onlyInReview(
                @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project) {
            return ResponseEntity.ok("true");
        }

        @ProjectConstraints(queryFormats = {QueryFormat.AST_DATA})
        @FrontendAction(action = ONLY_AST_DATA)
        @GetMapping("/only-ast-data")
        public ResponseEntity<String> onlyAstData(
                @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project) {
            return ResponseEntity.ok("true");
        }

        // All three kinds at once, as on many real endpoints
        @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER}, projectRoles = {ProjectRole.CREATOR})
        @StateConstraints(projectStates = {ProjectState.REVIEW})
        @ProjectConstraints(queryFormats = {QueryFormat.AST_DATA})
        @FrontendAction(action = ALL_CONSTRAINTS)
        @GetMapping("/all-constraints")
        public ResponseEntity<String> allConstraints(
                @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project) {
            return ResponseEntity.ok("true");
        }

    }

    @Configuration
    @EnableWebMvc
    @EnableAspectJAutoProxy
    @Import({TestController.class, RequestVariableAndParameterMethodArgumentResolver.class, LanguageArgumentResolver.class,
            RequestBodyCache.class, AnnotatedParametersWrapper.class, ConstraintsService.class,
            RoleConstraintsAspect.class, StateConstraintsAspect.class, ProjectConstraintsAspect.class,
            ProjectConverter.class})
    static class TestConfiguration implements WebMvcConfigurer {

        @Autowired
        private RequestVariableAndParameterMethodArgumentResolver requestVariableResolver;
        @Autowired
        private LanguageArgumentResolver languageArgumentResolver;
        @Autowired
        private ProjectConverter projectConverter;

        // The application registers its converters automatically; here the one that loads a project by its code
        @Override
        public void addFormatters(FormatterRegistry registry) {
            registry.addConverter(projectConverter);
        }

        @Bean
        ProjectService projectService() {
            return Mockito.mock(ProjectService.class);
        }

        @Bean
        DocumentService documentService() {
            return Mockito.mock(DocumentService.class);
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(requestVariableResolver);
            resolvers.add(languageArgumentResolver);
        }

        // Session-scoped as in the application, with the user of a secured application (no roles to start with)
        @Bean
        @Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
        SessionUser sessionUser() {
            return new SessionUser(true);
        }

        @Bean
        ProjectBridgeheadUserService projectBridgeheadUserService() {
            return Mockito.mock(ProjectBridgeheadUserService.class);
        }

        @Bean
        OrganisationRoleToProjectRoleMapper organisationRoleToProjectRoleMapper() {
            return Mockito.mock(OrganisationRoleToProjectRoleMapper.class);
        }

        @Bean(name = ProjectManagerConst.ASYNC_ACTIONS_BATCH_EXECUTOR)
        ThreadPoolTaskExecutor actionsBatchExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(4);
            executor.setMaxPoolSize(4);
            // Small on purpose: the 30 entries of keepsTheOrderOfTheRequests also run on the thread of the batch request
            executor.setQueueCapacity(10);
            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
            return executor;
        }

        @Bean
        ActionsBatchService actionsBatchService(
                @Qualifier("requestMappingHandlerMapping") ObjectProvider<RequestMappingHandlerMapping> handlerMapping,
                @Qualifier("requestMappingHandlerAdapter") ObjectProvider<RequestMappingHandlerAdapter> handlerAdapter,
                @Qualifier(ProjectManagerConst.ASYNC_ACTIONS_BATCH_EXECUTOR) ThreadPoolTaskExecutor executor) {
            return new ActionsBatchService(handlerMapping, handlerAdapter, executor);
        }

    }

}
