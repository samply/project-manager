package de.samply.project;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProjectRunner implements ApplicationRunner {

    private final StateMachineFactory<ProjectState, ProjectEvent> projectStateMachineFactory;

    public ProjectRunner(StateMachineFactory<ProjectState, ProjectEvent> projectStateMachineFactory) {
        this.projectStateMachineFactory = projectStateMachineFactory;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        this.projectStateMachineFactory.getStateMachine(UUID.randomUUID().toString());
    }

}
