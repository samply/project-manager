package de.samply.app.controller;

import de.samply.annotations.*;
import de.samply.app.ProjectManagerConst;
import de.samply.cache.CacheCategory;
import de.samply.cache.CacheResource;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectDocument;
import de.samply.document.DocumentService;
import de.samply.document.DocumentServiceException;
import de.samply.document.DocumentType;
import de.samply.document.DtoDocumentService;
import de.samply.email.EmailRecipientType;
import de.samply.email.EmailTemplateType;
import de.samply.modules.OptionalModule;
import de.samply.project.*;
import de.samply.project.state.ProjectState;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * ProjectDocument of a Project (optionally of one bridgehead): upload, URL, download and existence of
 * the documents per type.
 */
@SuppressWarnings({"rawtypes"}) // See AbstractController
@RestController
public class ProjectDocumentController extends AbstractController {

    private final DocumentService documentService;
    private final DtoDocumentService dtoDocumentService;

    public ProjectDocumentController(DocumentService documentService,
                                     DtoDocumentService dtoDocumentService) {
        this.documentService = documentService;
        this.dtoDocumentService = dtoDocumentService;
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.DEVELOPER, ProjectRole.PILOT,
            ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_OTHER_DOCUMENT_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.UPLOAD_OTHER_DOCUMENT)
    public ResponseEntity uploadOtherDocument(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // bridgehead required for identifying developer, pilot, final user or bridgehead admin in role constraints
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestParameter(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParameter(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                project, Optional.ofNullable(bridgehead), document, DocumentType.OTHERS,
                Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.DEVELOPER, ProjectRole.PILOT,
            ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.CENTRAL_APPROVAL,
            ProjectState.LOCAL_APPROVAL, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @ProjectConstraints(documentCreatorOrProjectManagerAdmin = true)
    @FrontendAction(action = ProjectManagerConst.REMOVE_DOCUMENT_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @DeleteMapping(value = ProjectManagerConst.REMOVE_DOCUMENT)
    public ResponseEntity removeDocument(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestParameter(name = ProjectManagerConst.DOCUMENT_ID) Long documentId
    ) {
        return convertToResponseEntity(() -> this.documentService.removeDocument(project, documentId));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER,
            OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @EmailSender(templateType = EmailTemplateType.NEW_PUBLICATION, recipients = {
            EmailRecipientType.PROJECT_ALL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_PUBLICATION_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.UPLOAD_PUBLICATION)
    public ResponseEntity uploadPublication(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestParameter(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParameter(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                project, Optional.empty(), document, DocumentType.PUBLICATION, Optional.ofNullable(label)));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER,
            OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @EmailSender(templateType = EmailTemplateType.NEW_FINAL_REPORT, recipients = {
            EmailRecipientType.PROJECT_ALL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_FINAL_REPORT_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.UPLOAD_FINAL_REPORT)
    public ResponseEntity uploadFinalReport(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestParameter(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParameter(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                project, Optional.empty(), document, DocumentType.FINAL_REPORT, Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_SCRIPT_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.UPLOAD_SCRIPT)
    public ResponseEntity uploadScript(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // bridgehead required for identifying developer user or bridgehead admin in role constraints
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestParameter(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParameter(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                project, Optional.empty(), document, DocumentType.SCRIPT, Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.CENTRAL_APPROVAL,
            ProjectState.LOCAL_APPROVAL, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @EmailSender(templateType = EmailTemplateType.NEW_VOTUM, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_VOTUM_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.UPLOAD_VOTUM)
    public ResponseEntity uploadVotum(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            @RequestParameter(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParameter(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                project, Optional.ofNullable(bridgehead), document, DocumentType.VOTUM,
                Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @EmailSender(templateType = EmailTemplateType.NEW_VOTUM, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_DESCRIPTION_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.UPLOAD_DESCRIPTION)
    public ResponseEntity uploadDescription(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestParameter(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParameter(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                project, Optional.empty(), document, DocumentType.DESCRIPTION, Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_FORM_FIELD_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.UPLOAD_FORM_FIELD)
    public ResponseEntity uploadFormField(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestParameter(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParameter(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                project, Optional.empty(), document, DocumentType.FORM_FIELD, Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_FORM_FIELD_URL_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.ADD_FORM_FIELD_URL)
    public ResponseEntity addFormFieldUrl(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestVariable(name = ProjectManagerConst.DOCUMENT_URL) String documentUrl,
            @RequestVariable(name = ProjectManagerConst.LABEL, required = false) String label
    ) {
        return convertToResponseEntity(() -> this.documentService.addDocumentUrl(
                project, Optional.empty(), documentUrl, DocumentType.FORM_FIELD, Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.REVIEW, ProjectState.CENTRAL_APPROVAL,
            ProjectState.LOCAL_APPROVAL, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL, ProjectState.DRAFT})
    @EmailSender(templateType = EmailTemplateType.NEW_VOTUM_FOR_ALL_BRIDGEHEADS, recipients = {
            EmailRecipientType.ALL_BRIDGEHEAD_ADMINS, EmailRecipientType.CREATOR,
            EmailRecipientType.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_VOTUM_FOR_ALL_BRIDGEHEADS_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.UPLOAD_VOTUM_FOR_ALL_BRIDGEHEADS)
    public ResponseEntity uploadVotumForAllBridgeheads(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestParameter(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParameter(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                project, Optional.empty(), document, DocumentType.VOTUM, Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @EmailSender(templateType = EmailTemplateType.NEW_PUBLICATION, recipients = {
            EmailRecipientType.PROJECT_ALL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_PUBLICATION_URL_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.ADD_PUBLICATION_URL)
    public ResponseEntity addPublicationUrl(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // bridgehead required for identifying bridgehead admin in role constraints
            @SuppressWarnings("unused") @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestVariable(name = ProjectManagerConst.DOCUMENT_URL) String documentUrl,
            @RequestVariable(name = ProjectManagerConst.LABEL, required = false) String label
    ) {
        return convertToResponseEntity(() -> this.documentService.addDocumentUrl(
                project, Optional.empty(), documentUrl, DocumentType.PUBLICATION,
                Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @EmailSender(templateType = EmailTemplateType.NEW_FINAL_REPORT, recipients = {
            EmailRecipientType.PROJECT_ALL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_FINAL_REPORT_URL_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.ADD_FINAL_REPORT_URL)
    public ResponseEntity addFinalReportUrl(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // bridgehead required for identifying bridgehead admin in role constraints
            @SuppressWarnings("unused") @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestVariable(name = ProjectManagerConst.DOCUMENT_URL) String documentUrl,
            @RequestVariable(name = ProjectManagerConst.LABEL, required = false) String label
    ) {
        return convertToResponseEntity(() -> this.documentService.addDocumentUrl(
                project, Optional.empty(), documentUrl, DocumentType.FINAL_REPORT,
                Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_OTHER_DOCUMENT_URL_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.ADD_OTHER_DOCUMENT_URL)
    public ResponseEntity addOtherDocumentUrl(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestVariable(name = ProjectManagerConst.DOCUMENT_URL) String documentUrl,
            @RequestVariable(name = ProjectManagerConst.LABEL, required = false) String label
    ) {
        return convertToResponseEntity(() -> this.documentService.addDocumentUrl(
                project, Optional.ofNullable(bridgehead), documentUrl, DocumentType.OTHERS,
                Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL,
            ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL,
            ProjectState.FINISHED, ProjectState.ARCHIVED, ProjectState.REJECTED})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_SCRIPT_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_SCRIPT)
    public ResponseEntity downloadScript(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // bridgehead required for identifying developer user or bridgehead admin in role constraints
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) throws DocumentServiceException {
        return downloadProjectDocument(project, Optional.empty(), Optional.empty(), DocumentType.SCRIPT);
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL,
            ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL,
            ProjectState.FINISHED, ProjectState.ARCHIVED, ProjectState.REJECTED})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_SCRIPT_DESCRIPTION_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.FETCH_SCRIPT_DESCRIPTION)
    public ResponseEntity fetchScriptDescription(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // bridgehead required for identifying developer user or bridgehead admin in role constraints
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertOptionalToResponseEntity(
                () -> this.dtoDocumentService.fetchLastDocumentOfThisTypeForFrontend(project,
                        Optional.empty(), DocumentType.SCRIPT));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL,
            ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL,
            ProjectState.FINISHED, ProjectState.ARCHIVED, ProjectState.REJECTED})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_SCRIPT_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.EXISTS_SCRIPT)
    public ResponseEntity existsScript(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // bridgehead required for identifying developer user or bridgehead admin in role constraints
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return existsProjectDocument(project, Optional.empty(), DocumentType.SCRIPT);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.CENTRAL_APPROVAL,
            ProjectState.LOCAL_APPROVAL, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_VOTUM_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_VOTUM)
    public ResponseEntity downloadVotum(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) throws DocumentServiceException {
        return downloadProjectDocument(project, Optional.of(bridgehead), Optional.empty(), DocumentType.VOTUM);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_DESCRIPTION_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_DESCRIPTION)
    public ResponseEntity downloadDescription(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) throws DocumentServiceException {
        return downloadProjectDocument(project, Optional.empty(), Optional.empty(), DocumentType.DESCRIPTION);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_FORM_FIELD_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_FORM_FIELD)
    public ResponseEntity downloadFormField(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) throws DocumentServiceException {
        return downloadProjectDocument(project, Optional.empty(), Optional.empty(), DocumentType.FORM_FIELD);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.CENTRAL_APPROVAL,
            ProjectState.LOCAL_APPROVAL, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_VOTUM_FOR_ALL_BRIDGEHEADS_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_VOTUM_FOR_ALL_BRIDGEHEADS)
    public ResponseEntity downloadVotumForAllBridgeheads(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) throws DocumentServiceException {
        return downloadProjectDocument(project, Optional.empty(), Optional.empty(), DocumentType.VOTUM);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.CENTRAL_APPROVAL,
            ProjectState.LOCAL_APPROVAL, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_VOTUM_DESCRIPTION_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.FETCH_VOTUM_DESCRIPTION)
    public ResponseEntity fetchVotumDescription(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertOptionalToResponseEntity(
                () -> this.dtoDocumentService.fetchLastDocumentOfThisTypeForFrontend(project,
                        Optional.ofNullable(bridgehead), DocumentType.VOTUM));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.CENTRAL_APPROVAL,
            ProjectState.LOCAL_APPROVAL, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_DESCRIPTION_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.FETCH_DESCRIPTION)
    public ResponseEntity fetchProjectDescription(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertOptionalToResponseEntity(
                () -> this.dtoDocumentService.fetchLastDocumentOfThisTypeForFrontend(project,
                        Optional.empty(), DocumentType.DESCRIPTION));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_FORM_FIELD_FILE_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.FETCH_FORM_FIELD_FILE)
    public ResponseEntity fetchFormFieldFile(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertOptionalToResponseEntity(
                () -> this.dtoDocumentService.fetchLastDocumentOfThisTypeForFrontend(project,
                        Optional.empty(), DocumentType.FORM_FIELD));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.CENTRAL_APPROVAL,
            ProjectState.LOCAL_APPROVAL, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_VOTUM_FOR_ALL_BRIDGEHEADS_DESCRIPTION_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.FETCH_VOTUM_FOR_ALL_BRIDGEHEADS_DESCRIPTION)
    public ResponseEntity fetchVotumDescriptionForAllBridgeheads(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertOptionalToResponseEntity(
                () -> this.dtoDocumentService.fetchLastDocumentOfThisTypeForFrontend(project,
                        Optional.empty(), DocumentType.VOTUM));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.CENTRAL_APPROVAL,
            ProjectState.LOCAL_APPROVAL, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED,
            ProjectState.ARCHIVED, ProjectState.REJECTED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_VOTUM_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.EXISTS_VOTUM)
    public ResponseEntity existsVotum(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return existsProjectDocument(project, Optional.of(bridgehead), DocumentType.VOTUM);
    }

    // Local approval: one document per bridgehead (no "all bridgeheads"), uploaded in LOCAL_APPROVAL or FINAL
    @RequiresModule(OptionalModule.LOCAL_APPROVAL)
    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.LOCAL_APPROVAL, ProjectState.FINAL})
    // Two texts: in LOCAL_APPROVAL the site's approval is there; in FINAL the site can export now
    @EmailSender(templateType = EmailTemplateType.NEW_LOCAL_APPROVAL, projectStates = {ProjectState.LOCAL_APPROVAL},
            recipients = {EmailRecipientType.CREATOR, EmailRecipientType.BRIDGEHEAD_ADMIN,
                    EmailRecipientType.PROJECT_MANAGER_ADMIN})
    @EmailSender(templateType = EmailTemplateType.NEW_LOCAL_APPROVAL_FINAL, projectStates = {ProjectState.FINAL},
            recipients = {EmailRecipientType.CREATOR, EmailRecipientType.BRIDGEHEAD_ADMIN,
                    EmailRecipientType.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_LOCAL_APPROVAL_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.UPLOAD_LOCAL_APPROVAL)
    public ResponseEntity uploadLocalApproval(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            @RequestParameter(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParameter(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                project, Optional.ofNullable(bridgehead), document, DocumentType.LOCAL_APPROVAL,
                Optional.ofNullable(label)));
    }

    @RequiresModule(OptionalModule.LOCAL_APPROVAL)
    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.LOCAL_APPROVAL, ProjectState.DEVELOP,
            ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED, ProjectState.ARCHIVED, ProjectState.REJECTED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_LOCAL_APPROVAL_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_LOCAL_APPROVAL)
    public ResponseEntity downloadLocalApproval(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) throws DocumentServiceException {
        return downloadProjectDocument(project, Optional.of(bridgehead), Optional.empty(), DocumentType.LOCAL_APPROVAL);
    }

    @RequiresModule(OptionalModule.LOCAL_APPROVAL)
    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.LOCAL_APPROVAL, ProjectState.DEVELOP,
            ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED, ProjectState.ARCHIVED, ProjectState.REJECTED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_LOCAL_APPROVAL_DESCRIPTION_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.FETCH_LOCAL_APPROVAL_DESCRIPTION)
    public ResponseEntity fetchLocalApprovalDescription(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertOptionalToResponseEntity(
                () -> this.dtoDocumentService.fetchLastDocumentOfThisTypeForFrontend(project,
                        Optional.ofNullable(bridgehead), DocumentType.LOCAL_APPROVAL));
    }

    @RequiresModule(OptionalModule.LOCAL_APPROVAL)
    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.LOCAL_APPROVAL, ProjectState.DEVELOP,
            ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED, ProjectState.ARCHIVED, ProjectState.REJECTED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_LOCAL_APPROVAL_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.EXISTS_LOCAL_APPROVAL)
    public ResponseEntity existsLocalApproval(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return existsProjectDocument(project, Optional.of(bridgehead), DocumentType.LOCAL_APPROVAL);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINISHED, ProjectState.ARCHIVED,
            ProjectState.REJECTED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_PUBLICATION_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.EXISTS_PUBLICATION)
    public ResponseEntity existsPublication(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return existsProjectDocument(project, Optional.empty(), DocumentType.PUBLICATION);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINISHED, ProjectState.ARCHIVED,
            ProjectState.REJECTED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_FINAL_REPORT_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.EXISTS_FINAL_REPORT)
    public ResponseEntity existsFinalReport(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return existsProjectDocument(project, Optional.empty(), DocumentType.FINAL_REPORT);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_DESCRIPTION_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.EXISTS_DESCRIPTION)
    public ResponseEntity existsDescription(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return existsProjectDocument(project, Optional.empty(), DocumentType.DESCRIPTION);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_FORM_FIELD_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.EXISTS_FORM_FIELD)
    public ResponseEntity existsFormField(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return existsProjectDocument(project, Optional.empty(), DocumentType.FORM_FIELD);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.CENTRAL_APPROVAL,
            ProjectState.LOCAL_APPROVAL, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED,
            ProjectState.ARCHIVED, ProjectState.REJECTED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_VOTUM_FOR_ALL_BRIDGEHEADS_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.EXISTS_VOTUM_FOR_ALL_BRIDGEHEADS)
    public ResponseEntity existsVotumForAllBridgeheads(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return existsProjectDocument(project, Optional.empty(), DocumentType.VOTUM);
    }

    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_PUBLICATION_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_PUBLICATION)
    public ResponseEntity downloadPublication(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestParameter(name = ProjectManagerConst.FILENAME, required = false) String filename
    ) throws DocumentServiceException {
        return downloadProjectDocument(project, Optional.empty(), Optional.ofNullable(filename), DocumentType.PUBLICATION);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_FINAL_REPORT_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_FINAL_REPORT)
    public ResponseEntity downloadFinalReport(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestParameter(name = ProjectManagerConst.FILENAME, required = false) String filename
    ) throws DocumentServiceException {
        return downloadProjectDocument(project, Optional.empty(), Optional.ofNullable(filename), DocumentType.FINAL_REPORT);
    }

    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PUBLICATIONS_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.FETCH_PUBLICATIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchPublications(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> dtoDocumentService.fetchPublications(project));
    }

    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_FINAL_REPORTS_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.FETCH_FINAL_REPORTS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchFinalReports(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> dtoDocumentService.fetchFinalReports(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.DEVELOPER, ProjectRole.PILOT,
            ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_OTHER_DOCUMENT_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_OTHER_DOCUMENT)
    public ResponseEntity downloadOtherProjectDocument(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestParameter(name = ProjectManagerConst.FILENAME) String filename
    ) throws DocumentServiceException {
        return downloadProjectDocument(project, Optional.ofNullable(bridgehead), Optional.of(filename), DocumentType.OTHERS);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.DEVELOPER, ProjectRole.PILOT,
            ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_OTHER_DOCUMENTS_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.FETCH_OTHER_DOCUMENTS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchOtherDocuments(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> dtoDocumentService.fetchOtherDocuments(project, Optional.ofNullable(bridgehead)));
    }

    private ResponseEntity downloadProjectDocument(Project project,
                                                   Optional<ProjectBridgehead> bridgehead,
                                                   Optional<String> filename,
                                                   DocumentType documentType) throws DocumentServiceException {
        Optional<ProjectDocument> projectDocument = filename.isPresent()
                ? this.documentService.fetchProjectDocument(project, bridgehead, filename.get())
                : this.documentService.fetchLastDocumentOfThisType(project, bridgehead, documentType);

        if (filename.isPresent() && projectDocument.isPresent()
                && projectDocument.get().getDocumentType() != documentType) {
            return createMethodNotAllowedResponse("Requested document is not of allowed type: " + documentType);
        }

        return downloadProjectDocument(projectDocument);
    }

    private ResponseEntity existsProjectDocument(Project project,
                                                 Optional<ProjectBridgehead> bridgehead, DocumentType documentType) {
        return convertToResponseEntity(
                () -> this.documentService.fetchLastDocumentOfThisType(project, bridgehead, documentType)
                        .isPresent());
    }

    private ResponseEntity downloadProjectDocument(Optional<ProjectDocument> projectDocument)
            throws DocumentServiceException {
        return (projectDocument.isEmpty()) ? ResponseEntity.notFound().build() :
                downloadDocument(encodeFilename(projectDocument.get().getOriginalFilename()),
                        Path.of(projectDocument.get().getFilePath()));
    }

    private ResponseEntity createMethodNotAllowedResponse(String errorMessage) {
        ByteArrayResource errorResource = new ByteArrayResource(errorMessage.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers)
                .body(errorResource);
    }

    private ResponseEntity downloadDocument(String filename, Path filePath)
            throws DocumentServiceException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(filePath.toFile().length())
                .body(fetchResource(filePath));
    }

    private String encodeFilename(String filename) {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8);
    }

    private ByteArrayResource fetchResource(Path filePath) throws DocumentServiceException {
        try {
            return new ByteArrayResource(Files.readAllBytes(filePath));
        } catch (IOException e) {
            throw new DocumentServiceException(e);
        }
    }

}
