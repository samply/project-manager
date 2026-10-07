package de.samply.datashield;

import de.samply.annotations.ModuleStandIn;
import de.samply.datashield.dto.DataShieldProjectStatus;
import de.samply.datashield.dto.DataShieldTokenManagerProjectStatus;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.modules.OptionalModule;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Stand-in while the module DATASHIELD is disabled: the same answers as before with ENABLE_TOKEN_MANAGER=false - the
 * project status INACTIVE (shown in the frontend), no authentication script.
 */
@Service
@ModuleStandIn(OptionalModule.DATASHIELD)
public class DisabledDataShieldService implements DataShieldService {

    @Override
    public Mono<DataShieldTokenManagerProjectStatus> fetchProjectStatus(@NotNull Project project, @NotNull ProjectBridgehead bridgehead) {
        return Mono.just(new DataShieldTokenManagerProjectStatus(project.getCode(), bridgehead.getBridgehead(),
                DataShieldProjectStatus.INACTIVE));
    }

    @Override
    public Resource fetchAuthenticationScript(Project project, ProjectBridgehead bridgehead) {
        return new ByteArrayResource("Token Manager inactive".getBytes());
    }

    @Override
    public Boolean existsAuthenticationScript(Project project, ProjectBridgehead bridgehead) {
        return false;
    }

}
