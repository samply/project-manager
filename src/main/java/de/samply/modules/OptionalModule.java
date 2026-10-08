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
 * The beans of a module carry {@link de.samply.annotations.ConditionalOnModule}: they are only created when the module is
 * enabled, so a disabled module needs none of its configuration. See docs/optional-modules.md.
 * <p>
 * The variable is "true" (the default), "false", or "test" for a module with a test implementation
 * ({@link ModuleMode}).
 * <p>
 * An <i>implicit</i> module has no variable: it is enabled exactly when a module that requires it is "true" (a module in
 * test mode needs none of its required modules). A module may only require modules declared before it (Java does not
 * allow forward references in enum constants).
 */
public enum OptionalModule {

    /** Beam: the connection to the bridgeheads that feasibility and exporter share. Implicit. */
    BEAM(null),
    /**
     * Sends the scheduled queries to the bridgeheads through the exporter and follows the exports; test mode: the
     * queries go through all their states without Beam, nothing is exported.
     */
    EXPORTER(ProjectManagerConst.ENABLE_EXPORTER, true, BEAM),
    /** Feasibility queries to the bridgeheads through Beam; test mode: random results from TEST_FEASIBILITY_RESULT. */
    FEASIBILITY(ProjectManagerConst.ENABLE_FEASIBILITY, true, BEAM),
    /**
     * Research environment workspaces (implemented with Coder), registered as Beam apps (app register). The exporter
     * transfers the export files into the workspaces.
     */
    RESEARCH_ENVIRONMENT(ProjectManagerConst.ENABLE_RESEARCH_ENVIRONMENT, EXPORTER),
    /** DataSHIELD: Opal tokens through the token manager; every user gets a research environment workspace. */
    DATASHIELD(ProjectManagerConst.ENABLE_DATASHIELD, RESEARCH_ENVIRONMENT),
    /**
     * Sending emails (rendering the templates is always possible); test mode: everything runs as when sending, but the
     * emails are written to the log (EMAILS_TEST_LOG: summary or full).
     */
    EMAILS(ProjectManagerConst.ENABLE_EMAILS, true),
    /**
     * Site admins execute a request's query at their site directly (endpoint saveAndExecuteQueryInBridgehead), outside
     * the usual flow. Disabled unless its variable is "true". The request types it executes need their own modules
     * (e.g. EXPORTER), checked where they are received.
     */
    EXTERNAL_EXECUTION(ProjectManagerConst.ENABLE_EXTERNAL_EXECUTION, ModuleMode.FALSE, false);

    private final String enableVariable;
    private final ModuleMode defaultMode;
    private final boolean withTestMode;
    private final Set<OptionalModule> requiredModules;

    /** Enabled by default ("true"): all modules were, before they became optional. */
    OptionalModule(String enableVariable, OptionalModule... requiredModules) {
        this(enableVariable, false, requiredModules);
    }

    OptionalModule(String enableVariable, boolean withTestMode, OptionalModule... requiredModules) {
        this(enableVariable, ModuleMode.TRUE, withTestMode, requiredModules);
    }

    OptionalModule(String enableVariable, ModuleMode defaultMode, boolean withTestMode, OptionalModule... requiredModules) {
        this.enableVariable = enableVariable;
        this.defaultMode = defaultMode;
        this.withTestMode = withTestMode;
        this.requiredModules = Set.of(requiredModules);
    }

    /** Environment variable that enables the module ("true", "false" or "test"); null for an implicit module. */
    public String getEnableVariable() {
        return enableVariable;
    }

    public boolean isImplicit() {
        return enableVariable == null;
    }

    /** Whether the module has a test implementation ({@link de.samply.annotations.ConditionalOnModuleTest}). */
    public boolean isWithTestMode() {
        return withTestMode;
    }

    /** Modules that must be enabled too when this one is "true" (directly; see {@link #requires}). */
    public Set<OptionalModule> getRequiredModules() {
        return requiredModules;
    }

    /** Whether this module needs the other one, directly or through a module it requires. */
    public boolean requires(OptionalModule module) {
        return requiredModules.contains(module) || requiredModules.stream().anyMatch(required -> required.requires(module));
    }

    /**
     * "true" unless its variable says otherwise: all modules were enabled by default before they became optional. An
     * implicit module is "true" when a module that requires it is, "false" otherwise. Stops the start on an invalid
     * value, and on "test" for a module without a test implementation.
     */
    public ModuleMode fetchMode(PropertyResolver propertyResolver) {
        if (isImplicit()) {
            return Arrays.stream(values()).anyMatch(module -> module.requiredModules.contains(this)
                    && module.fetchMode(propertyResolver) == ModuleMode.TRUE) ? ModuleMode.TRUE : ModuleMode.FALSE;
        }
        // Empty means not set, as with Spring's boolean values before: e.g. a compose file passing "${ENABLE_X}" while
        // the .env does not define ENABLE_X
        String value = propertyResolver.getProperty(enableVariable);
        ModuleMode mode = (value == null || value.isBlank()) ? defaultMode : ModuleMode.parse(enableVariable, value);
        if (mode == ModuleMode.TEST && !withTestMode) {
            throw new IllegalStateException(enableVariable + "=" + ModuleMode.TEST + ": module " + this
                    + " has no test mode");
        }
        return mode;
    }

    /** Enabled: "true" or "test". */
    public boolean isEnabled(PropertyResolver propertyResolver) {
        return fetchMode(propertyResolver) != ModuleMode.FALSE;
    }

    /** What switches the module on, for messages: its variable, or the modules that require it. */
    public String describeSwitch() {
        return isImplicit()
                ? "required by " + Arrays.stream(values()).filter(module -> module.requiredModules.contains(this))
                        .map(OptionalModule::name).collect(Collectors.joining(", "))
                : enableVariable;
    }

}
