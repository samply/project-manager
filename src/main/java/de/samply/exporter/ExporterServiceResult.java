package de.samply.exporter;

import de.samply.db.model.ProjectBridgehead;

public record ExporterServiceResult(ProjectBridgehead projectBridgehead, String result) {
}
