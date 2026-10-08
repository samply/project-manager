# Optional modules

An optional module is a group of beans for one scope - the research environment, feasibility, sending emails - that a
deployment switches on or off with an environment variable. When a module is disabled, its beans are not created, so
the deployment needs none of its configuration (variables, files).

"Module" here means an application module in the sense of Spring Modulith: not a Java module (`module-info.java`, not
used in this backend) and not a frontend module (`@FrontendSiteModule`, a part of a page).

- Plan and decisions: `plans/2026-10-07-plan-optional-modules.md` (local, `plans/` is not in git).

## The modules

`OptionalModule` (`de.samply.modules`) lists them. The variable is `true` (the default), `false`, or `test` for a
module with a test implementation (`ModuleMode`); any other value stops the start.

| Module | Variable | Requires | What it does |
|---|---|---|---|
| `BEAM` | - (implicit) | - | Beam (`BeamService`), shared by exporter and feasibility; `BEAM_URL`, `BEAM_API_KEY`, `BEAM_PROJECT_MANAGER_ID` |
| `EXPORTER` | `ENABLE_EXPORTER` | `BEAM` | Sends the scheduled queries to the bridgeheads through the exporter and follows the exports; without it, queries stay "to be sent". Exporter templates, `EXPORTER_QUERY_LABEL_TEMPLATE` |
| `FEASIBILITY` | `ENABLE_FEASIBILITY` (also `test`) | `BEAM` | Feasibility queries to the bridgeheads through Beam, mapped with `FEASIBILITY_MAPPING`. `test`: random results from the template `TEST_FEASIBILITY_RESULT`, without Beam (development) |
| `RESEARCH_ENVIRONMENT` | `ENABLE_RESEARCH_ENVIRONMENT` | `EXPORTER` | Research environment workspaces, implemented with Coder; each workspace is registered as a Beam app (app register); the exporter transfers the export files into them |
| `DATASHIELD` | `ENABLE_DATASHIELD` | `RESEARCH_ENVIRONMENT` | DataSHIELD: Opal tokens through the token manager; every user gets a workspace |
| `EMAILS` | `ENABLE_EMAILS` | - | Sending emails through SMTP, including the `@EmailSender` emails of the controller; rendering the templates (`EmailService`) is always possible |

DataSHIELD implies the research environment, and the research environment the exporter - not the other way round. If
an enabled module requires a disabled one, the backend does not start and says which variable to change.

The **test mode** (`test`) replaces the module's real beans by its test beans (`@ConditionalOnModuleTest`), e.g. feasibility with
random results instead of Beam. A module in test mode needs none of its required modules: the test replaces the systems
they connect to. `test` on a module without a test implementation stops the start.

| Value | Created |
|---|---|
| `true` (default) | `@ConditionalOnModule` beans |
| `test` | `@ConditionalOnModuleTest` beans |
| `false` | `@ConditionalOnModuleDisabled` beans |

A bean needed in both the real and the test mode carries both annotations (`FeasibilityMapper`: the test results are
mapped like real ones). Only the real implementation is `@Primary` (for IntelliJ; at runtime only one exists).

An **implicit** module has no variable: it is enabled exactly when a module that requires it is `true` (`BEAM` with
`EXPORTER` or `FEASIBILITY`), so it can never be "required but disabled". A module may only require modules declared
before it in the enum.

Renamed 2026-10-07, without fallback: `ENABLE_CODER` is now `ENABLE_RESEARCH_ENVIRONMENT`, `ENABLE_TOKEN_MANAGER` is now
`ENABLE_DATASHIELD`, and `ENABLE_APP_REGISTER` is no longer read (app register is part of the research environment).
An old name left in a deployment is ignored, and the module is then enabled (the default).

At start-up the backend logs the enabled modules with their beans:

```
Enabled optional modules: BEAM (BeamService) [through EXPORTER, FEASIBILITY], EXPORTER (BeamExporterService, ...), FEASIBILITY (BeamFeasibilityService, FeasibilityMapper), EMAILS (...)
Enabled optional modules: FEASIBILITY [test] (FeasibilityMapper, TestFeasibilityService)
```

The disabled ones are logged at debug level. Spring's condition report (start with `--debug`) shows the decision for
every bean.

## Why custom annotations and not plain `@ConditionalOnBooleanProperty`

Decided 2026-10-08. `@ConditionalOnModule` / `@ConditionalOnModuleDisabled` are built on Spring's own mechanism (`@Conditional`): the
small `OptionalModuleCondition` turns the enum into the real Spring condition, and the result also appears in Spring's
condition report. Plain Spring (`@ConditionalOnBooleanProperty(name = ENABLE_X, matchIfMissing = true)` on each real
bean, `havingValue = false` on each disabled implementation) would do for a single module, but not for what the enum adds:

- the default ("enabled if unset") and the variable are stated once per module, not on every bean;
- dependencies between modules, checked at start (with disabled implementations, a wrong combination would otherwise start silently
  with the disabled implementation);
- implicit modules (BEAM), which plain Spring could only express as a `@ConditionalOnExpression` string repeating the
  defaults and the dependency;
- the one-line start-up log and `ModuleDependenciesTest`, which read the module directly from the annotation;
- the compiler checks the module name (a mistyped property name compiles and makes a bean that is always on).

Annotation values must be compile-time constants, so `"ENABLE_" + OptionalModule.X` cannot be used in an annotation
anyway; the enum keeps the variable name next to the module.

Names (decided 2026-10-08): Spring style, like `@ConditionalOnProperty` - `@ConditionalOnModule` (mode `true`),
`@ConditionalOnModuleDisabled` (`false`), `@ConditionalOnModuleTest` (`test`). They are conditions, not stereotypes: the
class still needs `@Service` / `@Component`. Formerly `@ModuleComponent`, `@ModuleStandIn` and `@ModuleTest`.

## Request types need their modules

Each request type (`ProjectType`) lists the modules it needs: `EXPORT`, `SAMPLES` and `SEQUENCING` need `EXPORTER`
(SEQUENCING until a SequencingService exists), `DATASHIELD` needs `DATASHIELD`, `RESEARCH_ENVIRONMENT` needs
`RESEARCH_ENVIRONMENT`. A type is available when all its modules are `true` or `test`
(`OptionalModules.isAvailable`). A deployment offers only what is available:

- **Start**: a type in `frontend-project-configs.json` whose modules are disabled stops the start, e.g. "configuration
  'DataSHIELD': Request type DATASHIELD requires module DATASHIELD (ENABLE_DATASHIELD=false)"
  (`ProjectConfigurationsFactory`).
- **Offered types**: the endpoint `fetchProjectTypes` returns only the available types.
- **Endpoints**: a request parameter of an unavailable type (single or in a list, also in an actions batch) is answered
  with 400 and the reason (`RequestVariableAndParameterMethodArgumentResolver`).
- **Requests created before** a type's module was disabled can be **viewed, not changed**: for them,
  `@ProjectConstraints(projectTypes = ...)` matches only endpoints that read (`@GetMapping`), so a finished request
  still shows its results, while actions that change something are neither offered nor accepted (`ConstraintsService`).
  A transition into or within an active state (create, accept, start develop/pilot/final) is refused
  (`ProjectEventService`): e.g. an archived request cannot be accepted again; rejecting, archiving and finishing stay
  possible.
- **Unfinished requests** (DRAFT to FINAL) that need a disabled module stop the start (`UnfinishedRequestsCheck`). The
  message lists them and gives the solution: enable the modules again and close the requests in the UI, or close them
  with the SQL in the message - `ARCHIVED` for REVIEW to FINAL, `REJECTED` for drafts (they cannot be archived). The
  state lives only in `samply.project.state` (the state machine is rebuilt from it), so the SQL is safe.

## How to make something part of a module

1. Put `@ConditionalOnModule(OptionalModule.X)` on every bean that only exists for module X: its service implementation,
   its jobs, its configuration. Spring creates them only when X is enabled.
2. Code outside the module depends on an **interface**, never on the implementation. Next to the real implementation
   (`@ConditionalOnModule(X)`) there is a **disabled implementation** (`@ConditionalOnModuleDisabled(X)`) that implements the same interface
   and is created when the module is disabled. The real implementation also carries `@Primary`: at runtime only
   one of the two exists, but IntelliJ does not evaluate the module condition (neither ours nor Spring Boot's
   `@ConditionalOnBooleanProperty`, tried 2026-10-07) and would otherwise report "Could not autowire. There is more
   than one bean". `ModuleDependenciesTest` checks it. Per method it does nothing (side effects, logged at debug), returns a neutral value (answers the
   UI shows), or throws `IllegalStateException` (calls that are a programming error when the module is disabled).
3. For a new module: side effects are better triggered by events (`@EventListener` in the module) than by calls from
   outside - a disabled module then has no listener, and needs no disabled implementation for them.
4. Add the module to `OptionalModule`, with its variable and the modules it requires, and to the table above.

Example: `FeasibilityService` (interface), `BeamFeasibilityService` (`@ConditionalOnModule(FEASIBILITY)`) and
`DisabledFeasibilityService` (`@ConditionalOnModuleDisabled(FEASIBILITY)`). Use `@ConditionalOnModuleDisabled` for the disabled implementation, not
`@ConditionalOnMissingBean`: Spring only supports the latter reliably in auto-configuration, not on component-scanned
classes. Whether a module is enabled (e.g. for the frontend): `OptionalModules.isEnabled(module)`.

`EXPORTER` shows the event way: `ProjectBridgeheadService` publishes `SendQueryToBridgeheadEvent`, and
`ExporterJobTrigger` (in the module) listens. When the module is disabled there is no listener - nothing to stand in for.
The interface `ExporterService` has every export operation (send, execute, follow the export, execution ID, templates,
transfer of the export file into a workspace), not only what today's callers use: `BeamExporterService` implements it,
`DisabledExporterService` stands in (no templates; the job's operations and the transfer are programming errors without
the module). Everyone, the module's own job included, depends on the interface.

`RESEARCH_ENVIRONMENT`: the interface `ResearchEnvironmentService` lives in the neutral package
`de.samply.researchenvironment` (with its disabled implementation), the Coder implementation `CoderResearchEnvironmentService` in
`de.samply.coder` - another implementation can replace Coder without touching the callers.

`ModuleDependenciesTest` checks the whole codebase: a class of module X may only be injected by classes of X or of a
module that requires X (directly or through other modules). Everything else must use the module's interface. It finds a
forgotten dependency without starting the backend. It also reports the opposite: a class outside every module that only
module classes inject - it probably belongs to one of them.

`DATASHIELD`: the interface `DataShieldService` has every DataSHIELD operation (tokens, their status, the bridgeheads
they cover, project status, authentication script); the controller and the DataSHIELD job use it. The disabled implementation answers
INACTIVE and has no script and no bridgeheads; the job's token operations fail without the module.

The interface of a module holds every operation that belongs to its concept, not only what today's callers use: other
code may need them later, and the module's own classes depend on the interface too.

`EMAILS`: `EmailService` only renders the templates (always there: the Credentials Sharing Tool shows rendered
templates without sending them); sending goes through `EmailSendingService` (`SmtpEmailSendingService` or
`DisabledEmailSendingService`, which logs the email). The SMTP beans (`MailSenderConfiguration`) belong to the module,
so the SMTP settings are only needed with emails enabled. So do `EmailSenderAspect` (the `@EmailSender` annotations
do nothing with emails disabled), `AttachmentFileService` and the email executor (`@ConditionalOnModule` on its `@Bean`
method). (The interface is not called `EmailSender`: that name is
taken by the annotation `@EmailSender`.)

Status (2026-10-08): all modules use the mechanism; BEAM is implicit; FEASIBILITY has a test mode.
