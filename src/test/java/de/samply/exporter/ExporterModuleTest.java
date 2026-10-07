package de.samply.exporter;

import de.samply.annotations.ModuleComponent;
import de.samply.modules.OptionalModule;
import de.samply.project.SendQueryToBridgeheadEvent;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ExporterModuleTest {

    @Test
    void jobAndTriggerBelongToTheExporterModule() {
        assertThat(ExporterJob.class.getAnnotation(ModuleComponent.class).value()).isEqualTo(OptionalModule.EXPORTER);
        assertThat(ExporterJobTrigger.class.getAnnotation(ModuleComponent.class).value()).isEqualTo(OptionalModule.EXPORTER);
    }

    @Test
    void triggerRunsTheJobSeveralTimesAfterAQueryWasScheduled() {
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        ExporterJobTrigger trigger = new ExporterJobTrigger(mock(ExporterJob.class), taskScheduler, 5, 3);

        trigger.onQuerySentToBridgehead(new SendQueryToBridgeheadEvent());

        verify(taskScheduler, times(3)).schedule(any(Runnable.class), any(Instant.class));
    }

}
