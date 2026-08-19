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
}
