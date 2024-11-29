package de.samply.email.thymeleaf;

import de.samply.app.ProjectManagerConst;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Set;

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
        if (supportedTagNames.contains(variableName)) {
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

            // Replace the tag with the final value
            structureHandler.replaceWith(finalValue, false);
        }
    }

}
