package de.samply.exporter;

import de.samply.annotations.ConditionalOnModule;
import de.samply.annotations.ConditionalOnModuleTest;
import de.samply.app.ProjectManagerConst;
import de.samply.modules.OptionalModule;
import de.samply.project.SendQueryToBridgeheadEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Runs the exporter job a few extra times right after a query was scheduled for a bridgehead, so that it is sent without
 * waiting for the next cron run. Part of the optional module EXPORTER: when it is disabled, nobody listens to the event.
 */
@Slf4j
@Component
@ConditionalOnModule(OptionalModule.EXPORTER)
// Also in test mode: it moves the queries through their states with TestExporterService
@ConditionalOnModuleTest(OptionalModule.EXPORTER)
public class ExporterJobTrigger {

    private final ExporterJob exporterJob;
    private final TaskScheduler taskScheduler;
    private final int sleepTime;
    private final int maxNumberOfRetries;

    public ExporterJobTrigger(
            ExporterJob exporterJob,
            TaskScheduler taskScheduler,
            @Value(ProjectManagerConst.TIME_BETWEEN_CHECK_EXPORTS_IN_SECONDS_SV) int sleepTime,
            @Value(ProjectManagerConst.MAX_NUMBER_OF_RETRIES_BETWEEN_CHECK_EXPORTS_SV) int maxNumberOfRetries) {
        this.exporterJob = exporterJob;
        this.taskScheduler = taskScheduler;
        this.sleepTime = sleepTime;
        this.maxNumberOfRetries = maxNumberOfRetries;
    }

    @EventListener
    public void onQuerySentToBridgehead(@SuppressWarnings("unused") SendQueryToBridgeheadEvent event) {
        log.debug("Query scheduled for a bridgehead: running the exporter job {} times", maxNumberOfRetries);
        for (int i = 0; i < maxNumberOfRetries; i++) {
            Instant when = Instant.now().plusSeconds((long) sleepTime * i);
            taskScheduler.schedule(exporterJob::checkExports, when);
        }
    }

}
