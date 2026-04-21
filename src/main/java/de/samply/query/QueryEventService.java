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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    public void onQueryChanged(QueryChangedEvent event) {
        projectBridgeheadService.handleChange(event.query());
    }

}
