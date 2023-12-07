package de.samply.project.state;

import de.samply.project.event.ProjectEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.Arrays;

@Slf4j
@Configuration
@EnableStateMachineFactory
public class ProjectStatemachineConfiguration extends StateMachineConfigurerAdapter<ProjectState, ProjectEvent> {

    @Override
    public void configure(StateMachineConfigurationConfigurer<ProjectState, ProjectEvent> config) throws Exception {
        config.withConfiguration()
                .autoStartup(false)
                .listener(new StateMachineListenerAdapter<>() {
                    @Override
                    public void stateChanged(State<ProjectState, ProjectEvent> from, State<ProjectState, ProjectEvent> to) {
                        super.stateChanged(from, to);
                        log.info("State changed from " + from + " to " + to);
                    }
                });
    }

    @Override
    public void configure(StateMachineStateConfigurer<ProjectState, ProjectEvent> states) throws Exception {
        states.withStates()
                .initial(ProjectState.DRAFT)
                .state(ProjectState.CREATED)
                .state(ProjectState.ACCEPTED)
                .state(ProjectState.DEVELOP)
                .state(ProjectState.PILOT)
                .state(ProjectState.FINAL)
                .state(ProjectState.ARCHIVED)
                .end(ProjectState.REJECTED)
                .end(ProjectState.FINISHED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<ProjectState, ProjectEvent> transitions) throws Exception {
        addRejectTransitions(transitions);
        transitions
                .withExternal().source(ProjectState.DRAFT).target(ProjectState.CREATED).event(ProjectEvent.CREATE).and()
                .withExternal().source(ProjectState.CREATED).target(ProjectState.ACCEPTED).event(ProjectEvent.ACCEPT).and()
                .withExternal().source(ProjectState.ACCEPTED).target(ProjectState.DEVELOP).event(ProjectEvent.START_DEVELOP).and()
                .withExternal().source(ProjectState.DEVELOP).target(ProjectState.PILOT).event(ProjectEvent.START_PILOT).and()
                .withExternal().source(ProjectState.PILOT).target(ProjectState.FINAL).event(ProjectEvent.START_FINAL).and()
                .withExternal().source(ProjectState.FINAL).target(ProjectState.FINISHED).event(ProjectEvent.FINISH).and()
                .withExternal().source(ProjectState.ACCEPTED).target(ProjectState.FINAL).event(ProjectEvent.START_FINAL).and()
                .withExternal().source(ProjectState.ARCHIVED).target(ProjectState.ACCEPTED).event(ProjectEvent.ACCEPT);
    }

    private void addRejectTransitions(StateMachineTransitionConfigurer<ProjectState, ProjectEvent> transitions) throws Exception {
        try {
            ProjectState[] statesToBeRejected = {ProjectState.DRAFT, ProjectState.CREATED, ProjectState.ACCEPTED,
                    ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL, ProjectState.ARCHIVED};
            Arrays.stream(statesToBeRejected).forEach(state -> {
                try {
                    rejectState(transitions, state);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            throw new Exception(e);
        }
    }

    private void rejectState(StateMachineTransitionConfigurer<ProjectState, ProjectEvent> transitions, ProjectState state) throws Exception {
        transitions.withExternal().source(state).target(ProjectState.REJECTED).event(ProjectEvent.REJECT).and();
    }


}
