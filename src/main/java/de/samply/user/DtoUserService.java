package de.samply.user;

import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.User;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DtoUserService {

    private final UserService userService;
    private final DtoFactory dtoFactory;
    private final BridgeheadsConfiguration bridgeheadsConfiguration;

    public DtoUserService(UserService userService, DtoFactory dtoFactory,
                          BridgeheadsConfiguration bridgeheadsConfiguration) {
        this.userService = userService;
        this.dtoFactory = dtoFactory;
        this.bridgeheadsConfiguration = bridgeheadsConfiguration;
    }

    public Set<User> fetchUsersForAutocomplete(@NotNull String partialEmail, @NotNull ProjectBridgehead bridgehead) {
        Set<User> allUsers = userService
                .fetchAllUsers(partialEmail, bridgehead)
                .stream()
                .map(dtoFactory::convertFilteringProjectRoleAndState)
                .collect(Collectors.toSet());
        Set<User> alreadySetUsers = userService
                .fetchAlreadySetUsers(partialEmail, bridgehead)
                .stream()
                .map(dtoFactory::convertFilteringProjectRoleAndState)
                .collect(Collectors.toSet());
        allUsers.removeAll(alreadySetUsers);
        return allUsers;
    }

    public Optional<User> fetchCurrentUser(@NotNull ProjectBridgehead bridgehead) {
        return userService
                .fetchCurrentUser(bridgehead)
                .map(dtoFactory::convert);
    }

    public Set<User> fetchProjectUsers(@NotNull Project project) throws UserServiceException {
        return convert(userService.fetchProjectUsers(project));
    }

    public Set<User> fetchProjectCreators(Optional<Project> project) {
        return userService.fetchProjectCreators(project).stream()
                .map(email -> userService.fetchUser(email)
                        .map(DtoFactory::convert)
                        .orElseGet(() -> new User(email, null, null, null, null, null, null)))
                .sorted(Comparator.comparing(this::sortValue, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public List<User> fetchMailingBlackList() {
        return userService
                .fetchMailingBlackList()
                .stream()
                .map(DtoFactory::convert)
                .sorted(Comparator.comparing(User::firstName).thenComparing(User::lastName))
                .collect(Collectors.toList());
    }

    public List<User> fetchUsersForAutocompleteInMailingBlackList(String email) {
        return userService
                .fetchUsersForAutocompleteInMailingBlackList(email)
                .stream()
                .map(DtoFactory::convert)
                .collect(Collectors.toList());
    }

    // The email lists of the configuration page: name from the user table if the person has logged in already
    public List<User> fetchProjectManagerAdmins() {
        return sortByName(userService.fetchAllProjectManagerAdmins().stream()
                .map(admin -> convertEmail(admin.getEmail(), null)));
    }

    public List<User> fetchCentralApprovalCommitteeMembers() {
        return sortByName(userService.fetchAllCentralApprovalCommitteeUsers().stream()
                .map(member -> convertEmail(member.getEmail(), null)));
    }

    public List<User> fetchBridgeheadAdmins() {
        return userService.fetchAllBridgeheadAdmins().stream()
                .map(admin -> convertEmail(admin.getEmail(), admin.getBridgehead()))
                .sorted(Comparator.comparing(User::bridgehead).thenComparing(this::sortValue, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private User convertEmail(String email, String bridgehead) {
        Optional<de.samply.db.model.User> user = userService.fetchUser(email);
        return new User(email, user.map(de.samply.db.model.User::getFirstName).orElse(null),
                user.map(de.samply.db.model.User::getLastName).orElse(null), bridgehead,
                Optional.ofNullable(bridgehead).flatMap(bridgeheadsConfiguration::getHumanReadable).orElse(null),
                null, null);
    }

    private List<User> sortByName(Stream<User> users) {
        return users.sorted(Comparator.comparing(this::sortValue, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private Set<User> convert(Collection<ProjectBridgeheadUser> users) {
        return users.stream().map(dtoFactory::convert).collect(Collectors.toSet());
    }

    private String sortValue(User user) {
        String firstName = user.firstName() == null ? "" : user.firstName();
        String lastName = user.lastName() == null ? "" : user.lastName();
        String name = (firstName + " " + lastName).trim();
        return name.isBlank() ? user.email() : name;
    }

}
