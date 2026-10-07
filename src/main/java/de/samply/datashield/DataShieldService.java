package de.samply.datashield;

import de.samply.datashield.dto.DataShieldTokenManagerProjectStatus;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Mono;

/**
 * What the rest of the backend (the controller) needs from DataSHIELD. Part of the optional module DATASHIELD:
 * {@link DataShieldTokenManagerService} when enabled, {@link DisabledDataShieldService} when not.
 */
public interface DataShieldService {

    Mono<DataShieldTokenManagerProjectStatus> fetchProjectStatus(@NotNull Project project, @NotNull ProjectBridgehead bridgehead);

    Resource fetchAuthenticationScript(Project project, ProjectBridgehead bridgehead);

    Boolean existsAuthenticationScript(Project project, ProjectBridgehead bridgehead);

}
