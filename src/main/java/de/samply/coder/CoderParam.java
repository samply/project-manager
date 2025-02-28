package de.samply.coder;

import de.samply.coder.request.CreateRequestParameter;
import de.samply.db.model.ProjectCoder;
import jakarta.validation.constraints.NotNull;
import org.springframework.util.StringUtils;

public enum CoderParam {
    APP_ID,
    APP_SECRET;

    public static void replaceParameters(@NotNull CreateRequestParameter parameter, @NotNull ProjectCoder projectCoder) {
        replaceParameter(parameter, APP_ID, projectCoder.getAppId());
        replaceParameter(parameter, APP_SECRET, projectCoder.getAppSecret());
    }

    private static void replaceParameter(CreateRequestParameter parameter, CoderParam coderParam, String value) {
        if (StringUtils.hasText(parameter.getValue()) && StringUtils.hasText(value)) {
            parameter.setValue(parameter.getValue().replace("${" + coderParam.name() + "}", value));
        }
    }


}
