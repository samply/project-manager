package de.samply.form.condition;

import de.samply.form.FormConfig;
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
 * Conditions are defined in {@link de.samply.form.FormFieldConfig} as SpEL
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
        FormFieldConditionContext context = new FormFieldConditionContext(formFields);
        return formFields
                .stream()
                .filter(formField -> !hasCondition(formField) ||
                        evaluateCondition(formField, context.getContext(formField)))
                .toList();
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
