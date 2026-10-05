# Actions batch: why it is safe

**Status:** Findings and decisions (2026-09-30, extended 2026-10-06)
**Decision:** The batch endpoint has no security logic of its own. It calls the protected controller method of each
entry, so the endpoint's constraints decide. Tests compare every kind of refusal with the endpoint called on its own.
On top of that, the batch limits what it accepts and how much it runs at once (2026-10-06).

## Summary

`POST /actions/batch/results` runs several read actions of the frontend in one request. It is safe because:

| Concern | How it is handled | Where |
|---|---|---|
| **Authorization** | Every entry calls the endpoint's controller method through Spring's proxy, so the same role, state and project constraints run and refuse the same way. The batch has no rules of its own. Details below. | `ActionsBatchService.invoke` |
| **Only read actions** | An entry must be a GET endpoint with a `@FrontendAction`. Any other action is answered with 405, unknown ones with 404, without calling anything. GET endpoints do not write (audited 2026-10-01). | `ActionsBatchService.runEntry` |
| **No nested batches** | The batch endpoint is a POST, so it is never a read action: an entry naming it is refused with 405. | same; test `refusesANestedBatch` |
| **Only JSON answers** | A batch answers only JSON. An entry whose endpoint answers with a file (a `Resource`, `byte[]` or stream, or a content type other than JSON or text) is refused with 406: no file content reaches the batch. The endpoint has run by then (a download has read its file) - the same work the user may cause by calling it directly, bounded by the size of a batch and the thread pool. | `ActionsBatchService.toResult` / `isBinary`; test `refusesWhatIsNotAJsonReadAction` |
| **Size of a batch** | A batch with more than `ACTIONS_BATCH_MAX_ENTRIES` entries (default 100) is refused as a whole with 413 before any entry runs. The frontend sends a few dozen at most (opening a request: about 20 entries in 4 batches). | `ProjectManagerController.fetchActionsBatch`; test `refusesABatchWithTooManyEntriesBeforeRunningAny` |
| **Load** | All batches share one thread pool (`ACTIONS_BATCH_CORE_POOL_SIZE` / `_MAX_POOL_SIZE`, 10 by default): at most that many entries run at once, so batches take at most half of the 20 database connections. When the pool and its queue are full, an entry runs on the thread of its own batch request instead of failing, so overload slows the caller down rather than breaking the page. | `ProjectManagerAsyncConfiguration`; `actions-batch-concurrency.md` |
| **Entries outliving the request** | The batch waits for all its entries before it answers, so no entry works on a request object the server has already reused. | `actions-batch-concurrency.md` |
| **Hanging entries** | The waits that can block are bounded below the batch: getting a database connection (30 s, `hikari.connection-timeout`) and calls to other services (the `WebClientFactory` timeouts). | |
| **Clean worker threads** | After each entry the request context and the login are removed from the worker thread, also when a request-scoped bean fails while being destroyed. | `ActionsBatchService.invoke`; test `cleansTheWorkerThreadEvenWhenARequestScopeCallbackFails` |
| **Nothing else guards the endpoints** | Checked 2026-10-06: besides the login filter chain and the three constraint aspects there are only `CachePolicyFilter`, `RequestBodyCachingFilter` and `RequestCacheFilter` (cache headers and request body caching) and no `HandlerInterceptor`. So nothing that only runs for a direct call is skipped by an entry. | see "When to revisit" |
| **Session** | The entries share the session of the batch request and only read it (the session user and its roles); none writes it. | tests on the roles read in the worker threads |
| **Error answers** | An entry answers what its endpoint answers: for an error its status, and for a 500 the stack trace the endpoint puts in its body. An exception thrown before the controller method answers (for example while the arguments are resolved, as for an unknown project or bridgehead) is also answered with its stack trace, where the endpoint called directly gets Spring's error answer without one. Accepted (2026-10-06): the code is open source, so a stack trace reveals nothing secret; the same holds for all endpoints. | `ActionsBatchService.toErrorResult` |

### Left to the deployment

- **Size of the request body** and **slow uploads**: like every other JSON endpoint, the batch accepts what the
  server accepts. Limit the body size and the read timeouts in the reverse proxy (e.g. nginx `client_max_body_size`),
  for all endpoints at once.
- **Requests per user and time**: if needed, at the reverse proxy, for all endpoints. The application has a single
  backend instance per deployment and logged-in users only.

### Considered and left out (2026-10-06)

A first hardening (branch `feat/actions-batch-security`) added more. It was reduced to the table above:

| Proposal | Why it was left out |
|---|---|
| A rate limit per user (actions per minute) | It would break the page: while a query is on its way the request view reloads every 5 seconds, about 20 entries each, plus a few per additional site; with many sites or a few open tabs a user would get 429 and an empty page. It also needs a registry of users in memory. Better at the reverse proxy if ever needed. |
| A limit of concurrent batches | The thread pool already bounds the work. |
| Refusing entries when the pool is full (503), smaller queue | Undoes the decision of `actions-batch-concurrency.md`: under load the page would break instead of slowing down. |
| A deadline per batch with cancelled entries (504) | Needs entries that can outlive the batch, and with it a copy of the request (below). The blocking waits are already bounded (see "Hanging entries"); cancelling is cooperative anyway. |
| A detached copy of the request built by reflection | Fragile: every request method not copied would fail at run time, only inside a batch. Only needed for the deadline. |
| A read-only transaction with a timeout per entry | Changes how every endpoint runs inside a batch compared to the same endpoint on its own (transactions, lazy loading). A database timeout belongs in the global configuration. |
| A body size filter for the batch only | All JSON endpoints have the same exposure; the reverse proxy limits it for all. |
| Marking downloads `batchAllowed = false` to refuse them before they run | A second mechanism next to the check of the answer, which stays anyway as the rule "only JSON"; every new file action would have to remember it. It only saves work a user may cause anyway by calling the downloads directly. If files ever get too large to read into memory, stream them in the endpoints. |

## Authorization: how the constraints of the endpoints are kept

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
bridgehead are loaded from the `project-code` and `bridgehead` parameters by the argument resolver and the converters
(`ProjectConverter`, `ProjectBridgeheadConverter`; both are used by the automated test).

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
  including the batch action itself, is answered with 405 without calling anything. An entry whose endpoint
  answers with a file (a download) is answered with 406 after the call: its content never reaches the batch.

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
| The bridgehead is in another state (`projectBridgeheadStates`) | 405 | also without a bridgehead; allowed in the right state |
| The query at the bridgehead is in another state (`queryStates`) | 405 | allowed in the right state |
| The user is not invited at the bridgehead, or in another state there (`userProjectStates`) | 405 | allowed in the right state; the creator passes with the project's results state |
| The bridgehead role is held at another bridgehead, or no bridgehead is sent | 405 | allowed at the user's own bridgehead |
| Someone else's document (`documentCreatorOrProjectManagerAdmin`) | 405 | allowed for the creator of the document |
| A required parameter is missing | 400 | |
| The action is not a read action | 405 | |
| The action does not exist | 404 | |

Also tested: a refused entry has no response; entries running in parallel each see their own project (request scope);
the user's roles are read from the session in the worker threads; a download is refused (406); a nested batch is
refused; the worker thread is clean after an entry whose request-scoped bean fails on destruction.

### Automated: `ProjectManagerControllerBatchTest`

A batch with more entries than allowed is refused with 413 before any entry runs.

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

What the batch does not protect against, what was decided to leave, and what has not been tested (as of 2026-10-06).

### Not protected by the batch (left to the deployment or accepted)

- **Size of the request body and slow uploads:** limited by the server and the reverse proxy, as for every endpoint
  (see "Left to the deployment").
- **Requests per user and time:** no rate limit in the application; at the reverse proxy if ever needed (see
  "Considered and left out" for why not in the batch).
- **Work of a refused download:** a download in a batch runs (reads its file, renders its PDF) before it is refused
  with 406. Bounded by the size of a batch and the thread pool; the same work the user may cause directly.
- **Cancelling a hanging entry:** the batch waits for all its entries. An entry is only bounded by the timeouts below
  it (database connection, calls to other services); a controller method that hangs for another reason keeps its
  worker thread and the batch request waiting.
- **Stack traces in error answers:** accepted, see the summary ("Error answers").

### Findings outside the batch (all endpoints)

- **Unknown project or bridgehead answers 500 instead of 404:** the converters (`ProjectConverter`,
  `ProjectBridgeheadConverter`) throw while the arguments are resolved. Found in the comparison of 2026-09-30; the same
  for the endpoints called directly.
- **Stack traces in every 500 answer** (`createInternalServerError` of the controller). Accepted for the same reason as
  above.

### Not tested

- **The real controller in an automated test.** The automated test uses a test controller with one endpoint per kind
  of constraint; the 69 real read actions are covered by the manual comparisons only. An automated version needs a test
  that starts the whole application with a database (an integration test). The project has one such test,
  `ProjectCodeManagerApplicationTests`, and it is disabled. See `plans/2026-09-30-plan-spring-and-library-update.md`,
  §5 and §8, and `plans/2026-09-30-plan-unit-and-integration-tests.md`.
- **DataSHIELD requests:** the local test data has only export requests (no project type constraint for DataSHIELD
  was compared on the running application).
- **Load:** no load or stress test. The limits (entries per batch, thread pool, queue, waiting on the caller's thread)
  are tested only in their logic, not under real load or against a real database.
- **Several backend instances:** the application runs one backend per deployment; nothing was checked for more.
- **The reverse proxy:** no deployed proxy configuration was checked (body size, timeouts).

## When to revisit

- `SecurityConfiguration` gets a rule that is not derived from `@RoleConstraints`.
- The Spring authorities and the session user's roles stop coming from the same mapping.
- A constraint is implemented somewhere else than in an aspect around the controller method, for example in a servlet
  filter or an interceptor. Filters and interceptors do not run for batch entries (checked 2026-10-06: none guards
  the endpoints).
- An endpoint writes the session, or a GET endpoint starts writing data: entries run in parallel and share the
  session.
- The batch is opened to actions that are not GET.
- Files get large enough that reading them into memory hurts: stream them in the download endpoints (this also
  helps the direct calls), rather than excluding them from the batch.
- The frontend starts sending much larger batches (more than `ACTIONS_BATCH_MAX_ENTRIES`), or more than one backend
  instance runs per deployment.
