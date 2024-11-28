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
            // Retrieve the value of the variable from the context
            Object variableValue = context.getVariable(variableName);

            // Replace the tag with the variable's value or an empty string if the variable doesn't exist
            structureHandler.replaceWith(variableValue != null ? variableValue.toString() : "", false);
        }
    }

}
