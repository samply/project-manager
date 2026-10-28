package de.samply.app.controller;

import de.samply.annotations.*;
import de.samply.app.ProjectManagerConst;
import de.samply.cache.CacheCategory;
import de.samply.cache.CacheResource;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.form.core.DtoFormService;
import de.samply.form.core.FormService;
import de.samply.form.template.FormTemplateService;
import de.samply.frontend.dto.FormField;
import de.samply.frontend.dto.configuration.ProjectConfigurations;
import de.samply.project.state.ProjectState;
import de.samply.user.roles.ProjectRole;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * ProjectForm and ProjectFormField of a Project (definitions versioned in FormHistory): titles, fields,
 * layouts, selection, values and the PDF of a form.
 */
@SuppressWarnings({"rawtypes"}) // See AbstractController
@RestController
public class ProjectFormController extends AbstractController {

    private final ProjectConfigurations frontendProjectConfigurations;
    private final FormService formService;
    private final DtoFormService dtoFormService;
    private final FormTemplateService formTemplateService;

    public ProjectFormController(ProjectConfigurations frontendProjectConfigurations,
                                 FormService formService,
                                 DtoFormService dtoFormService,
                                 FormTemplateService formTemplateService) {
        this.frontendProjectConfigurations = frontendProjectConfigurations;
        this.formService = formService;
        this.dtoFormService = dtoFormService;
        this.formTemplateService = formTemplateService;
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN,
            ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.DEVELOPER, ProjectRole.FINAL, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.DEVELOP,
            ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_FORM_TITLES_ACTION)
    @CacheCategory(CacheResource.FORM_METADATA)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_FORM_TITLES)
    public ResponseEntity fetchProjectFormTitles(
            // ProjectCode code needed for role constraints
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @Language String language
    ) {
        return convertToResponseEntity(
                () -> dtoFormService.fetchProjectFormTitles(project, Optional.ofNullable(language)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN,
            ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.DEVELOPER, ProjectRole.FINAL, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.DEVELOP,
            ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_FORM_TITLE_ORDER_ACTION)
    @CacheCategory(CacheResource.FORM_METADATA)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_FORM_TITLE_ORDER)
    public ResponseEntity fetchProjectFormTitleCanonicalOrder(
            // ProjectCode code needed for role constraints
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @Language String language
    ) {
        return convertToResponseEntity(() -> dtoFormService.fetchProjectFormTitleCanonicalOrder(
                frontendProjectConfigurations.getFormTitleOrder(), project, Optional.ofNullable(language)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN,
            ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.DEVELOPER, ProjectRole.FINAL, ProjectRole.PILOT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_FORM_FIELDS_ACTION)
    @CacheCategory(CacheResource.FORM_METADATA)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_FORM_FIELDS)
    public ResponseEntity fetchProjectFormFields(
            // ProjectCode code needed for role constraints
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @Language String language,
            @RequestParameter(name = ProjectManagerConst.FORM_TITLE, required = false) String formTitle
    ) {
        return convertToResponseEntity(
                () -> dtoFormService.fetchProjectFormFields(Optional.ofNullable(formTitle), project,
                        Optional.ofNullable(language)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN,
            ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.DEVELOPER, ProjectRole.FINAL, ProjectRole.PILOT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_FORM_LAYOUTS_ACTION)
    @CacheCategory(CacheResource.FORM_METADATA)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_FORM_LAYOUTS)
    public ResponseEntity fetchProjectFormLayouts(
            // ProjectCode code needed for role constraints
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestParameter(name = ProjectManagerConst.FORM_TITLE, required = false) String formTitle
    ) {
        return convertToResponseEntity(
                () -> dtoFormService.fetchFormLayouts(Optional.ofNullable(formTitle)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN,
            ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.DEVELOP,
            ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_FORM_TEMPLATES_ACTION)
    @CacheCategory(CacheResource.FORM_METADATA)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_FORM_TEMPLATES)
    public ResponseEntity fetchProjectFormTemplates(
            // ProjectCode code and bridgehead needed for role constraints
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @Language String language
    ) {
        return convertToResponseEntity(
                () -> formTemplateService.fetchTemplates(project, Optional.ofNullable(language)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN,
            ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.DEVELOPER, ProjectRole.FINAL, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.DEVELOP,
            ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_SELECTED_PROJECT_FORMS_ACTION)
    @CacheCategory(CacheResource.PROJECT_DETAIL)
    @GetMapping(value = ProjectManagerConst.FETCH_SELECTED_PROJECT_FORMS)
    public ResponseEntity fetchSelectedProjectForms(
            // ProjectCode code needed for role constraints
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @Language String language
    ) {
        return convertToResponseEntity(
                () -> dtoFormService.fetchSelectedForms(project, Optional.ofNullable(language)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_SELECTED_PROJECT_FORM_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.ADD_SELECTED_PROJECT_FORM)
    public ResponseEntity addSelectedProjectForm(
            // ProjectCode code needed for role constraints
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestVariable(name = ProjectManagerConst.FORM_TITLE) String formTitle
    ) {
        return convertToResponseEntity(() -> formService.addSelectedForm(project, formTitle));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.REMOVE_SELECTED_PROJECT_FORM_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @DeleteMapping(value = ProjectManagerConst.REMOVE_SELECTED_PROJECT_FORM)
    public ResponseEntity removeSelectedProjectForm(
            // ProjectCode code needed for role constraints
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestParameter(name = ProjectManagerConst.FORM_TITLE) String formTitle
    ) {
        return convertToResponseEntity(() -> formService.removeSelectedForm(project, formTitle));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.EDIT_PROJECT_FORM_FIELDS_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.EDIT_PROJECT_FORM_FIELDS)
    public ResponseEntity editProjectFormValues(
            // ProjectCode code needed for role constraints
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestVariable(name = ProjectManagerConst.FORM_FIELDS) FormField[] formFields
    ) {
        return convertToResponseEntity(
                () -> formService.editProjectFormFieldValues(Optional.ofNullable(formFields), project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.DELETE_FORM_FIELD_BLOCK_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @DeleteMapping(value = ProjectManagerConst.DELETE_FORM_FIELD_BLOCK)
    public ResponseEntity removeProjectFormFieldBlock(
            // ProjectCode code needed for role constraints
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestVariable(name = ProjectManagerConst.FORM_FIELD) FormField formField
    ) {
        return convertToResponseEntity(
                () -> formService.removeProjectFormFieldBlock(formField, project));
    }

    // Removes ONE value instance of a multiple field (formField.fieldInstance),
    // e.g., clicking "-" next to one of several values. This is not how a
    // field's value is cleared in general - for a non-multiple field, or for
    // a specific value of a multiple field that should just become empty
    // rather than disappear, save an empty value through EDIT_PROJECT_FORM_FIELDS
    // instead. formService.removeProjectFormFieldValue requires fieldInstance
    // to be non-null precisely to guard against this action being used outside
    // its intended "multiple field" context.
    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.DELETE_FORM_FIELD_VALUE_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @DeleteMapping(value = ProjectManagerConst.DELETE_FORM_FIELD_VALUE)
    public ResponseEntity removeProjectFormFieldValue(
            // ProjectCode code needed for role constraints
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestVariable(name = ProjectManagerConst.FORM_FIELD) FormField formField
    ) {
        return convertToResponseEntity(
                () -> formService.removeProjectFormFieldValue(formField, project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN,
            ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.DEVELOP,
            ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_FORM_AS_PDF_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_FORM_AS_PDF)
    public ResponseEntity downloadFormAsPdf(
            // ProjectCode code and bridgehead needed for role constraints
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @Language String language,
            @RequestParameter(name = ProjectManagerConst.FORM_TEMPLATE) String formTemplate
    ) {
        return convertToResponseEntity(() ->
                // Unknown, or its condition does not hold for this project.
                !formTemplateService.isTemplateAvailable(project, formTemplate, Optional.ofNullable(language))
                        ? ResponseEntity.notFound().build()
                        : ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" +
                                formTemplateService.fetchFormFilename(project, formTemplate))
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(formTemplateService.createFormAsPdf(project, formTemplate,
                                Optional.ofNullable(language)))
        );
    }

}
