package de.samply.project;

import de.samply.db.model.Project;
import de.samply.db.model.User;
import de.samply.db.repository.ProjectRepository;
import de.samply.utils.LogUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;

@Service
public class ProjectEventService implements ProjectEventActions {

    private final ProjectRepository projectRepository;
    private final StateMachineFactory<ProjectState, ProjectEvent> projectStateMachineFactory;
    private final LogUtils logUtils;


    public ProjectEventService(ProjectRepository projectRepository, StateMachineFactory<ProjectState, ProjectEvent> projectStateMachineFactory, LogUtils logUtils) {
        this.projectRepository = projectRepository;
        this.projectStateMachineFactory = projectStateMachineFactory;
        this.logUtils = logUtils;
    }

    private Optional<StateMachine<ProjectState, ProjectEvent>> loadProject(String projectName) {
        Optional<StateMachine<ProjectState, ProjectEvent>> result = Optional.empty();
        Optional<Project> project = this.projectRepository.findByName(projectName);
        if (project.isPresent()) {
            StateMachine<ProjectState, ProjectEvent> stateMachine = this.projectStateMachineFactory.getStateMachine(project.get().getStateMachineKey());
            result = Optional.of(stateMachine);
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
                                .subscribe(null, logUtils::logError, () -> stateMachine.startReactively());
                    }));
        }
        return result;
    }

    private Flux<StateMachineEventResult<ProjectState, ProjectEvent>> changeEvent(String projectName, ProjectEvent projectEvent) {
        Optional<StateMachine<ProjectState, ProjectEvent>> stateMachineOptional = loadProject(projectName);
        return (stateMachineOptional.isPresent()) ? changeEvent(stateMachineOptional.get(), projectEvent) : Flux.empty();
    }

    private Flux<StateMachineEventResult<ProjectState, ProjectEvent>> changeEvent(StateMachine<ProjectState, ProjectEvent> stateMachine, ProjectEvent projectEvent) {
        Message<ProjectEvent> createEventMessage = MessageBuilder.withPayload(projectEvent).build();
        return stateMachine.sendEvent(Mono.just(createEventMessage));
    }

    @Override
    public Project draft(ProjectParameters projectParameters) throws ProjectEventActionsException {
        Optional<User> userOptional = fetchUser(projectParameters.email());
        if (userOptional.isEmpty()) {
            throw new ProjectEventActionsException("User" + projectParameters.email() + " not found");
        } else {
            Project project = new Project();
            project.setName(projectParameters.projectName());
            project.setCreatedAt(LocalDate.now());
            project.setStateMachineKey(UUID.randomUUID());
            StateMachine<ProjectState, ProjectEvent> stateMachine = this.projectStateMachineFactory.getStateMachine(project.getStateMachineKey());
            stateMachine.startReactively();
            this.projectRepository.save(project);
            createProjectUser(project, userOptional.get());
            createProjectBridgeheads(project, projectParameters.bridgeheads());
            return project;
        }
    }

    private Optional<User> fetchUser(String email) {
        //TODO
        return Optional.empty();
    }

    private void createProjectUser(Project project, User user) {
        //TODO
    }

    private void createProjectBridgeheads(Project project, String[] bridgeheads) {
        Arrays.stream(bridgeheads).forEach(bridgehead ->{
            //TODO
        });

    }

    @Override
    public void create(String projectName) {
        changeEvent(projectName, ProjectEvent.CREATE);
    }

    @Override
    public void accept(String projectName) {
        changeEvent(projectName, ProjectEvent.ACCEPT);
    }

    @Override
    public void reject(String projectName) {
        changeEvent(projectName, ProjectEvent.REJECT);
    }

    @Override
    public void archive(String projectName) {
        changeEvent(projectName, ProjectEvent.ARCHIVE);
    }

    @Override
    public void startDevelopStage(String projectName) {
        changeEvent(projectName, ProjectEvent.START_DEVELOP);
    }

    @Override
    public void startPilotStage(String projectName) {
        changeEvent(projectName, ProjectEvent.START_PILOT);
    }

    @Override
    public void startFinalStage(String projectName) {
        changeEvent(projectName, ProjectEvent.START_FINAL);
    }

    @Override
    public void finish(String projectName) {
        changeEvent(projectName, ProjectEvent.FINISH);
    }

    public Set<ProjectState> fetchNextPossibleProjectStates(String projectName) {
        Set<ProjectState> result = new HashSet<>();
        loadProject(projectName).ifPresent(stateMachine -> {
            stateMachine.getTransitions().forEach(transition -> result.add(transition.getTarget().getId()));
        });
        return result;
    }

}
