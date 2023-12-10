package de.samply.bridgehead;

import de.samply.app.ProjectManagerConst;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Getter
public class BridgeheadConfiguration {

    private List<String> bridgeheads;

    public BridgeheadConfiguration(@Value(ProjectManagerConst.REGISTERED_BRIDGEHEADS_SV) List<String> bridgeheads) {
        this.bridgeheads = bridgeheads;
    }

    public boolean isRegisteredBridgehead(String bridgehead) {
        return bridgeheads.contains(bridgehead);
    }

}
