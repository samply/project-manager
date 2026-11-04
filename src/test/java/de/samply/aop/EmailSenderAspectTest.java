package de.samply.aop;

import de.samply.annotations.Bridgehead;
import de.samply.annotations.EmailSender;
import de.samply.annotations.ProjectCode;
import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.db.model.CentralApprovalCommitteeUser;
import de.samply.db.model.ProjectManagerAdminUser;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.email.EmailKeyValues;
import de.samply.email.EmailKeyValuesFactory;
import de.samply.email.EmailMetaData;
import de.samply.email.EmailRecipient;
import de.samply.email.EmailRecipientType;
import de.samply.email.EmailSendingService;
import de.samply.email.EmailTemplateType;
import de.samply.email.EmailTemplates;
import de.samply.project.ProjectBridgeheadService;
import de.samply.project.ProjectBridgeheadUserService;
import de.samply.project.state.ProjectState;
import de.samply.security.SessionUser;
import de.samply.user.UserService;
import de.samply.user.roles.OrganisationRoleToProjectRoleMapper;
import de.samply.user.roles.ProjectRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailSenderAspectTest {

    private final EmailSendingService emailSendingService = mock(EmailSendingService.class);
    private final EmailKeyValues keyValues = mock(EmailKeyValues.class, RETURNS_SELF);
    private final BridgeheadsConfiguration bridgeheadsConfiguration = new BridgeheadsConfiguration();
    private final UserService userService = mock(UserService.class);
    private final EmailTemplates emailTemplates = new EmailTemplates();
    private Endpoints endpoints;
    private Project project;
    private ProjectBridgehead jurassicPark;

    @BeforeEach
    void setUp() {
        BridgeheadsConfiguration.BridgeheadConfig config = new BridgeheadsConfiguration.BridgeheadConfig();
        config.setApprovalContacts(List.of(contact(Map.of("en", "John Hammond", "de", "Johann Hammond"),
                "john.hammond@example.org"), contact(Map.of(), " ")));
        bridgeheadsConfiguration.setConfig(Map.of("jurassic-park", config));

        EmailKeyValuesFactory keyValuesFactory = mock(EmailKeyValuesFactory.class);
        when(keyValuesFactory.newInstance()).thenReturn(keyValues);
        EmailSenderAspect aspect = new EmailSenderAspect(emailSendingService, mock(SessionUser.class),
                userService, mock(ProjectBridgeheadService.class), mock(ProjectBridgeheadUserService.class),
                mock(OrganisationRoleToProjectRoleMapper.class), keyValuesFactory, bridgeheadsConfiguration, emailTemplates, "en");
        AspectJProxyFactory factory = new AspectJProxyFactory(new Endpoints());
        factory.setProxyTargetClass(true);
        factory.addAspect(aspect);
        endpoints = factory.getProxy();

        project = new Project();
        project.setCode("REQ-1");
        jurassicPark = new ProjectBridgehead();
        jurassicPark.setBridgehead("jurassic-park");
        jurassicPark.setProject(project);
    }

    // Approval contacts are not users: the address and the name (default language) come from the configuration;
    // a contact without address is skipped
    @Test
    void sendsToTheApprovalContactsOfTheBridgehead() throws Exception {
        project.setState(ProjectState.LOCAL_APPROVAL);

        endpoints.upload(project, jurassicPark);

        verify(emailSendingService).sendEmail(eq("john.hammond@example.org"), eq(Optional.of(project)),
                eq(Optional.of(jurassicPark)), eq(ProjectRole.BRIDGEHEAD_APPROVAL_CONTACT),
                eq(EmailTemplateType.NEW_LOCAL_APPROVAL), any());
        ArgumentCaptor<EmailRecipient> recipient = ArgumentCaptor.forClass(EmailRecipient.class);
        verify(keyValues).add(recipient.capture());
        assertThat(recipient.getValue().getName()).contains("John Hammond");
    }

    // @EmailSender(projectStates): only the template of the project's state is sent
    @Test
    void sendsOnlyTheEmailOfTheProjectState() throws Exception {
        project.setState(ProjectState.FINAL);

        endpoints.upload(project, jurassicPark);

        verify(emailSendingService).sendEmail(any(), any(), any(), any(), eq(EmailTemplateType.NEW_LOCAL_APPROVAL_FINAL), any());
        verify(emailSendingService, never()).sendEmail(any(), any(), any(), any(), eq(EmailTemplateType.NEW_LOCAL_APPROVAL), any());
    }

    // The committee members' addresses come from their table (filled at login), with the committee's template
    @Test
    void sendsToTheCentralApprovalCommittee() throws Exception {
        when(userService.fetchAllCentralApprovalCommitteeUsers()).thenReturn(List.of(
                new CentralApprovalCommitteeUser(1L, "committee@example.org")));

        endpoints.accept(project);

        verify(emailSendingService).sendEmail(eq("committee@example.org"), eq(Optional.of(project)), eq(Optional.empty()),
                eq(ProjectRole.CENTRAL_APPROVAL_COMMITTEE), eq(EmailTemplateType.NEW_PROJECT_ACCEPTED), any());
    }

    // Someone who is project manager admin and committee member gets the same text only once
    @Test
    void sendsTheSameTemplateToAnAddressOnlyOnce() throws Exception {
        templates(Map.of(ProjectRole.DEFAULT, "changes-requested.html"));
        adminAndCommitteeMember("both@example.org");

        endpoints.requestChanges(project);

        verify(emailSendingService, times(1)).sendEmail(eq("both@example.org"), any(), any(), any(),
                eq(EmailTemplateType.PROJECT_BACK_TO_REVIEW), any());
    }

    // Different templates are different texts: each role gets its own
    @Test
    void sendsDifferentTemplatesToTheSameAddress() throws Exception {
        templates(Map.of(ProjectRole.PROJECT_MANAGER_ADMIN, "changes-requested-admin.html",
                ProjectRole.CENTRAL_APPROVAL_COMMITTEE, "changes-requested-committee.html"));
        adminAndCommitteeMember("both@example.org");

        endpoints.requestChanges(project);

        verify(emailSendingService).sendEmail(eq("both@example.org"), any(), any(), eq(ProjectRole.PROJECT_MANAGER_ADMIN),
                eq(EmailTemplateType.PROJECT_BACK_TO_REVIEW), any());
        verify(emailSendingService).sendEmail(eq("both@example.org"), any(), any(),
                eq(ProjectRole.CENTRAL_APPROVAL_COMMITTEE), eq(EmailTemplateType.PROJECT_BACK_TO_REVIEW), any());
    }

    private void templates(Map<ProjectRole, String> files) {
        EmailMetaData metaData = new EmailMetaData();
        metaData.setSubject("Changes requested");
        metaData.setFiles(files);
        emailTemplates.setTemplates(Map.of(EmailTemplateType.PROJECT_BACK_TO_REVIEW, metaData));
    }

    private void adminAndCommitteeMember(String email) {
        when(userService.fetchAllProjectManagerAdmins()).thenReturn(List.of(new ProjectManagerAdminUser(1L, email)));
        when(userService.fetchAllCentralApprovalCommitteeUsers()).thenReturn(List.of(
                new CentralApprovalCommitteeUser(1L, email)));
    }

    private static BridgeheadsConfiguration.Contact contact(Map<String, String> name, String emailAddress) {
        BridgeheadsConfiguration.Contact contact = new BridgeheadsConfiguration.Contact();
        contact.setName(name);
        contact.setEmailAddress(emailAddress);
        return contact;
    }

    static class Endpoints {

        @EmailSender(templateType = EmailTemplateType.NEW_LOCAL_APPROVAL, projectStates = {ProjectState.LOCAL_APPROVAL},
                recipients = {EmailRecipientType.BRIDGEHEAD_APPROVAL_CONTACTS})
        @EmailSender(templateType = EmailTemplateType.NEW_LOCAL_APPROVAL_FINAL, projectStates = {ProjectState.FINAL},
                recipients = {EmailRecipientType.BRIDGEHEAD_APPROVAL_CONTACTS})
        public ResponseEntity<Void> upload(@ProjectCode Project project, @Bridgehead ProjectBridgehead bridgehead) {
            return ResponseEntity.ok().build();
        }

        @EmailSender(templateType = EmailTemplateType.NEW_PROJECT_ACCEPTED,
                recipients = {EmailRecipientType.CENTRAL_APPROVAL_COMMITTEE})
        public ResponseEntity<Void> accept(@ProjectCode Project project) {
            return ResponseEntity.ok().build();
        }

        @EmailSender(templateType = EmailTemplateType.PROJECT_BACK_TO_REVIEW, recipients = {
                EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.CENTRAL_APPROVAL_COMMITTEE})
        public ResponseEntity<Void> requestChanges(@ProjectCode Project project) {
            return ResponseEntity.ok().build();
        }

    }

}
