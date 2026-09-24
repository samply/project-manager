package de.samply.exporter;

import de.samply.app.ProjectManagerConst;
import de.samply.project.ProjectType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.MapAccessor;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Builds the query label sent to the exporter from {@code EXPORTER_QUERY_LABEL_TEMPLATE}.
 *
 * <p>The template is literal text with SpEL expressions enclosed in double braces, e.g.
 * {@code [{{PROJECT_CODE}}] {{QUERY_LABEL}}} or {@code [{{PROJECT_CODE.substring(6)}}] {{QUERY_LABEL}}}.
 * Expressions can read the {@link ExporterQueryLabelVariable variables} and call
 * their methods, but cannot reference Java types, create objects or access beans.</p>
 *
 * <p>The template is required. It is parsed and evaluated with sample values at startup, so that a
 * missing or broken template stops the application with a clear message.</p>
 */
@Slf4j
@Component
public class ExporterQueryLabelTemplate {

    // Not "#{...}": Spring would evaluate that itself while injecting the environment variable
    private static final TemplateParserContext DELIMITERS = new TemplateParserContext("{{", "}}");
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final EvaluationContext CONTEXT = SimpleEvaluationContext
            .forPropertyAccessors(new MapAccessor(false))
            .withInstanceMethods()
            .build();

    private final String template;
    private final Expression expression;

    public ExporterQueryLabelTemplate(@Value(ProjectManagerConst.EXPORTER_QUERY_LABEL_TEMPLATE_SV) String template) {
        this.template = template;
        this.expression = parse(template);
    }

    /**
     * Renders the query label of a project for the exporter.
     *
     * <p>If the expression fails for this project (e.g. {@code substring(6)} of a shorter code),
     * the error is logged and {@code [PROJECT_CODE] QUERY_LABEL} is returned, so that the query
     * is still sent.</p>
     */
    public String render(String projectCode, ProjectType projectType, String queryLabel) {
        try {
            return evaluate(expression, projectCode, projectType, queryLabel);
        } catch (RuntimeException e) {
            // Also exceptions of the called methods, which SpEL does not wrap
            log.error("EXPORTER_QUERY_LABEL_TEMPLATE '{}' failed for project {}: {}", template, projectCode, e.getMessage());
            return "[" + projectCode + "] " + nullToEmpty(queryLabel);
        }
    }

    private static Expression parse(String template) {
        if (isBlank(template)) {
            throw new IllegalArgumentException("EXPORTER_QUERY_LABEL_TEMPLATE is required,"
                    + " e.g. EXPORTER_QUERY_LABEL_TEMPLATE=\"[{{PROJECT_CODE}}] {{QUERY_LABEL}}\"");
        }
        try {
            Expression expression = PARSER.parseExpression(template, DELIMITERS);
            evaluate(expression, "REQ-2026-0001", ProjectType.EXPORT, "Sample query");
            return expression;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid EXPORTER_QUERY_LABEL_TEMPLATE '" + template + "': "
                    + e.getMessage(), e);
        }
    }

    private static String evaluate(Expression expression, String projectCode, ProjectType projectType, String queryLabel) {
        Map<String, Object> variables = Map.of(
                ExporterQueryLabelVariable.PROJECT_CODE.name(), projectCode,
                ExporterQueryLabelVariable.PROJECT_TYPE.name(), projectType.name(),
                ExporterQueryLabelVariable.QUERY_LABEL.name(), nullToEmpty(queryLabel));
        String label = expression.getValue(CONTEXT, variables, String.class);
        return nullToEmpty(label);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
