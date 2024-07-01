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
public class BeamService {

    private final String projectManagerId;
    private final String ttl;
    private final FailureStrategy failureStrategy;
    private final BridgeheadConfiguration bridgeheadConfiguration;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public BeamService(
            @Value(ProjectManagerConst.BEAM_PROJECT_MANAGER_ID_SV) String projectManagerId,
            @Value(ProjectManagerConst.BEAM_TTL_SV) String ttl,
            @Value(ProjectManagerConst.BEAM_FAILURE_STRATEGY_BACKOFF_IN_MILLISECONDS_SV) int retriesBackoff,
            @Value(ProjectManagerConst.BEAM_FAILURE_STRATEGY_MAX_TRIES_SV) int maxRetries,
            BridgeheadConfiguration bridgeheadConfiguration) {
        this.projectManagerId = projectManagerId;
        this.ttl = ttl;
        this.failureStrategy = createFailureStrategy(retriesBackoff, maxRetries);
        this.bridgeheadConfiguration = bridgeheadConfiguration;
    }

    public BeamRequest generateFocusBeamRequest(String exporterQuery, TaskType taskType, String bridgehead) throws BeamServiceException {
        BeamRequest beamRequest = new BeamRequest();
        beamRequest.setId(generateId());
        beamRequest.setBody(exporterQuery);
        beamRequest.setFrom(projectManagerId);
        beamRequest.setTo(new String[]{fetchFocusBeamId(bridgehead)});
        beamRequest.setTtl(ttl);
        beamRequest.setMetadata(createFocusQueryMetadata(taskType));
        beamRequest.setFailureStrategy(failureStrategy);
        return beamRequest;
    }

    public String generateId() {
        return UUID.randomUUID().toString();
    }

    private String fetchFocusBeamId(String bridgehead) throws BeamServiceException {
        String focusId = bridgeheadConfiguration.getFocusBeamId(bridgehead);
        if (!StringUtils.hasText(focusId)) {
            throw new BeamServiceException("Focus Beam ID for bridgehead " + bridgehead + " not found");
        }
        return focusId;
    }

    private String fetchFileDispatcherId(String bridgehead) throws BeamServiceException {
        String focusId = bridgeheadConfiguration.getFileDispatcherBeamId(bridgehead);
        if (!StringUtils.hasText(focusId)) {
            throw new BeamServiceException("File Dispatcher Beam ID for bridgehead " + bridgehead + " not found");
        }
        return focusId;
    }

    private BeamRequestMetadata createFocusQueryMetadata(TaskType taskType) {
        BeamRequestMetadata beamRequestMetadata = new BeamRequestMetadata();
        beamRequestMetadata.setProject(ProjectManagerConst.BEAM_FOCUS_METADATA_PROJECT);
        beamRequestMetadata.setTaskType(taskType);
        return beamRequestMetadata;
    }

    private FailureStrategy createFailureStrategy(int backoff, int maxRetries) {
        FailureStrategy failureStrategy = new FailureStrategy();
        RetryStrategy retryStrategy = new RetryStrategy();
        retryStrategy.setMaxTries(maxRetries);
        retryStrategy.setBackoffInMilliseconds(backoff);
        failureStrategy.setRetryStrategy(retryStrategy);
        return failureStrategy;
    }

    public BeamRequest[] deserializeFocusResponse(String focusResponse) throws BeamServiceException {
        try {
            return objectMapper.readValue(focusResponse, BeamRequest[].class);
        } catch (JsonProcessingException e) {
            throw new BeamServiceException(e);
        }
    }

    public String serializeFocusQuery(BeamRequest beamRequest) throws BeamServiceException {
        try {
            BeamRequest[] focusQueries = {beamRequest};
            return objectMapper.writeValueAsString(focusQueries);
        } catch (JsonProcessingException e) {
            throw new BeamServiceException(e);
        }
    }

    public BeamRequest generateExporterFileTransferBeamRequest(String bridgehead, String exporterExecutionId, String targetBeamId) {
        BeamRequest beamRequest = new BeamRequest();
        beamRequest.setId(generateId());
        beamRequest.setBody(createExporterFileTransferBody(exporterExecutionId, targetBeamId));
        beamRequest.setFrom(projectManagerId);
        beamRequest.setTo(new String[]{fetchFileDispatcherId(bridgehead)});
        beamRequest.setTtl(ttl);
        beamRequest.setFailureStrategy(failureStrategy);
        return beamRequest;
    }

    private String createExporterFileTransferBody(String exporterExecutionId, String targetBeamId) {
        try {
            return createExporterFileTransferBodyWithoutExceptionHandling(exporterExecutionId, targetBeamId);
        } catch (JsonProcessingException e) {
            throw new BeamServiceException(e);
        }
    }

    private String createExporterFileTransferBodyWithoutExceptionHandling(String exporterExecutionId, String targetBeamId) throws JsonProcessingException {
        FileTransferBody body = new FileTransferBody();
        body.setExporterUrl(ProjectManagerConst.EXPORTER_FETCH_QUERY_EXECUTION_URL_PATH + exporterExecutionId);
        body.setBeamReceiver(targetBeamId);
        return objectMapper.writeValueAsString(body);
    }

}
