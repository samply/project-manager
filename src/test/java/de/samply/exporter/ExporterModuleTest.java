package de.samply.exporter;

import de.samply.annotations.ConditionalOnModule;
import de.samply.annotations.ConditionalOnModuleDisabled;
import de.samply.modules.OptionalModule;
import de.samply.project.SendQueryToBridgeheadEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.ClassUtils;

import java.time.Instant;
import java.util.List;

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

    // The real classes under Spring's own condition evaluation, for each value of ENABLE_EXPORTER
    @ParameterizedTest
    @CsvSource({
            "true, BeamExporterService",
            "test, TestExporterService",
            "false, DisabledExporterService"
    })
    void eachModeCreatesItsExporterService(String mode, String expectedService) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false,
                new MockEnvironment().withProperty("ENABLE_EXPORTER", mode));
        scanner.addIncludeFilter(new AssignableTypeFilter(ExporterService.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(ExporterJob.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(ExporterJobTrigger.class));

        List<String> found = scanner.findCandidateComponents("de.samply.exporter").stream()
                .map(candidate -> ClassUtils.getShortName(candidate.getBeanClassName()))
                .toList();

        // The job and its trigger run in the real and in the test mode
        assertThat(found).containsExactlyInAnyOrderElementsOf("false".equals(mode)
                ? List.of(expectedService) : List.of(expectedService, "ExporterJob", "ExporterJobTrigger"));
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
