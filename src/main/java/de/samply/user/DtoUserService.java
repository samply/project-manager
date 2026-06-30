package de.samply.user;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.User;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DtoUserService {

    private final UserService userService;
    private final DtoFactory dtoFactory;

    public DtoUserService(UserService userService, DtoFactory dtoFactory) {
        this.userService = userService;
        this.dtoFactory = dtoFactory;
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

    private Set<User> convert(Collection<ProjectBridgeheadUser> users) {
        return users.stream().map(dtoFactory::convert).collect(Collectors.toSet());
    }

}
