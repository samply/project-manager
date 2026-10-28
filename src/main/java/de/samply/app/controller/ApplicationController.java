package de.samply.app.controller;

import de.samply.annotations.*;
import de.samply.app.ProjectManagerConst;
import de.samply.batch.ActionsBatchRequest;
import de.samply.batch.ActionsBatchService;
import de.samply.cache.CacheCategory;
import de.samply.cache.CacheConfiguration;
import de.samply.cache.CacheResource;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.display.DisplayFormatService;
import de.samply.display.DisplayFormatsResponse;
import de.samply.email.EmailService;
import de.samply.email.EmailTemplateType;
import de.samply.frontend.FrontendConfiguration;
import de.samply.frontend.FrontendService;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;
import de.samply.utils.ProjectVersion;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Endpoints without an entity of their own: application info, frontend actions and variables, display
 * formats and the preview of e-mails.
 */
@SuppressWarnings({"rawtypes"}) // See AbstractController
@RestController
public class        ApplicationController extends AbstractController {

    private final String projectVersion = ProjectVersion.getProjectVersion();
    private final FrontendService frontendService;
    private final FrontendConfiguration frontendConfiguration;
    private final CacheConfiguration cacheConfiguration;
    private final DisplayFormatService displayFormatService;
    private final ActionsBatchService actionsBatchService;
    private final EmailService emailService;

    public ApplicationController(FrontendService frontendService,
                                 FrontendConfiguration frontendConfiguration,
                                 CacheConfiguration cacheConfiguration,
                                 DisplayFormatService displayFormatService,
                                 ActionsBatchService actionsBatchService,
                                 EmailService emailService) {
        this.frontendService = frontendService;
        this.frontendConfiguration = frontendConfiguration;
        this.cacheConfiguration = cacheConfiguration;
        this.displayFormatService = displayFormatService;
        this.actionsBatchService = actionsBatchService;
        this.emailService = emailService;
    }

    @CacheCategory(CacheResource.PUBLIC_INFORMATION)
    @GetMapping(value = ProjectManagerConst.INFO)
    public ResponseEntity info() {
        return new ResponseEntity<>(projectVersion, HttpStatus.OK);
    }

    @CacheCategory(CacheResource.AUTHENTICATED_GET)
    @GetMapping(value = ProjectManagerConst.TEST)
    public ResponseEntity test() {
        return new ResponseEntity<>(ProjectManagerConst.THIS_IS_A_TEST, HttpStatus.OK);
    }

    @CacheCategory(CacheResource.ACTION_AVAILABILITY)
    @GetMapping(value = ProjectManagerConst.ACTIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchActions(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE, required = false) Project project,
            // The actions depend on the bridgehead (the bridgehead roles count only for their bridgehead): the answer
            // has the actions without a bridgehead under "" and those of each given bridgehead under its id, so that
            // a page asks once (comma-separated ids)
            @Bridgeheads @RequestParameter(name = ProjectManagerConst.BRIDGEHEADS, required = false) List<ProjectBridgehead> bridgeheads,
            @Language String language,
            @RequestParameter(name = ProjectManagerConst.SITE) String site
    ) {
        return convertToResponseEntity(() -> this.frontendService.fetchModuleActionPackagesByBridgehead(site,
                Optional.ofNullable(project), Optional.ofNullable(bridgeheads).orElse(List.of()),
                Optional.ofNullable(language)));
    }

    @CacheCategory(CacheResource.ACTION_AVAILABILITY)
    @GetMapping(value = ProjectManagerConst.ALL_ACTIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchAllActions(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE, required = false) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @Language String language,
            @RequestParameter(name = ProjectManagerConst.SITE) String site
    ) {
        return convertToResponseEntity(() ->
                this.frontendService.fetchModuleActionPackage(site, Optional.ofNullable(project),
                        Optional.ofNullable(bridgehead), Optional.ofNullable(language), false));
    }

    // Several read actions (GET endpoints) in one request: {"requests": {id: {"action": ..., "params": {...}}}}.
    // POST only because browsers send nobody with GET; it changes nothing. Every entry is answered on its own,
    // with the response or the error of its endpoint, so the batch itself is answered with 200 - unless it has more
    // entries than allowed: then it is refused as a whole (docs/actions-batch-security.md).
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.ACTIONS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_DASHBOARD_SITE, module = ProjectManagerConst.ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_ACTIONS_BATCH_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.FETCH_ACTIONS_BATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchActionsBatch(
            @RequestVariable(name = ProjectManagerConst.ACTIONS_BATCH_REQUESTS) Map<String, ActionsBatchRequest> requests
    ) {
        if (requests != null && requests.size() > actionsBatchService.getMaxEntries()) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body("At most " + actionsBatchService.getMaxEntries() + " entries per batch");
        }
        return convertToResponseEntity(() ->
                Map.of(ProjectManagerConst.ACTIONS_BATCH_RESULTS, actionsBatchService.fetchActionsBatch(requests)));
    }

    @FrontendAction(action = ProjectManagerConst.FETCH_FRONTEND_VARIABLES_ACTION)
    @CacheCategory(CacheResource.FRONTEND_VARIABLES)
    @GetMapping(value = ProjectManagerConst.FETCH_FRONTEND_VARIABLES, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchFrontendVariables() {
        return convertToResponseEntity(() -> ResponseEntity.ok()
                .cacheControl(cacheConfiguration.cacheControl(CacheResource.FRONTEND_VARIABLES))
                .body(this.frontendConfiguration.getVariables()));
    }

    @FrontendAction(action = ProjectManagerConst.FETCH_DISPLAY_FORMATS_ACTION)
    @CacheCategory(CacheResource.DISPLAY_FORMATS)
    @GetMapping(value = ProjectManagerConst.FETCH_DISPLAY_FORMATS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DisplayFormatsResponse> fetchDisplayFormats(@Language String language) {
        return ResponseEntity.ok()
                .cacheControl(cacheConfiguration.cacheControl(CacheResource.DISPLAY_FORMATS))
                .body(DisplayFormatsResponse.from(displayFormatService, language));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER,
            OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_RESULTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_EMAIL_MESSAGE_AND_SUBJECT_ACTION)
    @CacheCategory(CacheResource.PROJECT_DETAIL)
    @GetMapping(value = ProjectManagerConst.FETCH_EMAIL_MESSAGE_AND_SUBJECT)
    public ResponseEntity fetchEmailMessageAndSubject(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE, required = false) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @Email @RequestParameter(name = ProjectManagerConst.EMAIL) String email,
            @RequestParameter(name = ProjectManagerConst.PROJECT_ROLE) ProjectRole projectRole,
            @RequestParameter(name = ProjectManagerConst.EMAIL_TEMPLATE_TYPE) EmailTemplateType emailTemplateType
    ) {
        return convertOptionalToResponseEntity(
                () -> emailService.createEmailMessageAndSubject(email, Optional.ofNullable(project),
                        Optional.ofNullable(bridgehead), projectRole, emailTemplateType));
    }

}
