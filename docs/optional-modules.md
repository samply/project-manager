# Optional modules

An optional module is a group of beans for one scope - the research environment, feasibility, sending emails - that a
deployment switches on or off with an environment variable. When a module is disabled, its beans are not created, so
the deployment needs none of its configuration (variables, files).

"Module" here means an application module in the sense of Spring Modulith: not a Java module (`module-info.java`, not
used in this backend) and not a frontend module (`@FrontendSiteModule`, a part of a page).

- Plan and decisions: `plans/2026-10-07-plan-optional-modules.md` (local, `plans/` is not in git).

## The modules

`OptionalModule` (`de.samply.modules`) lists them. All are enabled unless their variable is `false`.

| Module | Variable | Requires | What it does |
|---|---|---|---|
| `RESEARCH_ENVIRONMENT` | `ENABLE_RESEARCH_ENVIRONMENT` | - | Research environment workspaces, implemented with Coder; each workspace is registered as a Beam app (app register) |
| `DATASHIELD` | `ENABLE_DATASHIELD` | `RESEARCH_ENVIRONMENT` | DataSHIELD: Opal tokens through the token manager; every user gets a workspace |
| `EXPORTER` | `ENABLE_EXPORTER` | - | Sends the scheduled queries to the bridgeheads through the exporter and follows the exports; without it, queries stay "to be sent" |
| `FEASIBILITY` | `ENABLE_FEASIBILITY` | - | Feasibility queries to the bridgeheads through Beam |
| `EMAILS` | `ENABLE_EMAILS` | - | Sending emails |

DataSHIELD implies the research environment; the research environment works without DataSHIELD. If an enabled module
requires a disabled one, the backend does not start and says which variable to change.

Renamed 2026-10-07, without fallback: `ENABLE_CODER` is now `ENABLE_RESEARCH_ENVIRONMENT`, `ENABLE_TOKEN_MANAGER` is now
`ENABLE_DATASHIELD`, and `ENABLE_APP_REGISTER` is no longer read (app register is part of the research environment).
An old name left in a deployment is ignored, and the module is then enabled (the default).

At start-up the backend logs the enabled modules with their beans:

```
Enabled optional modules: EXPORTER (ExporterJob), FEASIBILITY (FeasibilityServiceImpl), EMAILS (...)
```

The disabled ones are logged at debug level. Spring's condition report (start with `--debug`) shows the decision for
every bean.

## How to make something part of a module

1. Put `@ModuleComponent(OptionalModule.X)` on every bean that only exists for module X: its service implementation,
   its jobs, its configuration. Spring creates them only when X is enabled.
2. Code outside the module depends on an **interface**, never on the implementation. Next to the real implementation
   (`@ModuleComponent(X)`) there is a **disabled stand-in** (`@ModuleStandIn(X)`) that implements the same interface
   and is created when the module is disabled. Per method it does nothing (side effects, logged at debug), returns a neutral value (answers the
   UI shows), or throws `IllegalStateException` (calls that are a programming error when the module is disabled).
3. For a new module: side effects are better triggered by events (`@EventListener` in the module) than by calls from
   outside - a disabled module then has no listener, and needs no stand-in for them.
4. Add the module to `OptionalModule`, with its variable and the modules it requires, and to the table above.

Example: `FeasibilityService` (interface), `BeamFeasibilityService` (`@ModuleComponent(FEASIBILITY)`) and
`DisabledFeasibilityService` (`@ModuleStandIn(FEASIBILITY)`). Use `@ModuleStandIn` for the stand-in, not
`@ConditionalOnMissingBean`: Spring only supports the latter reliably in auto-configuration, not on component-scanned
classes. Whether a module is enabled (e.g. for the frontend): `OptionalModules.isEnabled(module)`.

`EXPORTER` shows the event way: `ProjectBridgeheadService` publishes `SendQueryToBridgeheadEvent`, and
`ExporterJobTrigger` (in the module) listens. When the module is disabled there is no listener - nothing to stand in for.

`RESEARCH_ENVIRONMENT`: the interface `ResearchEnvironmentService` lives in the neutral package
`de.samply.researchenvironment` (with its stand-in), the Coder implementation `CoderResearchEnvironmentService` in
`de.samply.coder` - another implementation can replace Coder without touching the callers.

`ModuleDependenciesTest` checks the whole codebase: a class of module X may only be injected by classes of X or of a
module that requires X. Everything else must use the module's interface. It finds a forgotten dependency without
starting the backend.

Status (2026-10-07): moved onto the mechanism: FEASIBILITY, EXPORTER, RESEARCH_ENVIRONMENT (and the DataSHIELD job).
Next: DATASHIELD (token manager service), EMAILS.
