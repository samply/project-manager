package de.samply.datashield;

import de.samply.datashield.dto.DataShieldTokenManagerProjectStatus;
import de.samply.datashield.dto.DataShieldTokenManagerTokenStatus;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * DataSHIELD: the Opal tokens of the users of a request at the bridgeheads (through the token manager), their status,
 * and the authentication script. Part of the optional module DATASHIELD: {@link DataShieldTokenManagerService} when
 * enabled, {@link DisabledDataShieldService} when not.
 */
public interface DataShieldService {

    Mono<Void> generateTokensInOpal(@NotNull Project project, @NotNull ProjectBridgehead bridgehead, @NotNull String email,
                                    Supplier<Mono> ifSuccessMonoSupplier) throws DataShieldTokenManagerServiceException;

    /** The bridgeheads the user's tokens cover: the given one for developers and pilots, all for the final user. */
    List<ProjectBridgehead> fetchProjectBridgeheads(Project project, ProjectBridgehead bridgehead, String email)
            throws DataShieldTokenManagerServiceException;

    /** As {@link #fetchProjectBridgeheads(Project, ProjectBridgehead, String)}, only those the filter accepts. */
    List<ProjectBridgehead> fetchProjectBridgeheads(Project project, ProjectBridgehead bridgehead, String email,
                                                    Function<ProjectBridgehead, Boolean> filter)
            throws DataShieldTokenManagerServiceException;

    Mono<DataShieldTokenManagerTokenStatus> fetchTokenStatus(@NotNull Project project, @NotNull ProjectBridgehead bridgehead,
                                                             @NotNull String email);

    Mono<DataShieldTokenManagerProjectStatus> fetchProjectStatus(@NotNull Project project, @NotNull ProjectBridgehead bridgehead);

    Resource fetchAuthenticationScript(Project project, ProjectBridgehead bridgehead);

    Boolean existsAuthenticationScript(Project project, ProjectBridgehead bridgehead);

    Mono<Void> refreshToken(@NotNull Project project, @NotNull ProjectBridgehead bridgehead, @NotNull String email,
                            Supplier<Mono> ifSuccessMonoSupplier) throws DataShieldTokenManagerServiceException;

    Mono<Void> removeTokens(@NotNull Project project, @NotNull ProjectBridgehead bridgehead, @NotNull String email,
                            Supplier<Mono> ifSuccessMonoSupplier);

    Mono<Void> removeProjectAndTokens(@NotNull Project project, @NotNull ProjectBridgehead bridgehead);

}
