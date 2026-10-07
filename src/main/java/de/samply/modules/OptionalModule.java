package de.samply.modules;

import de.samply.app.ProjectManagerConst;
import org.springframework.core.env.PropertyResolver;

import java.util.Set;

/**
 * An optional application module: a group of beans for one scope (e.g. the research environment) that a deployment
 * switches on or off with an environment variable. Not a Java module (JPMS) and not a frontend module
 * ({@link de.samply.annotations.FrontendSiteModule}).
 * <p>
 * The beans of a module carry {@link de.samply.annotations.ModuleComponent}: they are only created when the module is
 * enabled, so a disabled module needs none of its configuration. See docs/optional-modules.md.
 */
public enum OptionalModule {

    /** Research environment workspaces (implemented with Coder), registered as Beam apps (app register). */
    RESEARCH_ENVIRONMENT(ProjectManagerConst.ENABLE_RESEARCH_ENVIRONMENT),
    /** DataSHIELD: Opal tokens through the token manager; every user gets a research environment workspace. */
    DATASHIELD(ProjectManagerConst.ENABLE_DATASHIELD, RESEARCH_ENVIRONMENT),
    /** Sends the scheduled queries to the bridgeheads through the exporter and follows the exports. */
    EXPORTER(ProjectManagerConst.ENABLE_EXPORTER),
    /** Feasibility queries to the bridgeheads through Beam. */
    FEASIBILITY(ProjectManagerConst.ENABLE_FEASIBILITY),
    /** Sending emails (rendering the templates is always possible). */
    EMAILS(ProjectManagerConst.ENABLE_EMAILS);

    private final String enableVariable;
    private final Set<OptionalModule> requiredModules;

    OptionalModule(String enableVariable, OptionalModule... requiredModules) {
        this.enableVariable = enableVariable;
        this.requiredModules = Set.of(requiredModules);
    }

    /** Environment variable that enables the module ("true" or "false"). */
    public String getEnableVariable() {
        return enableVariable;
    }

    /** Modules that must be enabled too when this one is. */
    public Set<OptionalModule> getRequiredModules() {
        return requiredModules;
    }

    /**
     * Enabled unless its variable is "false": all modules were enabled by default before they became optional. Same
     * rule as the ENABLE_*_SV placeholders in ProjectManagerConst.
     */
    public boolean isEnabled(PropertyResolver propertyResolver) {
        return propertyResolver.getProperty(enableVariable, Boolean.class, true);
    }

}
