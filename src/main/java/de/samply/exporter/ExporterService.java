package de.samply.exporter;

import de.samply.db.model.ProjectCoder;
import de.samply.project.ProjectType;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Exports of the requests' data at the bridgeheads: sending the query to a bridgehead's exporter, executing it,
 * following the export, and transferring the export file into a research environment workspace. Part of the optional
 * module EXPORTER: {@link BeamExporterService} when enabled, {@link DisabledExporterService} when not.
 */
public interface ExporterService {

    /** The exporter templates a request of each type may use. */
    Map<ProjectType, Set<String>> getExporterTemplates();

    /** Saves the query in the bridgehead's exporter, without executing it. */
    Mono<ExporterServiceResult> sendQueryToBridgehead(ProjectBridgeheadAndType projectBridgeheadAndType) throws ExporterServiceException;

    /** Saves the query in the bridgehead's exporter and executes it. */
    Mono<ExporterServiceResult> sendQueryToBridgeheadAndExecute(ProjectBridgeheadAndType projectBridgeheadAndType) throws ExporterServiceException;

    Mono<ExporterServiceResult> checkExecutionStatus(ProjectBridgeheadAndType projectBridgeheadAndType) throws ExporterServiceException;

    Mono<ExporterServiceResult> checkIfQueryIsAlreadySentOrExecuted(ProjectBridgeheadAndType projectBridgeheadAndType);

    /** The ID of the export execution in the exporter's response, if there is one. */
    Optional<String> fetchExporterExecutionIdFromExporterResponse(String exporterResponse);

    /** Transfers the export file of the request at the bridgehead into the session user's workspace. */
    void transferFileToResearchEnvironment(@NotNull String projectCode, @NotNull String bridgehead);

    /** Transfers the export file into the workspace. */
    Mono<Void> transferFileToResearchEnvironment(ProjectCoder projectCoder);

    boolean isExportFileTransferredToResearchEnvironment(@NotNull String projectCode, @NotNull String bridgehead);

}
