package de.samply.bridgehead;

import de.samply.app.ProjectManagerConst;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = ProjectManagerConst.REGISTERED_BRIDGEHEADS)
@Data
public class BridgeheadConfiguration {

    private Map<String, BridgeheadConfig> config = new HashMap<>();
    private Map<String, String> explorerIdBridgeheadMap = new HashMap<>();
    private Map<String, String> tokenManagerIdBridgeheadMap = new HashMap<>();
    private Map<String, String> focusIdBridgeheadMap = new HashMap<>();


    @Data
    public static class BridgeheadConfig {
        private String explorerId;
        private String focusId;
        private String tokenManagerId;
        private String humanReadable;
    }

    @PostConstruct
    private void initIdBridgeheadMaps() {
        config.forEach((bridgehead, bridgeheadConfig) -> {
            addBridgeheadId(bridgehead, bridgeheadConfig.getExplorerId(), explorerIdBridgeheadMap);
            addBridgeheadId(bridgehead, bridgeheadConfig.getFocusId(), focusIdBridgeheadMap);
            addBridgeheadId(bridgehead, bridgeheadConfig.getTokenManagerId(), tokenManagerIdBridgeheadMap);
        });
    }

    private String fetchBridgehead(String id, Map<String, String> idBridgeheadMap) {
        return idBridgeheadMap.get(id);
    }

    private Optional<String> fetchBridgeheadOptional(String id, Map<String, String> idBridgeheadMap) {
        return Optional.ofNullable(fetchBridgehead(id, idBridgeheadMap));
    }

    private void addBridgeheadId(String bridgehead, String id, Map<String, String> idBridgeheadMap) {
        if (bridgehead != null && id != null) {
            idBridgeheadMap.put(id, bridgehead);
        }
    }

    public boolean isRegisteredBridgehead(String bridgehead) {
        return config.keySet().contains(bridgehead);
    }

    public String getFocusId(String bridgehead) {
        return config.get(bridgehead).getFocusId();
    }

    public String getHumanReadable(String bridgehead) {
        return config.get(bridgehead).getHumanReadable();
    }

    public String getTokenManagerId(String bridgehead) {
        return config.get(bridgehead).getTokenManagerId();
    }

    public Optional<String> getBridgeheadForExplorerId(String explorerId) {
        return fetchBridgeheadOptional(explorerId, explorerIdBridgeheadMap);
    }

    public String fetchBridgeheadForExplorerId(String explorerId) {
        return fetchBridgehead(explorerId, explorerIdBridgeheadMap);
    }

    public Optional<String> getBridgeheadForFocusId(String focusId) {
        return fetchBridgeheadOptional(focusId, focusIdBridgeheadMap);
    }

    public String fetchBridgeheadForFocusId(String focusId) {
        return fetchBridgehead(focusId, focusIdBridgeheadMap);
    }

    public Optional<String> getBridgeheadForTokenManagerId(String tokenManagerId) {
        return fetchBridgeheadOptional(tokenManagerId, tokenManagerIdBridgeheadMap);
    }

    public String fetchBridgeheadForTokenManagerId(String tokenManagerId) {
        return fetchBridgehead(tokenManagerId, tokenManagerIdBridgeheadMap);
    }

    public Set<String> getRegisteredBridgeheads() {
        return config.keySet();
    }


}
