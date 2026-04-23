package de.samply.query;

import de.samply.project.ProjectBridgeheadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class QueryEventService {

    private final ProjectBridgeheadService projectBridgeheadService;

    public QueryEventService(ProjectBridgeheadService projectBridgeheadService) {
        this.projectBridgeheadService = projectBridgeheadService;
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

}
