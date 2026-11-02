package de.samply.bridgehead;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    private static BridgeheadsConfiguration bindAndInit(Map<String, String> variables) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(
                new SystemEnvironmentPropertySource("test-systemEnvironment", Map.<String, Object>copyOf(variables)));
        ConfigurationPropertySources.attach(environment);
        BridgeheadsConfiguration configuration = Binder.get(environment)
                .bind("bridgeheads", Bindable.of(BridgeheadsConfiguration.class))
                .orElseThrow(IllegalStateException::new);
        ReflectionTestUtils.invokeMethod(configuration, "initIdBridgeheadMaps");
        return configuration;
    }

    // BRIDGEHEADS_CONFIG_<ID>_GROUP: several bridgeheads of one site share its group; without it the group is the id
    @Test
    void groupsBridgeheadsBySiteGroup() {
        BridgeheadsConfiguration configuration = bindAndInit(Map.of(
                "BRIDGEHEADS_CONFIG_BERLIN_EXPLORERID", "lens-berlin",
                "BRIDGEHEADS_CONFIG_BERLINMINUSTEST_EXPLORERID", "lens-berlin-test",
                "BRIDGEHEADS_CONFIG_BERLINMINUSTEST_GROUP", " Berlin ",
                "BRIDGEHEADS_CONFIG_ESSEN_EXPLORERID", "lens-essen"));

        assertThat(configuration.getBridgeheadsForGroup("berlin")).containsExactly("berlin", "berlin-test");
        assertThat(configuration.getBridgeheadsForGroup("BERLIN")).containsExactly("berlin", "berlin-test");
        assertThat(configuration.getBridgeheadsForGroup("essen")).containsExactly("essen");
        assertThat(configuration.getBridgeheadsForGroup("berlin-test")).isEmpty();
        assertThat(configuration.getBridgeheadsForGroup("goettingen")).isEmpty();
        assertThat(configuration.getBridgeheadsForGroup(null)).isEmpty();
    }

    // Two bridgeheads with the same id: one of them would be unreachable, the start fails
    @Test
    void failsOnDuplicateIds() {
        assertThatThrownBy(() -> bindAndInit(Map.of(
                "BRIDGEHEADS_CONFIG_BERLIN_EXPLORERID", "lens-berlin",
                "BRIDGEHEADS_CONFIG_BERLINMINUSTEST_EXPLORERID", "lens-berlin")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("explorer id")
                .hasMessageContaining("lens-berlin [berlin, berlin-test]");
        assertThatThrownBy(() -> bindAndInit(Map.of(
                "BRIDGEHEADS_CONFIG_A_FOCUSBEAMID", "focus.x.broker",
                "BRIDGEHEADS_CONFIG_B_FOCUSBEAMID", "focus.x.broker")))
                .hasMessageContaining("focus beam id");
        assertThatThrownBy(() -> bindAndInit(Map.of(
                "BRIDGEHEADS_CONFIG_A_TOKENMANAGERID", "token-manager.x.broker",
                "BRIDGEHEADS_CONFIG_B_TOKENMANAGERID", "token-manager.x.broker")))
                .hasMessageContaining("token manager id");
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
