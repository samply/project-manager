package de.samply.project.event;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.Query;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.db.repository.QueryRepository;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.project.ProjectType;
import de.samply.project.state.ProjectState;
import de.samply.security.SessionUser;
import de.samply.user.UserService;
import de.samply.utils.LogUtils;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.ScopeNotActiveException;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

@Service
public class ProjectEventService implements ProjectEventActions {

    private final NotificationService notificationService;
    private final ProjectRepository projectRepository;
    private final QueryRepository queryRepository;
    private final StateMachineFactory<ProjectState, ProjectEvent> projectStateMachineFactory;
    private final LogUtils logUtils;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final SessionUser sessionUser;
    private final int projectExpirationTimeInDays;
    private final UserService userService;


    public ProjectEventService(NotificationService notificationService,
                               ProjectRepository projectRepository,
                               QueryRepository queryRepository,
                               StateMachineFactory<ProjectState, ProjectEvent> projectStateMachineFactory,
                               LogUtils logUtils,
                               ProjectBridgeheadRepository projectBridgeheadRepository,
                               SessionUser sessionUser,
                               @Value(ProjectManagerConst.PROJECT_DEFAULT_EXPIRATION_TIME_IN_DAYS_SV) int projectExpirationTimeInDays,
                               UserService userService) {
        this.notificationService = notificationService;
        this.projectRepository = projectRepository;
        this.queryRepository = queryRepository;
        this.projectStateMachineFactory = projectStateMachineFactory;
        this.logUtils = logUtils;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.sessionUser = sessionUser;
        this.projectExpirationTimeInDays = projectExpirationTimeInDays;
        this.userService = userService;
    }

    public void loadProject(String projectCode, Consumer<StateMachine<ProjectState, ProjectEvent>> stateMachineConsumer) {
        Optional<Project> project = this.projectRepository.findByCode(projectCode);
        if (project.isPresent()) {
            StateMachine<ProjectState, ProjectEvent> stateMachine = this.projectStateMachineFactory.getStateMachine(project.get().getStateMachineKey());
            stateMachine.stopReactively().subscribe(null, logUtils::logError,
                    () -> stateMachine.getStateMachineAccessor().doWithAllRegions(stateMachineAccess -> {
                        stateMachineAccess.addStateMachineInterceptor(new StateMachineInterceptorAdapter<>() {
                            @Override
                            public void postStateChange(State<ProjectState, ProjectEvent> state, Message<ProjectEvent> message, Transition<ProjectState, ProjectEvent> transition, StateMachine<ProjectState, ProjectEvent> stateMachine, StateMachine<ProjectState, ProjectEvent> rootStateMachine) {
                                project.get().setState(state.getId());
                                saveProject(project.get());
                            }
                        });
                        stateMachineAccess.resetStateMachineReactively(new DefaultStateMachineContext<>(project.get().getState(), null, null, null))
                                .subscribe(null, logUtils::logError,
                                        () -> stateMachine.startReactively().subscribe(null, logUtils::logError,
                                                () -> stateMachineConsumer.accept(stateMachine)));
                    }));
        }
    }

    private Project saveProject(@NotNull Project project) {
        project.setModifiedAt(Instant.now());
        projectRepository.save(project);
        return project;
    }

    private void changeEvent(String projectCode, ProjectEvent projectEvent) throws ProjectEventActionsException {
        changeEvent(projectCode, projectEvent, Optional.empty());
    }

    private void changeEvent(String projectCode, ProjectEvent projectEvent, Optional<Consumer<Project>> consumerAfterSuccessfulChangeEvent) throws ProjectEventActionsException {
        try {
            changeEventWithoutExceptionHandling(projectCode, projectEvent, consumerAfterSuccessfulChangeEvent);
        } catch (Exception e) {
            throw new ProjectEventActionsException(e);
        }
    }

    private void changeEventWithoutExceptionHandling(String projectCode, ProjectEvent projectEvent, Optional<Consumer<Project>> consumerAfterSuccessfulChangeEvent) {
        loadProject(projectCode, stateMachine -> {
            Message<ProjectEvent> createEventMessage = MessageBuilder.withPayload(projectEvent).build();
            stateMachine.sendEvent(Mono.just(createEventMessage)).subscribe(null, logUtils::logError, () -> {
                Optional<Project> project = this.projectRepository.findByCode(projectCode);
                if (project.isPresent()) {
                    project.get().setState(stateMachine.getState().getId());
                    project.get().setModifiedAt(Instant.now());
                    saveProject(project.get());
                    this.notificationService.createNotification(projectCode, null, fetchSessionUserEmailIfSessionIsActive(),
                            OperationType.CHANGE_PROJECT_STATE, projectEvent + " project", null, null);
                    consumerAfterSuccessfulChangeEvent.ifPresent(projectConsumer ->
                            projectConsumer.accept(project.get()));
                }
            });
        });
    }

    private String fetchSessionUserEmailIfSessionIsActive() {
        try {
            return sessionUser.getEmail();
        } catch (ScopeNotActiveException e) {
            return null;
        }
    }

    @Override
    public String draft(String[] bridgeheads, String queryCode) throws ProjectEventActionsException {
        try {
            return draftWithoutExceptionHandling(bridgeheads, queryCode);
        } catch (Exception e) {
            throw new ProjectEventActionsException(e);
        }
    }

    private String draftWithoutExceptionHandling(@NotNull String[] bridgeheads, @NotNull String queryCode) throws ProjectEventActionsException {
        Optional<Query> queryOptional = this.queryRepository.findByCode(queryCode);
        if (queryOptional.isEmpty()) {
            throw new ProjectEventActionsException("Query not found");
        }
        String projectCode = generateProjectCode();
        createProjectAsDraft(
                projectCode,
                project -> Arrays.stream(bridgeheads).forEach(bridgehead -> createProjectBridgehead(bridgehead, project)),
                queryOptional.get());
        return projectCode;
    }

    private String generateProjectCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, ProjectManagerConst.PROJECT_CODE_SIZE);
    }


    private void createProjectAsDraft(String projectCode, Consumer<Project> projectConsumer, Query query) {
        Project project = new Project();
        project.setCode(projectCode);
        project.setCreatorEmail(sessionUser.getEmail());
        project.setCreatedAt(Instant.now());
        project.setExpiresAt(createExpirationDate());
        project.setModifiedAt(Instant.now());
        project.setStateMachineKey(UUID.randomUUID().toString().replace("-", ""));
        project.setQuery(query);
        StateMachine<ProjectState, ProjectEvent> stateMachine =
                this.projectStateMachineFactory.getStateMachine(project.getStateMachineKey());
        stateMachine.startReactively().subscribe(null, logUtils::logError, () -> {
            project.setState(stateMachine.getState().getId());
            projectConsumer.accept(saveProject(project));
            userService.addCreatorIfNotExists();
            this.notificationService.createNotification(projectCode, null, sessionUser.getEmail(),
                    OperationType.CHANGE_PROJECT_STATE, "Design project", null, null);
        });
    }

    private LocalDate createExpirationDate() {
        return LocalDate.now().plusDays(projectExpirationTimeInDays);
    }

    private void createProjectBridgehead(String bridgehead, Project project) {
        ProjectBridgehead projectBridgehead = new ProjectBridgehead();
        projectBridgehead.setBridgehead(bridgehead.toLowerCase());
        projectBridgehead.setProject(project);
        this.projectBridgeheadRepository.save(projectBridgehead);
    }

    @Override
    public void create(String projectCode) throws ProjectEventActionsException {
        changeEvent(projectCode, ProjectEvent.CREATE);
    }

    @Override
    public void accept(String projectCode) throws ProjectEventActionsException {
        changeEvent(projectCode, ProjectEvent.ACCEPT);
    }

    @Override
    public void reject(String projectCode) throws ProjectEventActionsException {
        changeEvent(projectCode, ProjectEvent.REJECT);
    }

    @Override
    public void archive(String projectCode) throws ProjectEventActionsException {
        changeEvent(projectCode, ProjectEvent.ARCHIVE, Optional.of(project -> {
            project.setArchivedAt(project.getModifiedAt());
            saveProject(project);
        }));
    }

    @Override
    public void startDevelopStage(String projectCode) throws ProjectEventActionsException {
        changeEvent(projectCode, ProjectEvent.START_DEVELOP);
    }

    @Override
    public void startPilotStage(String projectCode) throws ProjectEventActionsException {
        changeEvent(projectCode, ProjectEvent.START_PILOT);
    }

    @Override
    public void startFinalStage(String projectCode) throws ProjectEventActionsException {
        changeEvent(projectCode, ProjectEvent.START_FINAL);
    }

    @Override
    public void finish(String projectCode) throws ProjectEventActionsException {
        changeEvent(projectCode, ProjectEvent.FINISH);
    }

    public ProjectState[] fetchAllProjectEvents(Optional<String> projectCode) {
        return projectCode
                .flatMap(projectRepository::findByCode)
                .map(project -> statesFor(project.fetchProjectTypes()))
                .orElseGet(ProjectState::values);
    }

    private ProjectState[] statesFor(Set<ProjectType> types) {

        if (types == null || types.isEmpty()) {
            return ProjectState.values();
        }

        // Convert each type to a Set for intersection
        Set<ProjectState> intersection = types.stream()
                .map(this::statesForSingleType)
                .map(Set::of) // convert array to Set
                .reduce((set1, set2) -> {
                    Set<ProjectState> result = new HashSet<>(set1);
                    result.retainAll(set2);
                    return result;
                })
                .orElse(Set.of());

        // Preserve enum declaration order
        return Arrays.stream(ProjectState.values())
                .filter(intersection::contains)
                .toArray(ProjectState[]::new);
    }

    private ProjectState[] statesForSingleType(ProjectType type) {
        return switch (type) {
            case EXPORT -> EXPORT_PROJECT_STATES;
            case SAMPLES -> SAMPLES_PROJECT_STATES;
            default -> ProjectState.values();
        };
    }

    private static final ProjectState[] EXPORT_PROJECT_STATES = {
            ProjectState.DRAFT,
            ProjectState.REVIEW,
            ProjectState.APPROVAL,
            ProjectState.FINAL,
            ProjectState.FINISHED
    };

    private static final ProjectState[] SAMPLES_PROJECT_STATES = {
            ProjectState.DRAFT,
            ProjectState.REVIEW,
            ProjectState.APPROVAL,
            ProjectState.FINISHED
    };

}
