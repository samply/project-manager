package de.samply.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.user.roles.ProjectRole;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EmailMetaDataTest {

    private static final String TEMPLATES = """
            {
              "templates": {
                "LOCAL_APPROVAL_STARTED": {
                  "subject": "Local approval",
                  "files": { "BRIDGEHEAD_APPROVAL_CONTACT": "contact.html", "DEFAULT": "default.html" },
                  "bridgeheads": {
                    "jurassic-park": { "subject": "Local approval at Jurassic Park",
                                       "files": { "BRIDGEHEAD_APPROVAL_CONTACT": "contact-jp.html" } },
                    "star-wars": { "files": { "DEFAULT": "default-sw.html" } }
                  }
                }
              }
            }
            """;

    private final EmailTemplates templates = read();

    // Order: bridgehead + role, bridgehead + DEFAULT, role, DEFAULT; the bridgehead's subject if it has one
    @Test
    void choosesTheTemplateOfTheBridgeheadFirst() {
        assertThat(lookup(ProjectRole.BRIDGEHEAD_APPROVAL_CONTACT, "jurassic-park"))
                .contains(new TemplateSubject("contact-jp.html", "Local approval at Jurassic Park"));
        assertThat(lookup(ProjectRole.BRIDGEHEAD_APPROVAL_CONTACT, "star-wars"))
                .contains(new TemplateSubject("default-sw.html", "Local approval"));
        assertThat(lookup(ProjectRole.CREATOR, "jurassic-park"))
                .contains(new TemplateSubject("default.html", "Local approval"));
        assertThat(lookup(ProjectRole.BRIDGEHEAD_APPROVAL_CONTACT, "other"))
                .contains(new TemplateSubject("contact.html", "Local approval"));
        assertThat(templates.getTemplateAndSubject(EmailTemplateType.LOCAL_APPROVAL_STARTED,
                ProjectRole.BRIDGEHEAD_APPROVAL_CONTACT, Optional.empty()))
                .contains(new TemplateSubject("contact.html", "Local approval"));
    }

    // No template: no email
    @Test
    void anUnconfiguredTypeHasNoTemplate() {
        assertThat(templates.getTemplateAndSubject(EmailTemplateType.NEW_LOCAL_APPROVAL, ProjectRole.CREATOR,
                Optional.of("jurassic-park"))).isEmpty();
    }

    private Optional<TemplateSubject> lookup(ProjectRole role, String bridgehead) {
        return templates.getTemplateAndSubject(EmailTemplateType.LOCAL_APPROVAL_STARTED, role, Optional.of(bridgehead));
    }

    private static EmailTemplates read() {
        try {
            return new ObjectMapper().readValue(TEMPLATES, EmailTemplates.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
