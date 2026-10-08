# project-manager (backend)

## Optional modules

Functionality that a deployment can switch off (exporter, feasibility, research environment, DataSHIELD, emails,
external execution) is an optional module: `OptionalModule` in `de.samply.modules`, one `ENABLE_*` variable each
(`true`, `false`, or `test` where a test implementation exists). Read `docs/optional-modules.md` before changing or
adding one. In short:

- A bean that only exists for module X carries `@ConditionalOnModule(X)`; its test implementation
  `@ConditionalOnModuleTest(X)`; the implementation used while X is disabled `@ConditionalOnModuleDisabled(X)`.
  These are conditions, not stereotypes: the class still needs `@Service` / `@Component`.
- Code outside the module depends on the module's interface, never on an implementation. The interface holds every
  operation of the concept, not only what today's callers use. The real implementation is `@Primary` (IntelliJ does
  not evaluate the conditions).
- An endpoint without a bean of the module behind it carries `@RequiresModule(X)`.
- Each request type (`ProjectType`) lists the modules it needs; a type is only offered and accepted when they are
  enabled.
- New request states must be added to `UnfinishedRequestsCheck.NOT_FINISHED` and, if they activate a request, to
  `ProjectEventService.ACTIVATING_EVENTS`.
- `ModuleDependenciesTest` checks the wiring for every mode; keep it green.
