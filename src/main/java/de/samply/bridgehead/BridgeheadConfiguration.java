package de.samply.bridgehead;

import de.samply.app.ProjectManagerConst;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Configuration
@ConfigurationProperties(prefix = ProjectManagerConst.REGISTERED_BRIDGEHEADS)
@Data
public class BridgeheadConfiguration {

    private Map<String, BridgeheadConfig> config = new HashMap<>();
    private Map<String, String> explorerCodeBridgeheadMap = new HashMap<>();

    @Data
    public static class BridgeheadConfig {
        private String explorerCode;
        private String focusId;
        private String humanReadable;
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

    public Optional<String> getBridgehead(String explorerCode) {
        String bridgehead = explorerCodeBridgeheadMap.get(explorerCode);
        if (bridgehead == null) {
            bridgehead = fetchBridgehead(explorerCode);
            explorerCodeBridgeheadMap.put(explorerCode, bridgehead);
        }
        return Optional.ofNullable(bridgehead);
    }

    private String fetchBridgehead(String explorerCode) {
        for (String bridgehead : config.keySet()) {
            BridgeheadConfig bridgeheadConfig = config.get(bridgehead);
            if (bridgeheadConfig.explorerCode.equals(explorerCode)) {
                return bridgehead;
            }
        }
        return null;
    }


}
