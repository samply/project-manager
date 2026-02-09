package de.samply.user;

import de.samply.db.model.*;
import de.samply.db.repository.*;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.User;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.project.state.UserProjectState;
import de.samply.security.SessionUser;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.OrganisationRoleToProjectRoleMapper;
import de.samply.user.roles.ProjectRole;
import de.samply.user.roles.UserProjectRoles;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    private final NotificationService notificationService;
    private final BridgeheadAdminUserRepository bridgeheadAdminUserRepository;
    private final ProjectManagerAdminUserRepository projectManagerAdminUserRepository;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private final UserRepository userRepository;
    private final CreatorUserRepository creatorUserRepository;
    private final ProjectRepository projectRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final SessionUser sessionUser;
    private final OrganisationRoleToProjectRoleMapper organisationRoleToProjectRoleMapper;
    private final DtoFactory dtoFactory;

    public UserService(NotificationService notificationService,
                       BridgeheadAdminUserRepository bridgeheadAdminUserRepository,
                       ProjectManagerAdminUserRepository projectManagerAdminUserRepository,
                       ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                       UserRepository userRepository,
                       CreatorUserRepository creatorUserRepository,
                       ProjectRepository projectRepository,
                       ProjectBridgeheadRepository projectBridgeheadRepository,
                       SessionUser sessionUser,
                       OrganisationRoleToProjectRoleMapper organisationRoleToProjectRoleMapper,
                       DtoFactory dtoFactory) {
        this.notificationService = notificationService;
        this.bridgeheadAdminUserRepository = bridgeheadAdminUserRepository;
        this.projectManagerAdminUserRepository = projectManagerAdminUserRepository;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.userRepository = userRepository;
        this.creatorUserRepository = creatorUserRepository;
        this.projectRepository = projectRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.sessionUser = sessionUser;
        this.organisationRoleToProjectRoleMapper = organisationRoleToProjectRoleMapper;
        this.dtoFactory = dtoFactory;
    }

    public void createBridgeheadAdminUserIfNotExists(@NotNull String email, @NotNull String bridgehead) {
        Optional<BridgeheadAdminUser> bridgeheadAdminUserOptional = this.bridgeheadAdminUserRepository.findFirstByEmailAndBridgehead(email, bridgehead);
        BridgeheadAdminUser result;
        if (bridgeheadAdminUserOptional.isEmpty()) {
            result = new BridgeheadAdminUser();
            result.setBridgehead(bridgehead);
            result.setEmail(email);
            this.bridgeheadAdminUserRepository.save(result);
        }
    }

    public void createProjectManagerAdminUserIfNotExists(@NotNull String email) {
        Optional<ProjectManagerAdminUser> projectManagerAdminUserOptional = this.projectManagerAdminUserRepository.findFirstByEmail(email);
        ProjectManagerAdminUser result;
        if (projectManagerAdminUserOptional.isEmpty()) {
            result = new ProjectManagerAdminUser();
            result.setEmail(email);
            this.projectManagerAdminUserRepository.save(result);
        }
    }

    public void setProjectBridgeheadUserWithRoleAndGenerateTokensIfDataShield(@NotNull String email, @NotNull String projectCode, @NotNull String bridgehead, @NotNull ProjectRole projectRole) throws UserServiceException {
        Optional<Project> project = this.projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new UserServiceException("Project " + projectCode + " not found");
        }
        Optional<ProjectBridgehead> projectBridgehead = this.projectBridgeheadRepository.findFirstByBridgeheadAndProject(bridgehead, project.get());
        if (projectBridgehead.isEmpty()) {
            throw new UserServiceException("Bridgehead " + bridgehead + " not involved in project " + projectCode);
        }
        Optional<ProjectBridgeheadUser> projectBridgeheadUserOptional = this.projectBridgeheadUserRepository.findFirstByEmailAndProjectBridgeheadAndProjectRole(email, projectBridgehead.get(), projectRole);
        if (projectBridgeheadUserOptional.isEmpty()) {
            ProjectBridgeheadUser projectBridgeheadUser = new ProjectBridgeheadUser();
            projectBridgeheadUser.setEmail(email);
            projectBridgeheadUser.setProjectRole(projectRole);
            projectBridgeheadUser.setProjectBridgehead(projectBridgehead.get());
            projectBridgeheadUser.setProjectState(UserProjectState.CREATED);
            projectBridgeheadUser.setModifiedAt(Instant.now());
            this.projectBridgeheadUserRepository.save(projectBridgeheadUser);
            this.notificationService.createNotification(projectCode, bridgehead, email, OperationType.ASSIGN_USER_TO_PROJECT,
                    "Set role " + projectRole + " to user", null, null);
        }
    }

    public void acceptProject(@NotNull String projectCode, @NotNull String bridgehead) throws UserServiceException {
        changeProjectState(projectCode, bridgehead, UserProjectState.ACCEPTED);
    }

    public void rejectProject(@NotNull String projectCode, @NotNull String bridgehead) throws UserServiceException {
        changeProjectState(projectCode, bridgehead, UserProjectState.REJECTED);
    }

    public void requestChangesInProject(@NotNull String projectCode, @NotNull String bridgehead) throws UserServiceException {
        changeProjectState(projectCode, bridgehead, UserProjectState.REQUEST_CHANGES);
    }

    private void changeProjectState(@NotNull String projectCode, @NotNull String bridgehead, @NotNull UserProjectState state) throws UserServiceException {
        Optional<ProjectBridgeheadUser> projectBridgeheadUser =
                projectBridgeheadUserRepository.getFirstByEmailAndProjectBridgeheadOrderByModifiedAtDesc(sessionUser.getEmail(),
                        fetchProjectBridgehead(projectCode, bridgehead));
        if (projectBridgeheadUser.isEmpty()) {
            throw new UserServiceException("Project " + projectCode + " for bridgehead " + bridgehead + " and user " + sessionUser.getEmail());
        }
        projectBridgeheadUser.get().setProjectState(state);
        projectBridgeheadUser.get().setModifiedAt(Instant.now());
        projectBridgeheadUserRepository.save(projectBridgeheadUser.get());
        this.notificationService.createNotification(projectCode, bridgehead, sessionUser.getEmail(), OperationType.CHANGE_PROJECT_BRIDGEHEAD_USER_EVALUATION,
                "Set project bridgehead user evaluation to " + state, null, null);
    }

    public Set<User> fetchUsersForAutocomplete(@NotNull String projectCode, @NotNull String partialEmail, @NotNull String bridgehead) {
        Set<User> allUsers = projectBridgeheadUserRepository.getDistinctByEmailContainingAndProjectBridgehead_Bridgehead(partialEmail, bridgehead).stream().map(dtoFactory::convertFilteringProjectRoleAndState).collect(Collectors.toSet());
        Set<User> alreadySetUsers = projectBridgeheadUserRepository.getDistinctByEmailContainingAndProjectBridgehead_BridgeheadAndUserAlreadySetForThisProjectInThisRole(partialEmail, bridgehead, projectCode).stream().map(dtoFactory::convertFilteringProjectRoleAndState).collect(Collectors.toSet());
        allUsers.removeAll(alreadySetUsers);
        return allUsers;
    }

    public Optional<User> fetchCurrentUser(@NotNull String projectCode, @NotNull String bridgehead) {
        Optional<ProjectBridgeheadUser> user = this.projectBridgeheadUserRepository.getFirstValidByEmailAndProjectBridgehead(sessionUser.getEmail(), fetchProjectBridgehead(projectCode, bridgehead));
        return user.map(dtoFactory::convert);
    }

    public Set<User> fetchProjectUsers(@NotNull String projectCode) throws UserServiceException {
        return projectBridgeheadRepository.findByProject(fetchProject(projectCode)).stream().flatMap(projectBridgehead -> fetchProjectUsers(projectBridgehead).stream()).collect(Collectors.toSet());
    }

    public Set<User> fetchProjectUsers(ProjectBridgehead projectBridgehead) throws UserServiceException {
        return (switch (projectBridgehead.getProject().getState()) {
            case DEVELOP ->
                    this.projectBridgeheadUserRepository.getDistinctByProjectRoleAndProjectBridgehead(ProjectRole.DEVELOPER, projectBridgehead);
            case PILOT ->
                    this.projectBridgeheadUserRepository.getDistinctByProjectRoleAndProjectBridgehead(ProjectRole.PILOT, projectBridgehead);
            case FINAL ->
                    this.projectBridgeheadUserRepository.getDistinctByProjectRoleAndProjectBridgehead(ProjectRole.FINAL, projectBridgehead);
            default -> new ArrayList<ProjectBridgeheadUser>();
        }).stream().map(dtoFactory::convert).collect(Collectors.toSet());
    }

    public boolean existInvitedUsers(@NotNull String projectCode) throws UserServiceException {
        return !fetchProjectUsers(projectCode).isEmpty();
    }

    private ProjectBridgehead fetchProjectBridgehead(String projectCode, String bridgehead) throws UserServiceException {
        Optional<ProjectBridgehead> projectBridgehead = projectBridgeheadRepository.findFirstByBridgeheadAndProject(bridgehead, fetchProject(projectCode));
        if (projectBridgehead.isEmpty()) {
            throw new UserServiceException("Project " + projectCode + " for bridgehead " + bridgehead + " not found");
        }
        return projectBridgehead.get();
    }

    private Project fetchProject(@NotNull String projectCode) throws UserServiceException {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new UserServiceException("Project " + projectCode + " not found");
        }
        return project.get();
    }

    public Set<ProjectRole> fetchProjectRoles(@NotNull String projectCode, Optional<String> bridgehead) throws UserServiceException {
        Optional<UserProjectRoles> userProjectRoles = organisationRoleToProjectRoleMapper.map(projectCode);
        if (userProjectRoles.isEmpty()) {
            return new HashSet<>();
        }
        Set<ProjectRole> result = userProjectRoles.get().getRolesNotDependentOnBridgeheads();
        bridgehead.ifPresent(s -> result.addAll(userProjectRoles.get().getBridgeheadRoles(s)));
        return result;
    }

    public Boolean isProjectManagerAdmin() {
        return sessionUser.getUserOrganisationRoles().containsRole(OrganisationRole.PROJECT_MANAGER_ADMIN);
    }

    public synchronized void addUserInformationIfNotExists(String email, String firstName, String lastName) {
        if (StringUtils.hasText(email) && (StringUtils.hasText(firstName) || StringUtils.hasText(lastName))) {
            Optional<de.samply.db.model.User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                de.samply.db.model.User user = new de.samply.db.model.User();
                user.setEmail(email);
                user.setFirstName(firstName);
                user.setLastName(lastName);
                userRepository.save(user);
                log.info("Added user information for {}", email);
            }
        }
    }

    public void updateUserInMailingBlackList(@NotNull String email, @NotNull boolean addedToBlackList) throws UserServiceException {
        if (StringUtils.hasText(email)) {
            Optional<de.samply.db.model.User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isPresent()) {
                userOptional.get().setInMailingBlackList(addedToBlackList);
                userRepository.save(userOptional.get());
            }
        }
    }

    public boolean isUserInMailingBlackList(@NotNull String email) throws UserServiceException {
        if (StringUtils.hasText(email)) {
            Optional<de.samply.db.model.User> userOptional = userRepository.findByEmail(email);
            return userOptional.isPresent() && userOptional.get().isInMailingBlackList();
        }
        return false;
    }

    public List<User> fetchMailingBlackList() {
        return userRepository.findByIsInMailingBlackListIsTrue().stream().map(DtoFactory::convert)
                .sorted(Comparator.comparing(User::firstName).thenComparing(User::lastName))
                .collect(Collectors.toList());
    }

    public List<User> fetchUsersForAutocompleteInMailingBlackList(String email) {
        return userRepository.findByEmailContainingIgnoreCaseAndIsInMailingBlackListIsFalse(email).stream().map(DtoFactory::convert).collect(Collectors.toList());
    }

    public void addCreatorIfNotExists() {
        sessionUser.getBridgeheads().forEach(bridgehead ->
                creatorUserRepository.findByEmailAndBridgehead(sessionUser.getEmail(), bridgehead)
                        .orElseGet(() -> {
                            CreatorUser creatorUser = new CreatorUser();
                            creatorUser.setBridgehead(bridgehead);
                            creatorUser.setEmail(sessionUser.getEmail());
                            return creatorUserRepository.save(creatorUser);
                        }));
    }

}
