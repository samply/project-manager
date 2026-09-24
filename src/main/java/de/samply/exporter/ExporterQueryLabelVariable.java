package de.samply.exporter;

/**
 * Variables available in {@code EXPORTER_QUERY_LABEL_TEMPLATE}. Each one is used in the
 * template by its name, e.g. {@code {{PROJECT_CODE}}} or {@code {{PROJECT_CODE.substring(6)}}}.
 */
public enum ExporterQueryLabelVariable {
    /** Project ID generated from {@code PROJECT_ID_TEMPLATE}, e.g. {@code REQ-2026-0001}. */
    PROJECT_CODE,
    /**
     * Type of the query sent to the bridgehead: {@code EXPORT}, {@code DATASHIELD},
     * {@code RESEARCH_ENVIRONMENT} or {@code SAMPLES}. One project can send several types.
     */
    PROJECT_TYPE,
    /** Label the user gave to the query of the project, e.g. {@code My query}; empty if none. */
    QUERY_LABEL
}
