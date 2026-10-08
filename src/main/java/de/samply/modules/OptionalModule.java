package de.samply.modules;

import de.samply.app.ProjectManagerConst;
import org.springframework.core.env.PropertyResolver;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An optional application module: a group of beans for one scope (e.g. the research environment) that a deployment
 * switches on or off with an environment variable. Not a Java module (JPMS) and not a frontend module
 * ({@link de.samply.annotations.FrontendSiteModule}).
 * <p>
 * The beans of a module carry {@link de.samply.annotations.ModuleComponent}: they are only created when the module is
 * enabled, so a disabled module needs none of its configuration. See docs/optional-modules.md.
 * <p>
 * An <i>implicit</i> module has no variable: it is enabled exactly when a module that requires it is enabled. A module
 * may only require modules declared before it (Java does not allow forward references in enum constants).
 */
public enum OptionalModule {

    /** Beam: the connection to the bridgeheads that feasibility and exporter share. Implicit. */
    BEAM(null),
    /** Sends the scheduled queries to the bridgeheads through the exporter and follows the exports. */
    EXPORTER(ProjectManagerConst.ENABLE_EXPORTER, BEAM),
    /** Feasibility queries to the bridgeheads through Beam. */
    FEASIBILITY(ProjectManagerConst.ENABLE_FEASIBILITY, BEAM),
    /**
     * Research environment workspaces (implemented with Coder), registered as Beam apps (app register). The exporter
     * transfers the export files into the workspaces.
     */
    RESEARCH_ENVIRONMENT(ProjectManagerConst.ENABLE_RESEARCH_ENVIRONMENT, EXPORTER),
    /** DataSHIELD: Opal tokens through the token manager; every user gets a research environment workspace. */
    DATASHIELD(ProjectManagerConst.ENABLE_DATASHIELD, RESEARCH_ENVIRONMENT),
    /** Sending emails (rendering the templates is always possible). */
    EMAILS(ProjectManagerConst.ENABLE_EMAILS);

    private final String enableVariable;
    private final Set<OptionalModule> requiredModules;

    OptionalModule(String enableVariable, OptionalModule... requiredModules) {
        this.enableVariable = enableVariable;
        this.requiredModules = Set.of(requiredModules);
    }

    /** Environment variable that enables the module ("true" or "false"); null for an implicit module. */
    public String getEnableVariable() {
        return enableVariable;
    }

    public boolean isImplicit() {
        return enableVariable == null;
    }

    /** Modules that must be enabled too when this one is (directly; see {@link #requires}). */
    public Set<OptionalModule> getRequiredModules() {
        return requiredModules;
    }

    /** Whether this module needs the other one, directly or through a module it requires. */
    public boolean requires(OptionalModule module) {
        return requiredModules.contains(module) || requiredModules.stream().anyMatch(required -> required.requires(module));
    }

    /**
     * Enabled unless its variable is "false": all modules were enabled by default before they became optional. Same
     * rule as the ENABLE_*_SV placeholders in ProjectManagerConst. An implicit module is enabled when a module that
     * requires it is.
     */
    public boolean isEnabled(PropertyResolver propertyResolver) {
        return isImplicit()
                ? Arrays.stream(values()).anyMatch(module -> module.requiredModules.contains(this) && module.isEnabled(propertyResolver))
                : propertyResolver.getProperty(enableVariable, Boolean.class, true);
    }

    /** What switches the module on, for messages: its variable, or the modules that require it. */
    public String describeSwitch() {
        return isImplicit()
                ? "required by " + Arrays.stream(values()).filter(module -> module.requiredModules.contains(this))
                        .map(OptionalModule::name).collect(Collectors.joining(", "))
                : enableVariable;
    }

}
