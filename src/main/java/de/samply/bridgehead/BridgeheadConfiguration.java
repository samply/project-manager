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

    @Data
    public static class BridgeheadConfig {
        private String explorerCode;
        private String exporterUrl;
        private String exporterApiKey;
    }

    public boolean isRegisteredBridgehead(String bridgehead) {
        return config.keySet().contains(bridgehead);
    }

    public String getExporterUrl(String bridgehead) {
        return config.get(bridgehead).exporterUrl;
    }

    public String getExporterApiKey(String bridgehead) {
        return config.get(bridgehead).exporterApiKey;
    }

    public Optional<String> getBridgehead(String explorerCode) {
        for (String bridgehead : config.keySet()) {
            BridgeheadConfig bridgeheadConfig = config.get(bridgehead);
            if (bridgeheadConfig.explorerCode.equals(explorerCode)) {
                return Optional.of(bridgehead);
            }
        }
        return Optional.empty();
    }


}
