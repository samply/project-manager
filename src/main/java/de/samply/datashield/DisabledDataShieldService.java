package de.samply.datashield;

import de.samply.annotations.ConditionalOnModuleDisabled;
import de.samply.datashield.dto.DataShieldProjectStatus;
import de.samply.datashield.dto.DataShieldTokenManagerProjectStatus;
import de.samply.datashield.dto.DataShieldTokenManagerTokenStatus;
import de.samply.datashield.dto.DataShieldTokenStatus;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.modules.OptionalModule;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Used while the module DATASHIELD is disabled: the same answers as before with ENABLE_TOKEN_MANAGER=false - the
 * project and token status INACTIVE (shown in the frontend), no authentication script, no bridgeheads with tokens.
 * Creating, refreshing and removing tokens is done by the DataSHIELD job, which belongs to the module - so those calls
 * are programming errors.
 */
@Service
@ConditionalOnModuleDisabled(OptionalModule.DATASHIELD)
public class DisabledDataShieldService implements DataShieldService {

    @Override
    public Mono<Void> generateTokensInOpal(@NotNull Project project, @NotNull ProjectBridgehead bridgehead, @NotNull String email,
                                           Supplier<Mono> ifSuccessMonoSupplier) {
        return Mono.error(createDisabledException("generate tokens"));
    }

    @Override
    public List<ProjectBridgehead> fetchProjectBridgeheads(Project project, ProjectBridgehead bridgehead, String email) {
        return List.of();
    }

    @Override
    public List<ProjectBridgehead> fetchProjectBridgeheads(Project project, ProjectBridgehead bridgehead, String email,
                                                           Function<ProjectBridgehead, Boolean> filter) {
        return List.of();
    }

    @Override
    public Mono<DataShieldTokenManagerTokenStatus> fetchTokenStatus(@NotNull Project project, @NotNull ProjectBridgehead bridgehead,
                                                                    @NotNull String email) {
        return Mono.just(new DataShieldTokenManagerTokenStatus(project.getCode(), bridgehead.getBridgehead(), email, null,
                DataShieldProjectStatus.INACTIVE, DataShieldTokenStatus.INACTIVE));
    }

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

    @Override
    public Mono<Void> refreshToken(@NotNull Project project, @NotNull ProjectBridgehead bridgehead, @NotNull String email,
                                   Supplier<Mono> ifSuccessMonoSupplier) {
        return Mono.error(createDisabledException("refresh a token"));
    }

    @Override
    public Mono<Void> removeTokens(@NotNull Project project, @NotNull ProjectBridgehead bridgehead, @NotNull String email,
                                   Supplier<Mono> ifSuccessMonoSupplier) {
        return Mono.error(createDisabledException("remove tokens"));
    }

    @Override
    public Mono<Void> removeProjectAndTokens(@NotNull Project project, @NotNull ProjectBridgehead bridgehead) {
        return Mono.error(createDisabledException("remove a project and its tokens"));
    }

    private IllegalStateException createDisabledException(String action) {
        return new IllegalStateException("DataSHIELD disabled: cannot " + action);
    }

}
