package de.samply.project.event;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import de.samply.security.SessionUser;
import de.samply.utils.LogUtils;
import jakarta.validation.constraints.NotNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class ProjectEventService implements ProjectEventActions {

    private final ProjectRepository projectRepository;
    private final StateMachineFactory<ProjectState, ProjectEvent> projectStateMachineFactory;
    private final LogUtils logUtils;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final SessionUser sessionUser;


    public ProjectEventService(ProjectRepository projectRepository,
                               StateMachineFactory<ProjectState, ProjectEvent> projectStateMachineFactory,
                               LogUtils logUtils,
                               ProjectBridgeheadRepository projectBridgeheadRepository,
                               SessionUser sessionUser) {
        this.projectRepository = projectRepository;
        this.projectStateMachineFactory = projectStateMachineFactory;
        this.logUtils = logUtils;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.sessionUser = sessionUser;
    }

    public void loadProject(String projectName, Consumer<StateMachine<ProjectState, ProjectEvent>> stateMachineConsumer) {
        Optional<Project> project = this.projectRepository.findByName(projectName);
        if (project.isPresent()) {
            StateMachine<ProjectState, ProjectEvent> stateMachine = this.projectStateMachineFactory.getStateMachine(project.get().getStateMachineKey());
            stateMachine.stopReactively().subscribe(null, logUtils::logError,
                    () -> stateMachine.getStateMachineAccessor().doWithAllRegions(stateMachineAccess -> {
                        stateMachineAccess.addStateMachineInterceptor(new StateMachineInterceptorAdapter<>() {
                            @Override
                            public void postStateChange(State<ProjectState, ProjectEvent> state, Message<ProjectEvent> message, Transition<ProjectState, ProjectEvent> transition, StateMachine<ProjectState, ProjectEvent> stateMachine, StateMachine<ProjectState, ProjectEvent> rootStateMachine) {
                                project.get().setState(state.getId());
                                projectRepository.save(project.get());
                            }
                        });
                        stateMachineAccess.resetStateMachineReactively(new DefaultStateMachineContext<>(project.get().getState(), null, null, null))
                                .subscribe(null, logUtils::logError,
                                        () -> stateMachine.startReactively().subscribe(null, logUtils::logError,
                                                () -> stateMachineConsumer.accept(stateMachine)));
                    }));
        }
    }

    private void changeEvent(String projectName, ProjectEvent projectEvent) throws ProjectEventActionsException {
        try {
            changeEventWithoutExceptionHandling(projectName, projectEvent);
        } catch (Exception e) {
            throw new ProjectEventActionsException(e);
        }
    }

    private void changeEventWithoutExceptionHandling(String projectName, ProjectEvent projectEvent) {
        loadProject(projectName, stateMachine -> {
            Message<ProjectEvent> createEventMessage = MessageBuilder.withPayload(projectEvent).build();
            stateMachine.sendEvent(Mono.just(createEventMessage)).subscribe(null, logUtils::logError, () -> {
                Optional<Project> project = this.projectRepository.findByName(projectName);
                if (project.isPresent()) {
                    project.get().setState(stateMachine.getState().getId());
                    this.projectRepository.save(project.get());
                }
            });
        });
    }

    @Override
    public void draft(String projectName, String[] bridgeheads) throws ProjectEventActionsException {
        try {
            draftWithoutExceptionHandling(projectName, bridgeheads);
        } catch (Exception e) {
            throw new ProjectEventActionsException(e);
        }
    }

    private void draftWithoutExceptionHandling(@NotNull String projectName, @NotNull String[] bridgeheads) {
        createProjectAsDraft(projectName, project -> Arrays.stream(bridgeheads).forEach(bridgehead -> createProjectBridgehead(bridgehead, project)));
    }


    private void createProjectAsDraft(String projectName, Consumer<Project> projectConsumer) {
        Project project = new Project();
        project.setName(projectName);
        project.setCreatorEmail(sessionUser.getEmail());
        project.setCreatedAt(LocalDate.now());
        project.setStateMachineKey(UUID.randomUUID().toString());
        StateMachine<ProjectState, ProjectEvent> stateMachine = this.projectStateMachineFactory.getStateMachine(project.getStateMachineKey());
        stateMachine.startReactively().subscribe(null, logUtils::logError, () -> {
            project.setState(stateMachine.getState().getId());
            projectConsumer.accept(this.projectRepository.save(project));
        });
    }

    private ProjectBridgehead createProjectBridgehead(String bridgehead, Project project) {
        ProjectBridgehead projectBridgehead = new ProjectBridgehead();
        projectBridgehead.setBridgehead(bridgehead.toLowerCase());
        projectBridgehead.setProject(project);
        projectBridgehead.setState(ProjectBridgeheadState.CREATED); // TODO: Replace with state machine
        return this.projectBridgeheadRepository.save(projectBridgehead);
    }

    @Override
    public void create(String projectName) throws ProjectEventActionsException {
        changeEvent(projectName, ProjectEvent.CREATE);
    }

    @Override
    public void accept(String projectName) throws ProjectEventActionsException {
        changeEvent(projectName, ProjectEvent.ACCEPT);
    }

    @Override
    public void reject(String projectName) throws ProjectEventActionsException {
        changeEvent(projectName, ProjectEvent.REJECT);
    }

    @Override
    public void archive(String projectName) throws ProjectEventActionsException {
        changeEvent(projectName, ProjectEvent.ARCHIVE);
    }

    @Override
    public void startDevelopStage(String projectName) throws ProjectEventActionsException {
        changeEvent(projectName, ProjectEvent.START_DEVELOP);
    }

    @Override
    public void startPilotStage(String projectName) throws ProjectEventActionsException {
        changeEvent(projectName, ProjectEvent.START_PILOT);
    }

    @Override
    public void startFinalStage(String projectName) throws ProjectEventActionsException {
        changeEvent(projectName, ProjectEvent.START_FINAL);
    }

    @Override
    public void finish(String projectName) throws ProjectEventActionsException {
        changeEvent(projectName, ProjectEvent.FINISH);
    }

}
