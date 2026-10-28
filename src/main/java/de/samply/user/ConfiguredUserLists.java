package de.samply.user;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.utils.EmailUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The addresses the configuration puts into the email lists of the roles (docs/user-lists.md): they are loaded at
 * startup and cannot be removed in the configuration page, only in the configuration. Stored normalized (lower case);
 * a malformed address stops the start.
 */
@Component
public class ConfiguredUserLists {

    private final Set<String> projectManagerAdmins;
    private final Set<String> centralApprovalCommitteeMembers;
    // Bridgehead -> addresses of its admins
    private final Map<String, Set<String>> bridgeheadAdmins;

    public ConfiguredUserLists(
            @Value(ProjectManagerConst.PROJECT_MANAGER_ADMIN_EMAILS_SV) List<String> projectManagerAdmins,
            @Value(ProjectManagerConst.CENTRAL_APPROVAL_COMMITTEE_EMAILS_SV) List<String> centralApprovalCommitteeMembers,
            BridgeheadsConfiguration bridgeheadsConfiguration) {
        this.projectManagerAdmins = clean(projectManagerAdmins, ProjectManagerConst.PROJECT_MANAGER_ADMIN_EMAILS);
        this.centralApprovalCommitteeMembers = clean(centralApprovalCommitteeMembers,
                ProjectManagerConst.CENTRAL_APPROVAL_COMMITTEE_EMAILS);
        this.bridgeheadAdmins = bridgeheadsConfiguration.getConfig().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> clean(entry.getValue().getAdminEmails(),
                        "BRIDGEHEADS_CONFIG_" + entry.getKey() + "_ADMINEMAILS")));
    }

    public Set<String> getProjectManagerAdmins() {
        return projectManagerAdmins;
    }

    public Set<String> getCentralApprovalCommitteeMembers() {
        return centralApprovalCommitteeMembers;
    }

    public Map<String, Set<String>> getBridgeheadAdmins() {
        return bridgeheadAdmins;
    }

    // Addresses are compared normalized: the login may write them differently
    public boolean isProjectManagerAdmin(String email) {
        return projectManagerAdmins.contains(EmailUtils.normalize(email));
    }

    public boolean isCentralApprovalCommitteeMember(String email) {
        return centralApprovalCommitteeMembers.contains(EmailUtils.normalize(email));
    }

    public boolean isBridgeheadAdmin(String email, String bridgehead) {
        return bridgeheadAdmins.getOrDefault(bridgehead, Set.of()).contains(EmailUtils.normalize(email));
    }

    private static Set<String> clean(Collection<String> emails, String variable) {
        if (emails == null) {
            return Set.of();
        }
        List<String> invalid = emails.stream()
                .filter(StringUtils::hasText)
                .filter(email -> !EmailUtils.isValid(email))
                .toList();
        if (!invalid.isEmpty()) {
            throw new IllegalStateException("Invalid email address in " + variable + ": " + String.join(", ", invalid));
        }
        return emails.stream()
                .filter(StringUtils::hasText)
                .map(EmailUtils::normalize)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

}
