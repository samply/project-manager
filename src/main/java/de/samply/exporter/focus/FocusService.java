package de.samply.exporter.focus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class FocusService {

    private final String projectManagerId;
    private final String ttl;
    private final FailureStrategy failureStrategy;
    private final BridgeheadConfiguration bridgeheadConfiguration;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public FocusService(
            @Value(ProjectManagerConst.FOCUS_PROJECT_MANAGER_ID_SV) String projectManagerId,
            @Value(ProjectManagerConst.FOCUS_TTL_SV) String ttl,
            @Value(ProjectManagerConst.FOCUS_FAILURE_STRATEGY_BACKOFF_IN_MILLISECONDS_SV) int retriesBackoff,
            @Value(ProjectManagerConst.FOCUS_FAILURE_STRATEGY_MAX_TRIES_SV) int maxRetries,
            BridgeheadConfiguration bridgeheadConfiguration) {
        this.projectManagerId = projectManagerId;
        this.ttl = ttl;
        this.failureStrategy = createFailureStrategy(retriesBackoff, maxRetries);
        this.bridgeheadConfiguration = bridgeheadConfiguration;
    }

    public FocusQuery generateFocusQuery(String exporterQuery, TaskType taskType, String bridgehead) throws FocusServiceException {
        FocusQuery focusQuery = new FocusQuery();
        focusQuery.setId(generateId());
        focusQuery.setBody(exporterQuery);
        focusQuery.setFrom(projectManagerId);
        focusQuery.setTo(fetchExporterFocusIds(bridgehead));
        focusQuery.setTtl(ttl);
        focusQuery.setMetadata(createFocusQueryMetadata(taskType));
        focusQuery.setFailureStrategy(failureStrategy);
        return focusQuery;
    }

    public String generateId() {
        return UUID.randomUUID().toString();
    }

    private String[] fetchExporterFocusIds(String bridgehead) throws FocusServiceException {
        String focusId = bridgeheadConfiguration.getFocusId(bridgehead);
        if (!StringUtils.hasText(focusId)) {
            throw new FocusServiceException("Focus ID for bridgehead " + bridgehead + " not found");
        }
        return new String[]{focusId};
    }

    private FocusQueryMetadata createFocusQueryMetadata(TaskType taskType) {
        FocusQueryMetadata focusQueryMetadata = new FocusQueryMetadata();
        focusQueryMetadata.setProject(ProjectManagerConst.FOCUS_METADATA_PROJECT);
        focusQueryMetadata.setTaskType(taskType);
        return focusQueryMetadata;
    }

    private FailureStrategy createFailureStrategy(int backoff, int maxRetries) {
        FailureStrategy failureStrategy = new FailureStrategy();
        RetryStrategy retryStrategy = new RetryStrategy();
        retryStrategy.setMaxTries(maxRetries);
        retryStrategy.setBackoffInMilliseconds(backoff);
        failureStrategy.setRetryStrategy(retryStrategy);
        return failureStrategy;
    }

    public FocusQuery[] deserializeFocusResponse(String focusResponse) throws FocusServiceException {
        try {
            return objectMapper.readValue(focusResponse, FocusQuery[].class);
        } catch (JsonProcessingException e) {
            throw new FocusServiceException(e);
        }
    }

    public String serializeFocusQuery(FocusQuery focusQuery) throws FocusServiceException {
        try {
            FocusQuery[] focusQueries = {focusQuery};
            return objectMapper.writeValueAsString(focusQueries);
        } catch (JsonProcessingException e) {
            throw new FocusServiceException(e);
        }
    }

}
