package de.samply.exporter;

import de.samply.annotations.ConditionalOnModule;
import de.samply.annotations.ConditionalOnModuleDisabled;
import de.samply.modules.OptionalModule;
import de.samply.project.SendQueryToBridgeheadEvent;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ExporterModuleTest {

    @Test
    void jobAndTriggerBelongToTheExporterModule() {
        assertThat(ExporterJob.class.getAnnotation(ConditionalOnModule.class).value()).isEqualTo(OptionalModule.EXPORTER);
        assertThat(ExporterJobTrigger.class.getAnnotation(ConditionalOnModule.class).value()).isEqualTo(OptionalModule.EXPORTER);
    }

    @Test
    void beamImplementationBelongsToTheExporterModule() {
        assertThat(BeamExporterService.class.getAnnotation(ConditionalOnModule.class).value())
                .isEqualTo(OptionalModule.EXPORTER);
        assertThat(ExporterQueryLabelTemplate.class.getAnnotation(ConditionalOnModule.class).value())
                .isEqualTo(OptionalModule.EXPORTER);
    }

    @Test
    void standInReplacesItWhenTheModuleIsDisabled() {
        assertThat(DisabledExporterService.class.getAnnotation(ConditionalOnModuleDisabled.class).value())
                .isEqualTo(OptionalModule.EXPORTER);
    }

    @Test
    void standInHasNoTemplatesAndNoTransfer() {
        DisabledExporterService standIn = new DisabledExporterService();

        assertThat(standIn.getExporterTemplates()).isEmpty();
        assertThat(standIn.isExportFileTransferredToResearchEnvironment("REQ-2026-0001", "site-a")).isFalse();
        assertThatThrownBy(() -> standIn.transferFileToResearchEnvironment("REQ-2026-0001", "site-a"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void triggerRunsTheJobSeveralTimesAfterAQueryWasScheduled() {
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        ExporterJobTrigger trigger = new ExporterJobTrigger(mock(ExporterJob.class), taskScheduler, 5, 3);

        trigger.onQuerySentToBridgehead(new SendQueryToBridgeheadEvent());

        verify(taskScheduler, times(3)).schedule(any(Runnable.class), any(Instant.class));
    }

}
