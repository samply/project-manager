package de.samply.form.core.condition;

import de.samply.form.core.FormConfig;
import de.samply.frontend.dto.FormField;
import jakarta.validation.constraints.NotNull;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Evaluates the visibility conditions configured for form fields.
 * <p>
 * Conditions are defined in {@link de.samply.form.core.model.FormFieldConfig} as SpEL
 * expressions. Because fields can belong to repeatable blocks, a condition
 * cannot always be evaluated against a single global field set. Instead, this
 * evaluator delegates context generation to {@link FormFieldConditionContext}
 * and considers a field visible when its condition matches at least one valid
 * evaluation context for the corresponding block instance.
 */
@Component
public class FormFieldConditionEvaluator {

    private final FormConfig formConfig;

    public FormFieldConditionEvaluator(FormConfig formConfig) {
        this.formConfig = formConfig;
    }


    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();


    public Collection<FormField> filter(@NotNull Collection<FormField> formFields) {
        List<FormField> formFieldsWithConditions = fetchFormFieldsWithConditions(formFields);
        if (formFieldsWithConditions.isEmpty()) {
            return formFields;
        }
        return formFields
                .stream()
                .filter(formField -> isVisible(formField, formFields))
                .toList();
    }

    /**
     * Evaluates one field against the supplied form-field context without
     * removing it from the response. This is used for FIXED fields, whose
     * visibility is represented by the active flag instead of filtering them
     * out of the API response.
     */
    public boolean isVisible(@NotNull FormField formField, @NotNull Collection<FormField> contextFields) {
        if (!hasCondition(formField)) {
            return true;
        }
        FormFieldConditionContext context = new FormFieldConditionContext(contextFields);
        return evaluateCondition(formField, context.getContext(formField));
    }

    /**
     * Evaluates a condition that belongs to no field (e.g. a form template's)
     * against the given fields: met if it holds for any combination of block
     * instances, same as a field condition. An expression that fails to
     * evaluate (e.g. it refers to a form that is not present) is not met.
     */
    public boolean isConditionMet(@NotNull String condition, @NotNull Collection<FormField> contextFields) {
        Expression expression = EXPRESSION_PARSER.parseExpression(condition);
        return new FormFieldConditionContext(contextFields).getAllContexts().stream()
                .anyMatch(context -> evaluateBooleanExpression(expression, context));
    }

    /**
     * Throws {@link org.springframework.expression.ParseException} if the
     * condition is not a syntactically valid expression.
     */
    public void validateSyntax(@NotNull String condition) {
        EXPRESSION_PARSER.parseExpression(condition);
    }

    private List<FormField> fetchFormFieldsWithConditions(Collection<FormField> formFields) {
        return formFields
                .stream()
                .filter(this::hasCondition)
                .toList();
    }

    private boolean hasCondition(FormField formField) {
        return fetchCondition(formField) != null;
    }

    private String fetchCondition(FormField formField) {
        return formConfig.fetchFormFieldConfig(formField.title(), formField.label()).getCondition();
    }

    private boolean evaluateCondition(FormField formField, Collection<StandardEvaluationContext> context) {
        for (StandardEvaluationContext standardEvaluationContext : context) {
            if (evaluateCondition(formField, standardEvaluationContext)) {
                return true;
            }
        }
        return false;
    }

    private boolean evaluateCondition(FormField formField, StandardEvaluationContext context) {
        return Optional
                .ofNullable(fetchCondition(formField))
                .map(EXPRESSION_PARSER::parseExpression)
                .map(expression -> evaluateBooleanExpression(expression, context))
                .orElse(true);
    }

    private boolean evaluateBooleanExpression(Expression expression, StandardEvaluationContext context) {
        try {
            return expression.getValue(context, Boolean.class);
        } catch (Exception e) {
            return false;
        }
    }

}
