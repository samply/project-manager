package de.samply.form.core.condition;

import de.samply.form.core.FormConfig;
import de.samply.form.core.model.FormFieldConfig;
import de.samply.frontend.dto.FormField;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;

/**
 * Evaluates the visibility conditions configured for form fields.
 * <p>
 * Conditions are defined in {@link de.samply.form.core.model.FormFieldConfig} as SpEL
 * expressions. Because fields can belong to repeatable blocks, a condition
 * cannot always be evaluated against a single global field set. Instead, this
 * evaluator delegates context generation to {@link FormFieldConditionContext}
 * and considers a condition met when it matches at least one valid
 * evaluation context for the corresponding block instance.
 * <p>
 * A field label can be configured several times ("instances", see
 * {@link FormConfig#fetchFormFieldConfigs}). The instances are tried in order and
 * the first whose condition is met is the one the field shows, like
 * if / else if; a field without a matching instance is not shown.
 */
@Slf4j
@Component
public class FormFieldConditionEvaluator {

    private final FormConfig formConfig;

    public FormFieldConditionEvaluator(FormConfig formConfig) {
        this.formConfig = formConfig;
    }


    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();


    /**
     * Chooses instances for fields, with conditions evaluated against the given fields.
     */
    public InstanceResolver instanceResolver(@NotNull Collection<FormField> contextFields) {
        return new InstanceResolver(contextFields);
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

    /**
     * Chooses the instance each field shows. The evaluation context is built
     * once, on the first condition that needs it: a form without conditions
     * never builds it.
     */
    public class InstanceResolver {

        private final Collection<FormField> contextFields;
        private FormFieldConditionContext context;

        private InstanceResolver(Collection<FormField> contextFields) {
            this.contextFields = contextFields;
        }

        /** The first instance of the field whose condition is met, or empty if none is. */
        public Optional<FormFieldConfig> resolve(@NotNull FormField formField) {
            List<FormFieldConfig> instances = formConfig.fetchFormFieldConfigs(formField.title(), formField.label());
            OptionalInt chosen = IntStream.range(0, instances.size())
                    .filter(i -> isConditionMet(instances.get(i), formField))
                    .findFirst();
            chosen.ifPresent(i -> logLaterMatches(formField, instances, i));
            return chosen.stream().mapToObj(instances::get).findFirst();
        }

        private boolean isConditionMet(FormFieldConfig instance, FormField formField) {
            String condition = instance.getCondition();
            if (condition == null || condition.isBlank()) {
                return true;
            }
            Expression expression = EXPRESSION_PARSER.parseExpression(condition);
            return Optional.ofNullable(context().getContext(formField)).orElse(List.of()).stream()
                    .anyMatch(evaluationContext -> evaluateBooleanExpression(expression, evaluationContext));
        }

        // Overlapping conditions are allowed (the first instance wins), but often a misordered configuration.
        private void logLaterMatches(FormField formField, List<FormFieldConfig> instances, int chosen) {
            if (log.isDebugEnabled()) {
                IntStream.range(chosen + 1, instances.size())
                        .filter(i -> isConditionMet(instances.get(i), formField))
                        .forEach(i -> log.debug("Field '{}' of form '{}' (block instance {}): instance {} is shown, "
                                        + "instance {} would also match",
                                formField.label(), formField.title(), formField.blockInstance(), chosen + 1, i + 1));
            }
        }

        private FormFieldConditionContext context() {
            if (context == null) {
                context = new FormFieldConditionContext(contextFields);
            }
            return context;
        }
    }

    private boolean evaluateBooleanExpression(Expression expression, StandardEvaluationContext context) {
        try {
            return expression.getValue(context, Boolean.class);
        } catch (Exception e) {
            return false;
        }
    }

}
