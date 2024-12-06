package de.samply.email.thymeleaf;

import de.samply.app.ProjectManagerConst;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailContextProcessor extends AbstractElementTagProcessor {

    private final Set<String> supportedTagNames = Set.of(ProjectManagerConst.EMAIL_CONTEXT_VARIABLES);

    public EmailContextProcessor(String dialectPrefix) {
        super(TemplateMode.HTML, dialectPrefix, null, false, null, true, ProjectManagerConst.THYMELEAF_PROCESSOR_PRECEDENCE);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structureHandler) {
        // Get the complete tag name
        String tagName = tag.getElementCompleteName();

        // Extract the variable name by removing the prefix (if any)
        String variableName = tagName.contains(":")
                ? tagName.substring(tagName.indexOf(':') + 1)
                : tagName;
        if (isEmailContextVariable(context, variableName)) {
            // Retrieve the desired variable from the context
            Object variableValue = context.getVariable(variableName);

            // Retrieve the "default" and "defaultVar" attributes, if they exist
            String defaultValue = tag.getAttributeValue(ProjectManagerConst.EMAIL_CONTEXT_VARIABLE_TAG_ATTRIBUTE_DEFAULT_VALUE);
            String defaultVarName = tag.getAttributeValue(ProjectManagerConst.EMAIL_CONTEXT_VARIABLE_TAG_ATTRIBUTE_DEFAULT_VARIABLE);

            // Retrieve the value of "defaultVar" from the context, if specified
            Object defaultVarValue = defaultVarName != null ? context.getVariable(defaultVarName) : null;

            // Determine the final value based on the priority:
            // 1) Desired variable
            // 2) Default variable
            // 3) Default value
            // 4) Empty string if none are available
            String finalValue = variableValue != null
                    ? variableValue.toString()
                    : defaultVarValue != null
                    ? defaultVarValue.toString()
                    : defaultValue != null
                    ? defaultValue
                    : "";

            // Replace ${XXX} variables with their values from the context
            finalValue = resolveVariables(finalValue, context);

            // Replace the tag with the final value
            structureHandler.replaceWith(finalValue, false);

        }
    }

    private boolean isEmailContextVariable(ITemplateContext context, String variable){
        return variable != null && (supportedTagNames.contains(variable) || context.getVariable(variable) != null);
    }

    // TODO: Ideally, we should let Thymeleaf continue processing the replaced content
    // so that all applicable processors (e.g., variable resolution, additional dialects)
    // are automatically applied.
    // Currently, setting `processable` to true in `structureHandler.replaceWith`
    // only triggers the `StandardInliningTextProcessor`, which simply writes the content
    // to the output without further processing.
    private String resolveVariables(String content, ITemplateContext context) {
        // Combined regular expression to match both ${variable} and <key /> patterns
        Pattern combinedPattern = Pattern.compile(
                "(\\$\\{([^}]+)})|(<\\s*([a-zA-Z0-9_-]+)\\s*/>)"
        );
        Matcher combinedMatcher = combinedPattern.matcher(content);

        StringBuffer resolvedContent = new StringBuffer();

        // Process the combined matches
        while (combinedMatcher.find()) {
            if (combinedMatcher.group(1) != null) {
                // Match found for ${variable}
                String variableName = combinedMatcher.group(2); // Extract variable name inside ${}
                if (isEmailContextVariable(context, variableName)){
                    Object variableValue = context.getVariable(variableName); // Resolve variable
                    String replacement = (variableValue != null) ? variableValue.toString() : ""; // Use empty string if not found
                    combinedMatcher.appendReplacement(resolvedContent, Matcher.quoteReplacement(replacement));
                }
            } else if (combinedMatcher.group(3) != null) {
                // Match found for <key />
                String tagName = combinedMatcher.group(4); // Extract the tag name
                if (isEmailContextVariable(context, tagName)){
                    Object tagValue = context.getVariable(tagName); // Resolve the value from context
                    String replacement = (tagValue != null) ? tagValue.toString() : ""; // Use empty string if not found
                    combinedMatcher.appendReplacement(resolvedContent, Matcher.quoteReplacement(replacement));
                }
            }
        }

        // Append any remaining content after the last match
        combinedMatcher.appendTail(resolvedContent);

        return resolvedContent.toString();
    }


}
