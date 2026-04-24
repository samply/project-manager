package de.samply.query;

import de.samply.app.ProjectManagerConst;
import de.samply.exporter.ExporterJob;
import de.samply.project.ProjectBridgeheadService;
import de.samply.project.SendQueryToBridgeheadEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class QueryEventService {

    private final ProjectBridgeheadService projectBridgeheadService;
    private final ExporterJob exporterJob;
    private final int sleepTime;
    private final int maxNumberOfRetries;


    public QueryEventService(
            ProjectBridgeheadService projectBridgeheadService,
            ExporterJob exporterJob,
            @Value(ProjectManagerConst.TIME_BETWEEN_CHECK_EXPORTS_IN_SECONDS_SV) int sleepTime,
            @Value(ProjectManagerConst.MAX_NUMBER_OF_RETRIES_BETWEEN_CHECK_EXPORTS_SV) int maxNumberOfRetries) {
        this.projectBridgeheadService = projectBridgeheadService;
        this.exporterJob = exporterJob;
        this.sleepTime = sleepTime;
        this.maxNumberOfRetries = maxNumberOfRetries;
    }

    /**
     * Listens for query changes after the modifying transaction has successfully committed,
     * ensuring we only react to the persisted state. AFTER_COMMIT (not AFTER_COMPLETION) guarantees
     * we don't run on rollback. No @Transactional here — handleChange opens its own transaction.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQueryChanged(QueryChangedEvent event) {
        projectBridgeheadService.handleChange(event.queryId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQuerySentToBridgehead(@SuppressWarnings("unused") SendQueryToBridgeheadEvent event) throws InterruptedException {
        // Execute twice to have enough time to check if the query has arrived to the bridgehead
        for (int i = 0; i < maxNumberOfRetries; i++) {
            exporterJob.checkExports();
            Thread.sleep(this.sleepTime * 1000L);
        }
    }


}
