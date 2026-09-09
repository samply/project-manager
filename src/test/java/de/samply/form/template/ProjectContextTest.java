package de.samply.form.template;

import de.samply.form.FormFieldConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ProjectContextTest {

    @Test
    void resolvesAConfiguredPlaceholder() {
        ProjectContext context = new ProjectContext(Map.of(ProjectContextKey.PROJECT_CODE, "PROJ-001"));
        FormFieldConfig field = FormFieldConfig.builder().projectValue("${project-code}").build();

        assertThat(context.resolveProjectContext(field).getProjectValue()).isEqualTo("PROJ-001");
    }

    @Test
    void leavesAnUnresolvablePlaceholderInPlaceRatherThanThrowing() {
        // Regression test: a missing key used to throw
        // "named capturing group is missing trailing '}'" instead of simply
        // leaving the "${...}" text as-is, because the fallback branch of
        // resolvePlaceholders passed the raw "${...}" text into
        // Matcher.appendReplacement without escaping it.
        ProjectContext context = new ProjectContext(Map.of());
        FormFieldConfig field = FormFieldConfig.builder().projectValue("${not-configured-anywhere}").build();

        assertThatCode(() -> context.resolveProjectContext(field)).doesNotThrowAnyException();
        assertThat(context.resolveProjectContext(field).getProjectValue()).isEqualTo("${not-configured-anywhere}");
    }

    @Test
    void resolvesAValueContainingDollarSignsInTheReplacementItself() {
        // The replacement text can itself contain "$" (e.g. environment
        // variables, or free text a user entered) - Matcher.quoteReplacement
        // must be applied to it too, or appendReplacement would misinterpret it.
        ProjectContext context = new ProjectContext(
                Map.of(ProjectContextKey.ENVIRONMENT_VARIABLES, "PRICE=$5.00"));
        FormFieldConfig field = FormFieldConfig.builder().projectValue("${environment-variables}").build();

        assertThat(context.resolveProjectContext(field).getProjectValue()).isEqualTo("PRICE=$5.00");
    }

    @Test
    void leavesAPlainValueWithNoPlaceholderUnchanged() {
        ProjectContext context = new ProjectContext(Map.of());
        FormFieldConfig field = FormFieldConfig.builder().projectValue("plain text, no placeholder").build();

        assertThat(context.resolveProjectContext(field).getProjectValue()).isEqualTo("plain text, no placeholder");
    }
}
