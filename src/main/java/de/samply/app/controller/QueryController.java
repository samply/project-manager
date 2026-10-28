package de.samply.app.controller;

import de.samply.annotations.*;
import de.samply.app.ProjectManagerConst;
import de.samply.cache.CacheCategory;
import de.samply.cache.CacheResource;
import de.samply.db.model.Project;
import de.samply.exporter.ExporterService;
import de.samply.project.*;
import de.samply.project.state.ProjectState;
import de.samply.query.OutputFormat;
import de.samply.query.QueryFormat;
import de.samply.query.QueryService;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Query and its QueryOutputs: create the query, remove an output and the available query and output
 * formats and exporter templates.
 */
@SuppressWarnings({"rawtypes"}) // See AbstractController
@RestController
public class QueryController extends AbstractController {

    private final QueryService queryService;
    private final ExporterService exporterService;
    private final ProjectService projectService;

    public QueryController(QueryService queryService,
                           ExporterService exporterService,
                           ProjectService projectService) {
        this.queryService = queryService;
        this.exporterService = exporterService;
        this.projectService = projectService;
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.CREATE_QUERY)
    public ResponseEntity createProjectQuery(
            @RequestVariable(name = ProjectManagerConst.QUERY, notEmpty = true) String query,
            @RequestVariable(name = ProjectManagerConst.QUERY_FORMAT, notEmpty = true) QueryFormat queryFormat,
            @RequestVariable(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestVariable(name = ProjectManagerConst.DESCRIPTION, required = false) String description,
            @RequestVariable(name = ProjectManagerConst.OUTPUT_FORMAT, required = false) OutputFormat outputFormat,
            @RequestVariable(name = ProjectManagerConst.TEMPLATE_ID, required = false) String templateId,
            @RequestVariable(name = ProjectManagerConst.HUMAN_READABLE, required = false) String humanReadable,
            @RequestVariable(name = ProjectManagerConst.REDIRECT_EXPLORER_URL, required = false) String explorerUrl,
            @RequestVariable(name = ProjectManagerConst.QUERY_CONTEXT, required = false) String queryContext,
            @RequestVariable(name = ProjectManagerConst.QUERY_DETAILS, required = false) String queryDetails,
            @RequestVariable(name = ProjectManagerConst.PROJECT_TYPE, required = false) ProjectType projectType
    ) {
        return convertToResponseEntity(() ->
                this.queryService.createQuery(query, queryFormat, label, description,
                        outputFormat, templateId, projectType, humanReadable, explorerUrl, queryContext,
                        queryDetails));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.REMOVE_PROJECT_OUTPUT_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @DeleteMapping(value = ProjectManagerConst.REMOVE_PROJECT_OUTPUT)
    public ResponseEntity removeOutput(
            @RequestVariable(name = ProjectManagerConst.PROJECT_TYPE) ProjectType projectType,
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> this.queryService.removeOutput(project, projectType));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_QUERY_FORMATS_ACTION)
    @CacheCategory(CacheResource.REFERENCE_DATA)
    @GetMapping(value = ProjectManagerConst.FETCH_QUERY_FORMATS)
    public ResponseEntity fetchQueryFormats(
            // ProjectCode code needed for role constraints
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(QueryFormat::values);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_OUTPUT_FORMATS_ACTION)
    @CacheCategory(CacheResource.REFERENCE_DATA)
    @GetMapping(value = ProjectManagerConst.FETCH_OUTPUT_FORMATS)
    public ResponseEntity fetchOutputFormats(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> projectService.fetchOutputFormats(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_EXPORTER_TEMPLATES_ACTION)
    @CacheCategory(CacheResource.REFERENCE_DATA)
    @GetMapping(value = ProjectManagerConst.FETCH_EXPORTER_TEMPLATES)
    public ResponseEntity fetchExporterTemplates(
            // ProjectCode code needed for role constraints
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(exporterService::getExporterTemplates);
    }

}
