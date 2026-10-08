package de.samply.researchenvironment;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.model.ProjectCoder;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * Research environment: a workspace per user of a research environment or DataSHIELD request, where the exported data
 * are transferred to. Part of the optional module RESEARCH_ENVIRONMENT: implemented with Coder
 * ({@link de.samply.coder.CoderResearchEnvironmentService}) when enabled, {@link DisabledResearchEnvironmentService}
 * when not.
 */
public interface ResearchEnvironmentService {

    /** Creates the user's workspace if it has none yet; empty if it already has one. */
    Mono<ProjectCoder> createWorkspace(@NotNull ProjectBridgeheadUser projectBridgeheadUser);

    Flux<ProjectCoder> deleteAllWorkspaces(@NotNull String projectCode, @NotNull String bridgehead);

    Mono<ProjectCoder> deleteWorkspace(@NotNull ProjectBridgeheadUser user);

    Mono<ProjectCoder> deleteWorkspace(@NotNull ProjectCoder projectCoder);

    /** ID of the user's workspace: its name and its Beam app ID (alphanumeric, limited in length). */
    String fetchCoderAppId(@NotNull ProjectBridgeheadUser projectBridgeheadUser);

    boolean existsUserResearchEnvironmentWorkspace(@NotNull Project project, @NotNull ProjectBridgehead bridgehead);

    boolean existsUserResearchEnvironmentWorkspace(@NotNull ProjectBridgeheadUser projectBridgeheadUser);

    List<ProjectCoder> fetchCoderOrderedByCreatedAtDesc(String projectCode, String bridgehead, String email);

    void saveCoder(ProjectCoder projectCoder);

    /** Where users open their workspaces (frontend and emails). */
    Optional<String> fetchResearchEnvironmentUrl();

    /** Beam ID the exporter sends the export file to, for the workspace. */
    String fetchFileBeamId(@NotNull ProjectCoder projectCoder);

}
