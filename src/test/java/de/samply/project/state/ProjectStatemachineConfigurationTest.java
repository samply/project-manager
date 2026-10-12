package de.samply.project.state;

import de.samply.project.event.ProjectEvent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(ProjectStatemachineConfiguration.class)
class ProjectStatemachineConfigurationTest {

    @Autowired
    private StateMachineFactory<ProjectState, ProjectEvent> stateMachineFactory;

    // Target "-": the event is not accepted in the source state
    @ParameterizedTest
    @CsvSource({
            "REVIEW, ACCEPT, CENTRAL_APPROVAL",
            "CENTRAL_APPROVAL, START_LOCAL_APPROVAL, LOCAL_APPROVAL",
            "CENTRAL_APPROVAL, START_FINAL, FINAL",
            "CENTRAL_APPROVAL, START_DEVELOP, DEVELOP",
            "CENTRAL_APPROVAL, BACK_TO_REVIEW, REVIEW",
            "LOCAL_APPROVAL, START_FINAL, FINAL",
            "LOCAL_APPROVAL, START_DEVELOP, DEVELOP",
            "LOCAL_APPROVAL, REJECT, REJECTED",
            "LOCAL_APPROVAL, ARCHIVE, ARCHIVED",
            "ARCHIVED, ACCEPT, CENTRAL_APPROVAL",
            "LOCAL_APPROVAL, BACK_TO_REVIEW, -",
            "LOCAL_APPROVAL, START_PILOT, -",
            "REVIEW, START_LOCAL_APPROVAL, -",
            "FINAL, START_LOCAL_APPROVAL, -"
    })
    void transitions(ProjectState source, ProjectEvent event, String target) {
        StateMachine<ProjectState, ProjectEvent> stateMachine = stateMachineFactory.getStateMachine("test");
        stateMachine.getStateMachineAccessor().doWithAllRegions(access -> access
                .resetStateMachineReactively(new DefaultStateMachineContext<>(source, null, null, null)).block());
        stateMachine.startReactively().block();

        stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(event).build())).blockLast();

        assertThat(stateMachine.getState().getId())
                .isEqualTo(target.equals("-") ? source : ProjectState.valueOf(target));
    }

}
