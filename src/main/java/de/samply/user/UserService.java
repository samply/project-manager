package de.samply.user;

import de.samply.db.model.*;
import de.samply.db.repository.BridgeheadAdminUserRepository;
import de.samply.db.repository.CreatorUserRepository;
import de.samply.db.repository.ProjectManagerAdminUserRepository;
import de.samply.db.repository.UserRepository;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.project.ProjectBridgeheadService;
import de.samply.project.ProjectBridgeheadUserService;
import de.samply.project.ProjectService;
import de.samply.project.state.ProjectState;
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

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    private final SessionUser sessionUser;
    private final OrganisationRoleToProjectRoleMapper organisationRoleToProjectRoleMapper;

    // Services
    private final NotificationService notificationService;
    private final ProjectService projectService;
    private final ProjectBridgeheadService projectBridgeheadService;
    private final ProjectBridgeheadUserService projectBridgeheadUserService;

    // Repositories
    private final BridgeheadAdminUserRepository bridgeheadAdminUserRepository;
    private final ProjectManagerAdminUserRepository projectManagerAdminUserRepository;
    private final UserRepository userRepository;
    private final CreatorUserRepository creatorUserRepository;


    public UserService(NotificationService notificationService,
                       BridgeheadAdminUserRepository bridgeheadAdminUserRepository,
                       ProjectManagerAdminUserRepository projectManagerAdminUserRepository,
                       UserRepository userRepository,
                       CreatorUserRepository creatorUserRepository,
                       SessionUser sessionUser,
                       OrganisationRoleToProjectRoleMapper organisationRoleToProjectRoleMapper,
                       ProjectBridgeheadService projectBridgeheadService,
                       ProjectBridgeheadUserService projectBridgeheadUserService,
                       ProjectService projectService) {
        this.notificationService = notificationService;
        this.bridgeheadAdminUserRepository = bridgeheadAdminUserRepository;
        this.projectManagerAdminUserRepository = projectManagerAdminUserRepository;
        this.userRepository = userRepository;
        this.creatorUserRepository = creatorUserRepository;
        this.sessionUser = sessionUser;
        this.organisationRoleToProjectRoleMapper = organisationRoleToProjectRoleMapper;
        this.projectBridgeheadService = projectBridgeheadService;
        this.projectBridgeheadUserService = projectBridgeheadUserService;
        this.projectService = projectService;
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

    public void setProjectBridgeheadUserWithRoleAndGenerateTokensIfDataShield(@NotNull String email, @NotNull Project project, @NotNull ProjectBridgehead bridgehead, @NotNull ProjectRole projectRole) throws UserServiceException {
        Optional<ProjectBridgeheadUser> projectBridgeheadUserOptional = projectBridgeheadUserService.fetchFirstUser(email, bridgehead, projectRole);
        if (projectBridgeheadUserOptional.isEmpty()) {
            ProjectBridgeheadUser projectBridgeheadUser = new ProjectBridgeheadUser();
            projectBridgeheadUser.setEmail(email);
            projectBridgeheadUser.setProjectRole(projectRole);
            projectBridgeheadUser.setProjectBridgehead(bridgehead);
            projectBridgeheadUser.setProjectState(UserProjectState.CREATED);
            projectBridgeheadUserService.saveUser(projectBridgeheadUser);
            this.notificationService.createNotification(project, bridgehead.getBridgehead(), email, OperationType.ASSIGN_USER_TO_PROJECT,
                    "Set role " + projectRole + " to user", null, null);
        }
    }

    public void acceptProject(@NotNull Project project, @NotNull ProjectBridgehead bridgehead) throws UserServiceException {
        changeProjectState(project, bridgehead, UserProjectState.ACCEPTED);
    }

    public void rejectProject(@NotNull Project project, @NotNull ProjectBridgehead bridgehead) throws UserServiceException {
        changeProjectState(project, bridgehead, UserProjectState.REJECTED);
    }

    public void requestChangesInProject(@NotNull Project project, @NotNull ProjectBridgehead bridgehead) throws UserServiceException {
        changeProjectState(project, bridgehead, UserProjectState.REQUEST_CHANGES);
    }

    private void changeProjectState(@NotNull Project project, @NotNull ProjectBridgehead bridgehead, @NotNull UserProjectState state) throws UserServiceException {
        Optional<ProjectBridgeheadUser> projectBridgeheadUser =
                projectBridgeheadUserService.fetchFirstUsersOrderByModifiedAtDesc(sessionUser.getEmail(), bridgehead);
        if (projectBridgeheadUser.isEmpty()) {
            throw new UserServiceException("ProjectCode " + project.getCode() + " for bridgehead " + bridgehead.getBridgehead() + " and user " + sessionUser.getEmail());
        }
        projectBridgeheadUser.get().setProjectState(state);
        projectBridgeheadUserService.saveUser(projectBridgeheadUser.get());
        this.notificationService.createNotification(project, bridgehead.getBridgehead(), sessionUser.getEmail(), OperationType.CHANGE_PROJECT_BRIDGEHEAD_USER_EVALUATION,
                "Set project bridgehead user evaluation to " + state, null, null);
    }

    protected List<ProjectBridgeheadUser> fetchAllUsers(@NotNull String partialEmail, @NotNull ProjectBridgehead bridgehead) {
        return projectBridgeheadUserService.fetchDistinctUsers(partialEmail, bridgehead);
    }

    protected List<ProjectBridgeheadUser> fetchAlreadySetUsers(@NotNull String partialEmail, @NotNull ProjectBridgehead bridgehead) {
        return projectBridgeheadUserService.fetchUsersAlreadySetInThisProjectForThisRole(partialEmail, bridgehead);
    }

    protected Optional<ProjectBridgeheadUser> fetchCurrentUser(@NotNull ProjectBridgehead bridgehead) {
        return projectBridgeheadUserService.fetchFirstValidUser(sessionUser.getEmail(), bridgehead);
    }

    protected Set<String> fetchProjectCreators(Optional<Project> project) {
        return project
                .map(value -> Optional.ofNullable(value.getCreatorEmail()).stream().collect(Collectors.toSet()))
                .orElseGet(() -> projectService.fetchAllUserVisibleProjects().stream()
                        .filter(value -> value.getState() != ProjectState.DRAFT)
                        .map(Project::getCreatorEmail)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()));
    }

    public List<ProjectBridgeheadUser> fetchProjectUsers(ProjectBridgehead projectBridgehead) throws UserServiceException {
        return (switch (projectBridgehead.getProject().getState()) {
            case DEVELOP -> projectBridgeheadUserService.fetchUsers(ProjectRole.DEVELOPER, projectBridgehead);
            case PILOT -> projectBridgeheadUserService.fetchUsers(ProjectRole.PILOT, projectBridgehead);
            case FINAL -> projectBridgeheadUserService.fetchUsers(ProjectRole.FINAL, projectBridgehead);
            default -> new ArrayList<>();
        });
    }

    protected Set<ProjectBridgeheadUser> fetchProjectUsers(@NotNull Project project) throws UserServiceException {
        return projectBridgeheadService
                .fetchBridgeheads(project)
                .stream()
                .flatMap(projectBridgehead ->
                        fetchProjectUsers(projectBridgehead).stream())
                .collect(Collectors.toSet());
    }

    public boolean existInvitedUsers(@NotNull Project project) throws UserServiceException {
        return !fetchProjectUsers(project).isEmpty();
    }

    public Set<ProjectRole> fetchProjectRoles(@NotNull Project project, Optional<ProjectBridgehead> bridgehead) throws UserServiceException {
        Optional<UserProjectRoles> userProjectRoles = organisationRoleToProjectRoleMapper.map(project);
        if (userProjectRoles.isEmpty()) {
            return new HashSet<>();
        }
        Set<ProjectRole> result = userProjectRoles.get().getRolesNotDependentOnBridgeheads();
        bridgehead.ifPresent(s -> result.addAll(userProjectRoles.get().getBridgeheadRoles(s.getBridgehead())));
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

    protected List<User> fetchMailingBlackList() {
        return userRepository.findByIsInMailingBlackListIsTrue();
    }

    protected List<User> fetchUsersForAutocompleteInMailingBlackList(String email) {
        return userRepository.findByEmailContainingIgnoreCaseAndIsInMailingBlackListIsFalse(email);
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

    public List<ProjectManagerAdminUser> fetchAllProjectManagerAdmins() {
        return projectManagerAdminUserRepository.findAll();
    }

    public Set<CreatorUser> fetchCreatorUser(String email) {
        return creatorUserRepository.findByEmail(email);
    }

    public Optional<User> fetchUser(String user) {
        return userRepository.findByEmail(user);
    }

    public Set<BridgeheadAdminUser> fetchBridgeheadAdmin(ProjectBridgehead bridgehead) {
        return bridgeheadAdminUserRepository.findByBridgehead(bridgehead.getBridgehead());
    }

    public Optional<BridgeheadAdminUser> fetchFirstBridgeheadAdmin(ProjectBridgehead bridgehead) {
        return fetchFirstBridgeheadAdmin(bridgehead.getBridgehead());
    }

    public Optional<BridgeheadAdminUser> fetchFirstBridgeheadAdmin(String bridgehead) {
        return bridgeheadAdminUserRepository.findFirstByBridgeheadOrderByIdAsc(bridgehead);
    }

}
