package de.samply.researchenvironment;

import de.samply.annotations.ModuleStandIn;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.model.ProjectCoder;
import de.samply.modules.OptionalModule;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * Stand-in while the module RESEARCH_ENVIRONMENT is disabled: there are no workspaces. Creating and deleting do
 * nothing, as before with ENABLE_CODER=false; storing a workspace or sending a file to one cannot happen without the
 * module and throws.
 */
@Slf4j
@Service
@ModuleStandIn(OptionalModule.RESEARCH_ENVIRONMENT)
public class DisabledResearchEnvironmentService implements ResearchEnvironmentService {

    private static final String DISABLED = "The research environment is disabled (ENABLE_RESEARCH_ENVIRONMENT=false)";

    @Override
    public Mono<ProjectCoder> createWorkspace(@NotNull ProjectBridgeheadUser projectBridgeheadUser) {
        log.debug("{}: no workspace created for {}", DISABLED, projectBridgeheadUser.getEmail());
        return Mono.empty();
    }

    @Override
    public Flux<ProjectCoder> deleteAllWorkspaces(@NotNull String projectCode, @NotNull String bridgehead) {
        return Flux.empty();
    }

    @Override
    public Mono<ProjectCoder> deleteWorkspace(@NotNull ProjectBridgeheadUser user) {
        return Mono.empty();
    }

    @Override
    public Mono<ProjectCoder> deleteWorkspace(@NotNull ProjectCoder projectCoder) {
        return Mono.empty();
    }

    @Override
    public boolean existsUserResearchEnvironmentWorkspace(@NotNull Project project, @NotNull ProjectBridgehead bridgehead) {
        return false;
    }

    @Override
    public boolean existsUserResearchEnvironmentWorkspace(@NotNull ProjectBridgeheadUser projectBridgeheadUser) {
        return false;
    }

    @Override
    public List<ProjectCoder> fetchCoderOrderedByCreatedAtDesc(String projectCode, String bridgehead, String email) {
        return List.of();
    }

    @Override
    public void saveCoder(ProjectCoder projectCoder) {
        throw new IllegalStateException(DISABLED);
    }

    @Override
    public Optional<String> fetchResearchEnvironmentUrl() {
        return Optional.empty();
    }

    @Override
    public String fetchFileBeamId(@NotNull ProjectCoder projectCoder) {
        throw new IllegalStateException(DISABLED);
    }

}
