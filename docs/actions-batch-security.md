# Actions batch: how the constraints of the endpoints are kept

**Status:** Findings and decision (2026-09-30)
**Decision:** The batch endpoint has no security logic of its own. It calls the protected controller method of each
entry, so the endpoint's constraints decide. Tests compare every kind of refusal with the endpoint called on its own.

## The question

`POST /actions/batch/results` lets a user call many read actions in one request (`ActionsBatchService`, see
`plans/2026-09-30-plan-batch-getter-endpoint.md`). A user who may not call an endpoint must get the same refusal when
they ask for it through the batch. Otherwise the batch would be a way around the security architecture.

## How an endpoint is protected

| Layer | Where | What it checks |
|---|---|---|
| Login | Security filter chain (`SecurityConfiguration`) | The user is authenticated. |
| URL rule | Security filter chain, `addAuthorityMapping` | The user has one of the organisation roles named in the endpoint's `@RoleConstraints`. Answers 403. |
| Role constraints | `RoleConstraintsAspect` | Organisation roles, and the user's role in the project (also per site and per phase). Answers 405, or 404 without a project. |
| State constraints | `StateConstraintsAspect` | The state of the project, of the site, of the query and of the user in the project. Answers 405 or 404. |
| Project constraints | `ProjectConstraintsAspect` | The kind of project (project type, query format) and who created a document. Answers 405 or 404. |

The three aspects run around the controller method. They need the method's resolved arguments: the project and the
site are loaded from the `project-code` and `bridgehead` parameters by the argument resolver and the converters.

## What runs for a batch entry

| Layer | For the batch request | For each entry |
|---|---|---|
| Login | yes | – (same user) |
| URL rule | only the batch's own URL, which asks for a login | **no** |
| Argument resolution | – | yes, the same resolvers and converters |
| Role, state and project constraints | – | **yes**, the same aspects |

The batch calls the controller method of an entry through Spring's proxy of the controller, with the arguments
resolved by the application's own resolvers (`InvocableHandlerMethod` with the resolvers of the
`RequestMappingHandlerAdapter`). This is the step Spring performs for the endpoint's own URL after the filter chain.
The aspects therefore run exactly as they do for the endpoint, and what an aspect answers becomes the entry's error.

Consequences:

- The batch contains **no list of rules** and no copy of a check. A constraint added to an endpoint, or a new kind of
  constraint aspect, applies to batch entries without touching the batch.
- A refused entry does not run its endpoint and carries no response.
- Only read actions can be entries: endpoints that are a GET and have a `@FrontendAction`. Every other action,
  including the batch action itself, is answered with 405 without calling anything.

## The URL rule does not run. Is that a gap?

No. The URL rule asks for the organisation roles of the endpoint's `@RoleConstraints`. `RoleConstraintsAspect` checks
the same roles when the method is called (`ConstraintsService.checkOrganisationRoleConstraints`). Both take the roles
from the same place: `GrantedAuthoritiesExtractor` maps the user's groups once and fills the Spring authorities (read
by the URL rule) and the session user's organisation roles (read by the aspect) together.

So a user who fails the URL rule also fails the aspect. The only difference is the status:

| | Endpoint on its own | As batch entry |
|---|---|---|
| User without the organisation role | 403 (filter chain, before the aspect is reached) | 405 (aspect) |

A first version of the batch repeated the URL rule to answer 403 as well. It was removed in the review: it was the
same check twice. The UI treats 403 and 405 alike ("not allowed").

**This equivalence is an assumption the batch relies on.** If the URL rule and the aspect ever take their roles from
different sources, or if `SecurityConfiguration` gets a rule that is not derived from `@RoleConstraints` (for example a
path that only some network or client may call), the batch needs the same rule. See "When to revisit".

## What is tested

### Automated: `ActionsBatchServiceTest`

A small test controller with one endpoint per kind of constraint, the real `ConstraintsService`, the three real
aspects, the real argument resolver and the real `ProjectConverter`. Services behind them (project lookup, role
mapping) are mocks that the test sets per case. Every case calls the endpoint on its own (MockMvc) and as a batch
entry, and requires the same status from both.

| Reason for refusal | Status | Also checked |
|---|---|---|
| The organisation role is missing | 405 | allowed once the user has it |
| No role in the project | 405 | |
| Another role in the project than the required one | 405 | allowed with the required role |
| The project is in another state | 405 | allowed in the right state |
| The project is of another kind (query format) | 405 | allowed with the right one |
| The project does not exist | 404 | |
| An endpoint with all constraints at once | 405 | each constraint alone refuses; allowed only when all are met |
| A required parameter is missing | 400 | |
| The action is not a read action | 405 | |
| The action does not exist | 404 | |

Also tested: a refused entry has no response; entries running in parallel each see their own project (request scope);
the user's roles are read from the session in the worker threads.

These tests fail if the batch ever calls a controller method without its aspects.

### Manual: comparison on the running application (2026-09-30)

For three users, every read action of the request view (50 actions, downloads left out) was called on its own endpoint
and as a batch entry, with the project and site of the page.

| User | Request | 200 | 400 | 403 | 404 | 405 |
|---|---|---|---|---|---|---|
| PM-Admin | TEST-2026-0005 (REVIEW) | 23 | 2 | 0 | 3 | 22 |
| Researcher (creator) | TEST-2026-0003 (DRAFT) | 26 | 2 | 1 | 3 | 18 |
| Bridgehead admin | TEST-2026-0004 (FINAL) | 21 | 2 | 1 | 2 | 24 |

The entry had the same status as the endpoint in all 150 cases, except the two 403 (`FETCH_NOTIFICATIONS` for the
researcher and the bridgehead admin), which are 405 as an entry, as explained above. The allowed entries had the same
body as the endpoint.

How it was done: in the browser, through the page's own backend service, which adds the login itself. For each action
from `all-actions`, one call to its path and one batch with all actions.

### Manual: other projects and sites than the user's own (2026-09-30)

The comparison above uses the project and site of the page. A batch entry can name any project and site, so this
second comparison asks for combinations the user is not meant to reach.

For each of the three users: 49 read actions (downloads and feasibility left out) × 6 projects (a finished one, two
drafts, one in REVIEW, one in FINAL, and a code that does not exist) × 4 sites (none, each of the two real sites, and
a site that does not exist) = **1176 entries in one batch**, each compared with its endpoint called on its own.

| User | 200 | 400 | 403 | 404 | 405 | 500 | Batch allows what the endpoint refuses | Different body |
|---|---|---|---|---|---|---|---|---|
| PM-Admin | 360 | 80 | 0 | 31 | 334 | 371 | 0 | 0 |
| Researcher | 398 | 80 | 24 | 34 | 281 | 359 | 0 | 0 |
| Bridgehead admin | 183 | 80 | 24 | 8 | 522 | 359 | 0 | 0 |

(The columns count the answers of the endpoints on their own.)

- **No entry was allowed that its endpoint refuses**, and none the other way round.
- **Every allowed entry had the body of its endpoint.** With 1176 entries for different projects and sites running in
  parallel in one request, this also shows that entries do not see each other's project or site.
- The status differed only for `FETCH_NOTIFICATIONS`, which needs the PM-Admin organisation role, for the two users
  without it (24 entries each). The endpoint's URL rule answers 403 before anything else happens. As an entry it is
  405 from the aspect (15), or 500 when the project or site of the entry does not exist (9), because the arguments are
  resolved before the aspect runs. All three are refusals.
- The 500 for a project or site that does not exist is what the endpoints answer on their own as well (359 to 371 of
  the single calls). It is not new with the batch, but it is a finding: an unknown project or site should be a 404.

This took the batch about half a second and the single calls about two minutes (the UI retries a failed GET twice).

## What is not covered

- **The real controller in an automated test.** The automated test uses a test controller; the 69 real read actions
  are covered by the manual comparison only. An automated version needs a test that starts the whole application with
  a database (an integration test). The project has one such test, `ProjectCodeManagerApplicationTests`, and it is
  disabled. See `plans/2026-09-30-plan-spring-and-library-update.md`, §5 and §8.
- **State constraints on sites, queries and users in a project** (`projectBridgeheadStates`, `queryStates`,
  `userProjectStates`) and the document creator check: they run through the same aspects, but have no test case of
  their own for the batch.
- **DataSHIELD requests:** the local test data has only export requests.

## When to revisit

- `SecurityConfiguration` gets a rule that is not derived from `@RoleConstraints`.
- The Spring authorities and the session user's roles stop coming from the same mapping.
- A constraint is implemented somewhere else than in an aspect around the controller method, for example in a servlet
  filter or an interceptor. Filters and interceptors do not run for batch entries.
- The batch is opened to actions that are not GET.
