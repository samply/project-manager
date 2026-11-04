package de.samply.frontend;

import de.samply.app.ProjectManagerController;
import de.samply.email.EmailMetaData;
import de.samply.email.EmailTemplateType;
import de.samply.email.EmailTemplates;
import de.samply.user.roles.ProjectRole;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// "An email is sent to ...": only who really gets an email in this deployment
class FrontendServiceEmailRecipientsTest {

    private static Method controllerMethod(String name) {
        return Arrays.stream(ProjectManagerController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static EmailTemplates templates(EmailTemplateType type, Map<ProjectRole, String> files) {
        EmailMetaData metaData = new EmailMetaData();
        metaData.setFiles(new HashMap<>(files));
        EmailTemplates templates = new EmailTemplates();
        templates.getTemplates().put(type, metaData);
        return templates;
    }

    @Test
    void recipientsOfEveryEmailSenderAreListedOnce() {
        EmailTemplates templates = new EmailTemplates();
        Arrays.stream(EmailTemplateType.values()).forEach(type -> {
            EmailMetaData metaData = new EmailMetaData();
            metaData.setFiles(new HashMap<>(Map.of(ProjectRole.DEFAULT, "default.html")));
            templates.getTemplates().put(type, metaData);
        });

        assertThat(FrontendService.fetchEmailRecipients(controllerMethod("acceptProject"), templates, true))
                .containsExactlyInAnyOrder("CREATOR", "ALL_BRIDGEHEAD_ADMINS", "CENTRAL_APPROVAL_COMMITTEE");
    }

    @Test
    void actionsWithoutEmailSenderHaveNoRecipients() {
        assertThat(FrontendService.fetchEmailRecipients(controllerMethod("fetchProject"), new EmailTemplates(), true))
                .isEmpty();
    }

    // DKTK: no committee template for "Request changes", so the committee is not mentioned
    @Test
    void recipientsWithoutTemplateAreLeftOut() {
        EmailTemplates templates = templates(EmailTemplateType.PROJECT_BACK_TO_REVIEW, Map.of(
                ProjectRole.CREATOR, "creator.html", ProjectRole.PROJECT_MANAGER_ADMIN, "admin.html"));

        assertThat(FrontendService.fetchEmailRecipients(controllerMethod("requestChangesInApproval"), templates, true))
                .containsExactlyInAnyOrder("CREATOR", "PROJECT_MANAGER_ADMIN");
    }

    @Test
    void aDefaultTemplateReachesEveryone() {
        EmailTemplates templates = templates(EmailTemplateType.PROJECT_BACK_TO_REVIEW,
                Map.of(ProjectRole.DEFAULT, "default.html"));

        assertThat(FrontendService.fetchEmailRecipients(controllerMethod("requestChangesInApproval"), templates, true))
                .containsExactlyInAnyOrder("CREATOR", "PROJECT_MANAGER_ADMIN", "CENTRAL_APPROVAL_COMMITTEE");
    }

    // A template of one site is enough: the email reaches that site
    @Test
    void aTemplateOfOneSiteCounts() {
        EmailMetaData site = new EmailMetaData();
        site.setFiles(new HashMap<>(Map.of(ProjectRole.CENTRAL_APPROVAL_COMMITTEE, "site.html")));
        EmailTemplates templates = templates(EmailTemplateType.PROJECT_BACK_TO_REVIEW,
                Map.of(ProjectRole.CREATOR, "creator.html"));
        templates.getTemplates().get(EmailTemplateType.PROJECT_BACK_TO_REVIEW).getBridgeheads().put("berlin", site);

        assertThat(FrontendService.fetchEmailRecipients(controllerMethod("requestChangesInApproval"), templates, true))
                .containsExactlyInAnyOrder("CREATOR", "CENTRAL_APPROVAL_COMMITTEE");
    }

    @Test
    void nobodyWithEmailsDisabled() {
        EmailTemplates templates = templates(EmailTemplateType.PROJECT_BACK_TO_REVIEW,
                Map.of(ProjectRole.DEFAULT, "default.html"));

        assertThat(FrontendService.fetchEmailRecipients(controllerMethod("requestChangesInApproval"), templates, false))
                .isEmpty();
    }
}
