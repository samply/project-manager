package de.samply.project;

import de.samply.modules.OptionalModule;

import java.util.Set;

/**
 * The type of a request's output. Each type needs optional modules; a deployment may only offer a type whose modules
 * are enabled ("true" or "test"), see {@link de.samply.modules.OptionalModules#isAvailable(ProjectType)}.
 */
public enum ProjectType {
    EXPORT(OptionalModule.EXPORTER),
    SAMPLES(OptionalModule.EXPORTER), // Interacts with Negotiator (a NegotiatorService is planned)
    DATASHIELD(OptionalModule.DATASHIELD),
    RESEARCH_ENVIRONMENT(OptionalModule.RESEARCH_ENVIRONMENT),
    // Sends its query through the exporter as before, until a SequencingService exists
    SEQUENCING(OptionalModule.EXPORTER);

    private final Set<OptionalModule> requiredModules;

    ProjectType(OptionalModule... requiredModules) {
        this.requiredModules = Set.of(requiredModules);
    }

    public Set<OptionalModule> getRequiredModules() {
        return requiredModules;
    }

}
