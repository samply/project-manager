package de.samply.app;

import de.samply.batch.ActionsBatchRequest;
import de.samply.batch.ActionsBatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectManagerControllerBatchTest {

    @Mock
    ActionsBatchService actionsBatchService;
    @InjectMocks
    ProjectManagerController controller;

    @Test
    void refusesABatchWithTooManyEntriesBeforeRunningAny() {
        when(actionsBatchService.getMaxEntries()).thenReturn(2);
        ActionsBatchRequest entry = new ActionsBatchRequest("A", Map.of());
        var response = controller.fetchActionsBatch(Map.of("a", entry, "b", entry, "c", entry));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        verify(actionsBatchService, never()).fetchActionsBatch(any());
    }
}
