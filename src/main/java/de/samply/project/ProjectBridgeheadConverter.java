package de.samply.project;

import de.samply.db.model.ProjectBridgehead;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ProjectBridgeheadConverter implements Converter<String, ProjectBridgehead> {

    @Override
    public ProjectBridgehead convert(@NonNull String source) {
        return null;
    }

}
