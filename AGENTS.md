# project-manager (backend)

## Optional modules

Functionality that a deployment can switch off (exporter, feasibility, research environment, DataSHIELD, emails,
external execution) is an optional module: `OptionalModule` in `de.samply.modules`, one `ENABLE_*` variable each
(`true`, `false`, or `test` where a test implementation exists; unset = `false`, modules are opt-in). Read
`docs/optional-modules.md` before changing or adding one. In short:

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

## Operations: read `project-manager-ops` first

What spans repositories lives in the private GitLab repository `project-manager-ops` (GitLab group `teiler`; clone it
next to this repository, as `../project-manager-ops`): inventory of projects, servers and images
(`inventory.yaml`), runbooks (`runbooks/`), shared plans (`plans/`), decisions (`decisions.md`) and the backlog.
New plans go to its `plans/`.

- Before creating a release or hotfix branch, changing the version, tagging, or merging into `main`/`develop`: read
  `../project-manager-ops/CLAUDE.md`, `runbooks/release.md` and `decisions.md`, follow the runbook step by step and
  say which step you are on.
- If `../project-manager-ops` is missing or you cannot read it: stop and ask the developer. Do not improvise release
  or hotfix steps.
- `main` (production) and `develop` (test) change only through pull requests; never push to them directly. Create
  branches from a remote branch only with `--no-track` (`git switch --no-track -c <branch> origin/develop`) and push
  new branches with `git push -u origin <branch>`.
