package de.samply.project;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ProjectParameters(
        @NotNull String projectName,
        @NotEmpty String[] bridgeheads
) {

}
