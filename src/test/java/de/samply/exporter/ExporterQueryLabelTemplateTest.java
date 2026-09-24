package de.samply.exporter;

import de.samply.project.ProjectType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ExporterQueryLabelTemplateTest {

    private static String render(String template, String projectCode) {
        return new ExporterQueryLabelTemplate(template).render(projectCode, ProjectType.EXPORT, "My query");
    }

    @Test
    void rendersVariables() {
        assertThat(render("[{{PROJECT_CODE}}] {{QUERY_LABEL}}", "REQ-2026-0001"))
                .isEqualTo("[REQ-2026-0001] My query");
        assertThat(render("[{{PROJECT_TYPE}}-{{PROJECT_CODE}}] {{QUERY_LABEL}}", "REQ-2026-0001"))
                .isEqualTo("[EXPORT-REQ-2026-0001] My query");
    }

    @Test
    void callsStringMethods() {
        assertThat(render("[{{PROJECT_CODE.substring(6)}}] {{QUERY_LABEL}}", "REQ-2026-0001"))
                .isEqualTo("[26-0001] My query");
        assertThat(render("{{PROJECT_CODE.toLowerCase()}}: {{QUERY_LABEL.toUpperCase()}}", "REQ-2026-0001"))
                .isEqualTo("req-2026-0001: MY QUERY");
        assertThat(render("{{PROJECT_CODE.replace('REQ-', '')}} {{QUERY_LABEL}}", "REQ-2026-0001"))
                .isEqualTo("2026-0001 My query");
        assertThat(render("{{PROJECT_CODE.substring(0, 8)}}", "REQ-2026-0001"))
                .isEqualTo("REQ-2026");
    }

    @Test
    void treatsMissingQueryLabelAsEmpty() {
        assertThat(new ExporterQueryLabelTemplate("[{{PROJECT_CODE}}] {{QUERY_LABEL}}")
                .render("REQ-2026-0001", ProjectType.EXPORT, null))
                .isEqualTo("[REQ-2026-0001] ");
    }

    @Test
    void fallsBackWhenExpressionFailsForOneProject() {
        assertThat(render("[{{PROJECT_CODE.substring(6)}}] {{QUERY_LABEL}}", "REQ"))
                .isEqualTo("[REQ] My query");
    }

    @Test
    void requiresTemplate() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ExporterQueryLabelTemplate(""))
                .withMessageContaining("EXPORTER_QUERY_LABEL_TEMPLATE is required");
        assertThatIllegalArgumentException().isThrownBy(() -> new ExporterQueryLabelTemplate(null));
    }

    @Test
    void rejectsInvalidTemplatesAtStartup() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ExporterQueryLabelTemplate("[{{PROJECT_CODE.}}]"));
        assertThatIllegalArgumentException().isThrownBy(() -> new ExporterQueryLabelTemplate("{{UNKNOWN}}"));
        assertThatIllegalArgumentException().isThrownBy(() -> new ExporterQueryLabelTemplate("{{PROJECT_CODE.substring(99)}}"));
    }

    @Test
    void blocksJavaTypesAndConstructors() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ExporterQueryLabelTemplate("{{T(java.lang.Runtime).getRuntime()}}"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ExporterQueryLabelTemplate("{{new java.io.File('x')}}"));
    }
}
