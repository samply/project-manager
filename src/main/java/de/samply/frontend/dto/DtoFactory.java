package de.samply.frontend.dto;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.db.model.BridgeheadAdminUser;
import de.samply.db.model.NotificationUserAction;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.repository.BridgeheadAdminUserRepository;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.db.repository.UserRepository;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.UserProjectState;
import de.samply.user.roles.ProjectRole;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class DtoFactory {

    private final BridgeheadConfiguration bridgeheadConfiguration;
    private final UserRepository userRepository;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private final BridgeheadAdminUserRepository bridgeheadAdminUserRepository;

    public DtoFactory(BridgeheadConfiguration bridgeheadConfiguration,
                      UserRepository userRepository,
                      ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                      BridgeheadAdminUserRepository bridgeheadAdminUserRepository) {
        this.bridgeheadConfiguration = bridgeheadConfiguration;
        this.userRepository = userRepository;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.bridgeheadAdminUserRepository = bridgeheadAdminUserRepository;
    }

    public Project convert(@NotNull de.samply.db.model.Project project) {
        Project result = new Project();
        result.setCode(project.getCode());
        result.setCreatorEmail(project.getCreatorEmail());
        result.setCreatorName(fetchEmailUserName(project.getCreatorEmail()));
        result.setCreatedAt(project.getCreatedAt());
        result.setExpiresAt(project.getExpiresAt());
        result.setArchivedAt(project.getArchivedAt());
        result.setModifiedAt(project.getModifiedAt());
        result.setState(project.getState());
        result.setCustomConfig(project.isCustomConfig());
        result.setType(project.getType());
        result.setQuery(project.getQuery().getQuery());
        result.setHumanReadable(project.getQuery().getHumanReadable());
        result.setQueryFormat(project.getQuery().getQueryFormat());
        result.setOutputFormat(project.getQuery().getOutputFormat());
        result.setTemplateId(project.getQuery().getTemplateId());
        result.setLabel(project.getQuery().getLabel());
        result.setDescription(project.getQuery().getDescription());
        result.setExplorerUrl(project.getQuery().getExplorerUrl());
        result.setQueryContext(project.getQuery().getContext());
        return result;
    }

    private String fetchEmailUserName(String email) {
        Optional<de.samply.db.model.User> user = userRepository.findByEmail(email);
        return (user.isPresent()) ?
                user.get().getFirstName() + " " + user.get().getLastName() :
                null;
    }

    public static de.samply.db.model.Project convert(@NotNull Project projectConfiguration, @NotNull de.samply.db.model.Project project) {
        if (projectConfiguration.getExpiresAt() != null) {
            project.setExpiresAt(projectConfiguration.getExpiresAt());
        }
        if (projectConfiguration.getType() != null) {
            project.setType(projectConfiguration.getType());
        }
        if (projectConfiguration.getQuery() != null) {
            project.getQuery().setQuery(projectConfiguration.getQuery());
        }
        if (projectConfiguration.getHumanReadable() != null) {
            project.getQuery().setHumanReadable(projectConfiguration.getHumanReadable());
        }
        if (projectConfiguration.getQueryFormat() != null) {
            project.getQuery().setQueryFormat(projectConfiguration.getQueryFormat());
        }
        if (projectConfiguration.getOutputFormat() != null) {
            project.getQuery().setOutputFormat(projectConfiguration.getOutputFormat());
        }
        if (projectConfiguration.getTemplateId() != null) {
            project.getQuery().setTemplateId(projectConfiguration.getTemplateId());
        }
        if (projectConfiguration.getQueryContext() != null) {
            project.getQuery().setContext(projectConfiguration.getQueryContext());
        }
        return project;
    }

    public Notification convert(@NotNull de.samply.db.model.Notification notification, Supplier<NotificationUserAction> userActionSupplier) {
        return new Notification(
                notification.getId(),
                notification.getEmail(),
                notification.getTimestamp(),
                notification.getProject().getCode(),
                notification.getBridgehead(),
                fetchHumanReadableBridgehead(notification.getBridgehead()),
                notification.getOperationType(),
                notification.getDetails(),
                notification.getError(),
                notification.getHttpStatus(),
                userActionSupplier.get().isRead()
        );
    }

    public ProjectDocument convert(@NotNull de.samply.db.model.ProjectDocument projectDocument) {
        return new ProjectDocument(
                projectDocument.getProject().getCode(),
                projectDocument.getOriginalFilename(),
                projectDocument.getUrl(),
                projectDocument.getCreatedAt(),
                projectDocument.getBridgehead(),
                fetchHumanReadableBridgehead(projectDocument.getBridgehead()),
                projectDocument.getCreatorEmail(),
                fetchEmailUserName(projectDocument.getCreatorEmail()),
                projectDocument.getLabel(),
                projectDocument.getDocumentType()
        );
    }

    public ProjectBridgehead convert(@NotNull de.samply.db.model.ProjectBridgehead projectBridgehead) {
        return new ProjectBridgehead(
                projectBridgehead.getProject().getCode(),
                projectBridgehead.getBridgehead(),
                fetchHumanReadableBridgehead(projectBridgehead),
                projectBridgehead.getState(),
                projectBridgehead.getModifiedAt(),
                projectBridgehead.getQueryState()
        );
    }

    public String fetchHumanReadableBridgehead(@NotNull de.samply.db.model.ProjectBridgehead projectBridgehead) {
        return fetchHumanReadableBridgehead(projectBridgehead.getBridgehead());
    }

    public String fetchHumanReadableBridgehead(@NotNull String bridgehead) {
        Optional<String> humanReadable = bridgeheadConfiguration.getHumanReadable(bridgehead);
        return (humanReadable.isPresent()) ? humanReadable.get() : bridgehead;
    }

    public User convert(@NotNull de.samply.db.model.ProjectBridgeheadUser projectBridgeheadUser) {
        Optional<de.samply.db.model.User> user = userRepository.findByEmail(projectBridgeheadUser.getEmail());
        return new User(
                projectBridgeheadUser.getEmail(),
                user.isPresent() ? user.get().getFirstName() : null,
                user.isPresent() ? user.get().getLastName() : null,
                projectBridgeheadUser.getProjectBridgehead().getBridgehead(),
                fetchHumanReadableBridgehead(projectBridgeheadUser.getProjectBridgehead()),
                projectBridgeheadUser.getProjectRole(),
                projectBridgeheadUser.getProjectState()
        );
    }

    public User convertFilteringProjectRoleAndState(@NotNull de.samply.db.model.ProjectBridgeheadUser projectBridgeheadUser) {
        Optional<de.samply.db.model.User> user = userRepository.findByEmail(projectBridgeheadUser.getEmail());
        return new User(
                projectBridgeheadUser.getEmail(),
                user.isPresent() ? user.get().getFirstName() : null,
                user.isPresent() ? user.get().getLastName() : null,
                projectBridgeheadUser.getProjectBridgehead().getBridgehead(),
                fetchHumanReadableBridgehead(projectBridgeheadUser.getProjectBridgehead()),
                null,
                null
        );
    }

    public Bridgehead convertToBridgehead(@NotNull String bridgehead) {
        Optional<String> humanReadable = bridgeheadConfiguration.getHumanReadable(bridgehead);
        return humanReadable.isPresent() ? new Bridgehead(bridgehead, humanReadable.get()) : new Bridgehead(bridgehead, null);
    }

    public static User convert(de.samply.db.model.User user) {
        return new User(user.getEmail(), user.getFirstName(), user.getLastName(), null, null, null, null);
    }

    public Results fetchResults(@NotNull de.samply.db.model.Project project) {
        Set<ProjectBridgeheadUser> finalUsers = projectBridgeheadUserRepository.getDistinctByProjectRoleAndProjectCode(ProjectRole.FINAL, project.getCode());
        Optional<ProjectBridgeheadUser> finalUser = finalUsers.stream().filter(user -> user.getProjectState() == UserProjectState.ACCEPTED).findAny();
        AtomicReference<Optional<String>> email = new AtomicReference<>(Optional.empty());
        AtomicReference<Optional<String>> firstName = new AtomicReference<>(Optional.empty());
        AtomicReference<Optional<String>> lastName = new AtomicReference<>(Optional.empty());
        if (finalUser.isEmpty()) {
            finalUser = finalUsers.stream().findAny();
        }
        finalUser.ifPresent(user -> email.set(Optional.of(user.getEmail())));
        email.get().ifPresent(tempEmail -> {
            userRepository.findByEmail(tempEmail).ifPresent(tempUser -> {
                firstName.set(Optional.of(tempUser.getFirstName()));
                lastName.set(Optional.of(tempUser.getLastName()));
            });
        });
        return new Results(null, null, fetchValue(email), fetchValue(firstName), fetchValue(lastName),
                fetchProjectResultsUrl(project, finalUser),
                project.getCreatorResultsState(),
                null,
                fetchValue(new AtomicReference<>(finalUser), user -> user.getProjectState()));
    }

    private String fetchProjectResultsUrl(@NotNull de.samply.db.model.Project project, Optional<ProjectBridgeheadUser> finalUser) {
        return (finalUser.isPresent() && finalUser.get().getProjectState() == UserProjectState.ACCEPTED) ? project.getResultsUrl() : ProjectManagerConst.NOT_AUTHORIZED;
    }

    public Results fetchResults(@NotNull de.samply.db.model.ProjectBridgehead projectBridgehead) {
        Optional<BridgeheadAdminUser> bridgeheadAdmin = bridgeheadAdminUserRepository.findByBridgehead(projectBridgehead.getBridgehead()).stream().findAny();
        AtomicReference<Optional<de.samply.db.model.User>> user = new AtomicReference<>(Optional.empty());
        bridgeheadAdmin.ifPresent(tempUser -> user.set(userRepository.findByEmail(tempUser.getEmail())));
        AtomicReference<Optional<String>> humanReadableBridghead = new AtomicReference<>(bridgeheadConfiguration.getHumanReadable(projectBridgehead.getBridgehead()));
        return new Results(projectBridgehead.getBridgehead(),
                fetchValue(humanReadableBridghead),
                fetchValue(user, u -> u.getEmail()),
                fetchValue(user, u -> u.getFirstName()),
                fetchValue(user, u -> u.getLastName()),
                fetchProjectBridgeheadResults(projectBridgehead),
                projectBridgehead.getCreatorResultsState(),
                projectBridgehead.getState(),
                null
        );
    }

    private String fetchProjectBridgeheadResults(@NotNull de.samply.db.model.ProjectBridgehead projectBridgehead) {
        return (projectBridgehead.getState() == ProjectBridgeheadState.ACCEPTED) ? projectBridgehead.getResultsUrl() : ProjectManagerConst.NOT_AUTHORIZED;
    }

    private <O> O fetchValue(AtomicReference<Optional<O>> value) {
        return value.get().isPresent() ? value.get().get() : null;
    }

    private <I,O> O fetchValue(AtomicReference<Optional<I>> value, Function<I, O> function) {
        return value.get().isPresent() ? function.apply(value.get().get()) : null;
    }

}
