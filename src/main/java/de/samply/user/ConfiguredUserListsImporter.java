package de.samply.user;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Makes sure that the addresses of the configuration ({@link ConfiguredUserLists}) are in the email lists of their
 * roles at startup, so that they get emails also before their first login. Additive: entries added at login or in the
 * configuration page are never removed.
 */
@Slf4j
@Component
public class ConfiguredUserListsImporter implements ApplicationRunner {

    private final ConfiguredUserLists configuredUserLists;
    private final UserService userService;

    public ConfiguredUserListsImporter(ConfiguredUserLists configuredUserLists, UserService userService) {
        this.configuredUserLists = configuredUserLists;
        this.userService = userService;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        configuredUserLists.getProjectManagerAdmins().forEach(userService::createProjectManagerAdminUserIfNotExists);
        configuredUserLists.getCentralApprovalCommitteeMembers()
                .forEach(userService::createCentralApprovalCommitteeUserIfNotExists);
        configuredUserLists.getBridgeheadAdmins().forEach((bridgehead, emails) ->
                emails.forEach(email -> userService.createBridgeheadAdminUserIfNotExists(email, bridgehead)));
        // Counts only: the addresses themselves are not logged
        log.info("Configured email lists loaded: {} project manager admins, {} committee members, {} site admins",
                configuredUserLists.getProjectManagerAdmins().size(),
                configuredUserLists.getCentralApprovalCommitteeMembers().size(),
                configuredUserLists.getBridgeheadAdmins().values().stream().mapToInt(Set::size).sum());
    }

}
