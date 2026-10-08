package de.samply.exporter;

import de.samply.annotations.ConditionalOnModuleDisabled;
import de.samply.db.model.ProjectCoder;
import de.samply.modules.OptionalModule;
import de.samply.project.ProjectType;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Used while the module EXPORTER is disabled: no exporter templates, no export. Sending and following queries is
 * done by the exporter job, which belongs to the module, and the research environment requires the module - so those
 * calls are programming errors.
 */
@Service
@ConditionalOnModuleDisabled(OptionalModule.EXPORTER)
public class DisabledExporterService implements ExporterService {

    @Override
    public Map<ProjectType, Set<String>> getExporterTemplates() {
        return Map.of();
    }

    @Override
    public Mono<ExporterServiceResult> sendQueryToBridgehead(ProjectBridgeheadAndType projectBridgeheadAndType) {
        return Mono.error(createDisabledException("send a query"));
    }

    @Override
    public Mono<ExporterServiceResult> sendQueryToBridgeheadAndExecute(ProjectBridgeheadAndType projectBridgeheadAndType) {
        return Mono.error(createDisabledException("send and execute a query"));
    }

    @Override
    public Mono<ExporterServiceResult> checkExecutionStatus(ProjectBridgeheadAndType projectBridgeheadAndType) {
        return Mono.error(createDisabledException("check an execution status"));
    }

    @Override
    public Mono<ExporterServiceResult> checkIfQueryIsAlreadySentOrExecuted(ProjectBridgeheadAndType projectBridgeheadAndType) {
        return Mono.error(createDisabledException("check a query"));
    }

    @Override
    public Optional<String> fetchExporterExecutionIdFromExporterResponse(String exporterResponse) {
        return Optional.empty();
    }

    @Override
    public void transferFileToResearchEnvironment(@NotNull String projectCode, @NotNull String bridgehead) {
        throw createDisabledException("transfer the export file of project " + projectCode + " and bridgehead " + bridgehead);
    }

    @Override
    public Mono<Void> transferFileToResearchEnvironment(ProjectCoder projectCoder) {
        return Mono.error(createDisabledException("transfer an export file"));
    }

    @Override
    public boolean isExportFileTransferredToResearchEnvironment(@NotNull String projectCode, @NotNull String bridgehead) {
        return false;
    }

    private IllegalStateException createDisabledException(String action) {
        return new IllegalStateException("Exporter disabled: cannot " + action);
    }

}
