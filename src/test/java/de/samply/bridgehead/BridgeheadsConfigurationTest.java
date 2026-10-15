package de.samply.bridgehead;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BridgeheadsConfigurationTest {

    @Test
    void bindsMultilingualContactsFromEnvironmentVariables() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource(
                "test-systemEnvironment",
                Map.of(
                        "BRIDGEHEADS_CONFIG_MASTER_CONTACTS_0_NAME_EN", "Master team",
                        "BRIDGEHEADS_CONFIG_MASTER_CONTACTS_0_NAME_DE", "Master-Team",
                        "BRIDGEHEADS_CONFIG_MASTER_CONTACTS_0_DESCRIPTION_EN", "English description",
                        "BRIDGEHEADS_CONFIG_MASTER_CONTACTS_0_DESCRIPTION_DE", "Deutsche Beschreibung",
                        "BRIDGEHEADS_CONFIG_MASTER_CONTACTS_0_EMAILADDRESS", "master@example.org"
                )));
        ConfigurationPropertySources.attach(environment);

        BridgeheadsConfiguration configuration = Binder.get(environment)
                .bind("bridgeheads", Bindable.of(BridgeheadsConfiguration.class))
                .orElseThrow(IllegalStateException::new);

        BridgeheadsConfiguration.Contact contact = configuration.getConfig()
                .get("master")
                .getContacts()
                .getFirst();

        assertThat(contact.getName())
                .containsExactlyInAnyOrderEntriesOf(Map.of("en", "Master team", "de", "Master-Team"));
        assertThat(contact.getDescription())
                .containsExactlyInAnyOrderEntriesOf(Map.of("en", "English description", "de", "Deutsche Beschreibung"));
        assertThat(contact.getEmailAddress()).isEqualTo("master@example.org");
    }

    // Separate list from the site contacts, same shape; numbered in case a site has several
    @Test
    void bindsApprovalContactsSeparatelyFromTheSiteContacts() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource(
                "test-systemEnvironment",
                Map.of(
                        "BRIDGEHEADS_CONFIG_MASTER_CONTACTS_0_EMAILADDRESS", "master@example.org",
                        "BRIDGEHEADS_CONFIG_MASTER_APPROVALCONTACTS_0_NAME_EN", "Data use committee",
                        "BRIDGEHEADS_CONFIG_MASTER_APPROVALCONTACTS_0_EMAILADDRESS", "committee@example.org",
                        "BRIDGEHEADS_CONFIG_MASTER_APPROVALCONTACTS_1_EMAILADDRESS", "office@example.org"
                )));
        ConfigurationPropertySources.attach(environment);

        BridgeheadsConfiguration configuration = Binder.get(environment)
                .bind("bridgeheads", Bindable.of(BridgeheadsConfiguration.class))
                .orElseThrow(IllegalStateException::new);

        assertThat(configuration.getContacts("master")).extracting(BridgeheadsConfiguration.Contact::getEmailAddress)
                .containsExactly("master@example.org");
        assertThat(configuration.getApprovalContacts("master")).extracting(BridgeheadsConfiguration.Contact::getEmailAddress)
                .containsExactly("committee@example.org", "office@example.org");
        assertThat(configuration.getApprovalContacts("master").getFirst().getName())
                .containsExactlyEntriesOf(Map.of("en", "Data use committee"));
        assertThat(configuration.getApprovalContacts("unknown")).isEmpty();
    }
}
