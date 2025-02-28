package de.samply.email.thymeleaf;

import de.samply.app.ProjectManagerConst;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

import java.util.HashSet;
import java.util.Set;

public class ProjectManagerDialect extends AbstractProcessorDialect {

    protected ProjectManagerDialect() {
        super(ProjectManagerConst.THYMELEAF_DIALECT_NAME, ProjectManagerConst.THYMELEAF_DIALECT_PREFIX, ProjectManagerConst.THYMELEAF_DIALECT_PRECEDENCE);
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        Set<IProcessor> processors = new HashSet<>();
        processors.add(new EmailContextProcessor(dialectPrefix));
        return processors;
    }

}
