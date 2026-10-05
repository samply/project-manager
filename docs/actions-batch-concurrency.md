# Actions batch: how the entries run in parallel

**Status:** Decision (2026-09-30)
**Decision:** The entries of an actions batch run on one shared thread pool and the batch waits for them with
`CompletableFuture.join()`. Move to structured concurrency once it is a final Java feature.

## Context

`POST /actions/batch/results` runs several read actions in one request (`ActionsBatchService`, see
`plans/2026-09-30-plan-batch-getter-endpoint.md`). A page sends 30 to 40 entries at once. Two things must hold:

1. **A limit on how many entries run at once.** Every entry reads from the database, and the pool has 20 connections
   (`hikari.maximum-pool-size`). Without a limit, one page load could take all of them and block every other user.
2. **The batch must not answer before its entries have finished.** An entry uses the batch request (session, headers).
   The server reuses that request object once the batch has answered; an entry still running then would work on
   someone else's request.

Each entry also needs the caller's login and its own request context on its thread, because Spring keeps both per
thread (`SecurityContextHolder`, `RequestContextHolder`). This is the same for every option below.

## Options

| Option | Limit (1) | Waits for all entries (2) | Assessment |
|---|---|---|---|
| `Future` + `get()` | The pool | Only with a retry loop: `get()` stops waiting when the thread is interrupted | The first implementation. Correct, but the loop is code that is easy to get wrong. |
| **`CompletableFuture` + `join()`** | The pool | Yes: `join()` keeps waiting through an interrupt | **Chosen.** Spring's executor returns it directly (`submitCompletable`). A few lines, final API. |
| `ExecutorService.invokeAll` | The pool | No: it ends on an interrupt and cancels the entries | Spring's `ThreadPoolTaskExecutor` does not expose it directly either. |
| Virtual threads + semaphore | A semaphore, written by us | With `join()`, as above | See below. |
| Structured concurrency (`StructuredTaskScope`) | A semaphore, written by us | Yes, by construction | See below. The best fit, but not final. |
| `@Async` methods | The pool | With `join()`, as above | The same `CompletableFuture` underneath, plus a proxy call. No gain. |
| Parallel streams | None of our own | Yes | They run on the JVM's shared pool, which must not be blocked with database calls. |

### Virtual threads + semaphore

Virtual threads cost almost nothing, so one can be started per entry without a pool. They pay off when thousands of
tasks wait at the same time.

Our limit is not threads but database connections. We would start one virtual thread per entry and make most of them
wait at once on a semaphore with 10 permits. That is a thread pool of 10 built from two parts instead of one. The
hand-over of login and request context stays the same. At this size there is no measurable gain, and the limit becomes
code of ours instead of configuration.

### Structured concurrency

`StructuredTaskScope` starts subtasks and ends only when all of them have ended. Requirement 2 is then guaranteed by
the language construct instead of by our call to `join()`, and cancelling the entries of a batch is built in.

Why not now:

- It is a **preview feature**: in Java 25 (fifth preview), 26 (sixth) and 27 (seventh). The proposal is to make it
  final in Java 28 (March 2027).
- Preview features need `--enable-preview` when compiling, in the tests and on the server, and the compiled classes
  run only on exactly that Java version.
- The API still changes between versions. In Java 27 methods were renamed and a type parameter was added, so code
  written for 25 does not compile on 27.

Two things stay the same even with it: it starts virtual threads and knows nothing about our pool, so the limit needs
a semaphore; and Spring's per-thread login and request context still have to be handed to each subtask.

## Decision

One shared thread pool (`actions-batch` in `ProjectManagerAsyncConfiguration`) and `CompletableFuture.join()`.

- The pool is the limit, for all users together. Defaults: 10 threads (half of the database pool), a queue of 1000.
  Configured like the other executors: `ACTIONS_BATCH_CORE_POOL_SIZE`, `ACTIONS_BATCH_MAX_POOL_SIZE`,
  `ACTIONS_BATCH_QUEUE_CAPACITY`.
- A batch may have any number of entries; the ones beyond the threads wait in the queue. If the queue is full too, an
  entry runs on the thread of its own batch request instead of being rejected.
- No timeout per entry, because of requirement 2. The single endpoints have none either.

## Projects and bridgeheads: loaded once per batch

Most entries of a batch name the same project and bridgehead (`project-code`, `bridgehead`), and each entry converted
them again: one database query per entry for the project and one for the bridgehead. `ActionsBatchLookups` keeps them
for one batch:

- The batch creates one `ActionsBatchLookups` (two `ConcurrentHashMap`s: projects by code, bridgeheads by project
  code and bridgehead) and gives it to every entry request as a request attribute; the entries' other attributes stay
  their own.
- `ProjectConverter` and `ProjectBridgeheadConverter` use it when the current request is a batch entry
  (`ActionsBatchLookups.current()`), and load as before otherwise. Only the conversion is cached: `ProjectService` and
  `ProjectBridgeheadService` are unchanged.
- `computeIfAbsent`: entries asking at the same time wait for one load. A load that fails (an exception, or a project
  that is not found) is not kept, so each entry tries again and answers as before.
- The lookups end with the batch. Nothing is kept across requests, not even in the same session; requests outside a
  batch and background jobs never use them. No Hibernate cache is involved (a general look at the database access is
  `plans/2026-10-06-plan-database-access-optimization.md`).

**Assumption:** the entries share the same entity objects (detached, as the converters always returned them, since
`open-in-view` is false). That is safe because a batch only runs read actions (GET) that do not change them. If an
entry ever changed a project or bridgehead in memory, the other entries of the batch would see it.

Tests (`ActionsBatchServiceTest`): a batch of 8 entries loads the project and the bridgehead once; nothing is kept
across batches or for the endpoints called on their own; a project that is not found is looked up again.

**Measured on the running application (2026-10-06).** Batches sent directly to `POST /actions/batch/results`, every
entry `FETCH_PROJECT_ROLES` for the same project and bridgehead (TEST-2026-0004, `jurassic-park`), as project manager
admin. Counted: the reads of the two tables in PostgreSQL (`pg_stat_user_tables`, sequential / index scans), read 15
seconds after each batch because PostgreSQL publishes the counters of an idle connection with a delay.

| Batch | `project` (seq / index) | `project_bridgehead` (seq / index) |
|---|---|---|
| 1 entry | 4 / 2, then 3 / 2 | 1 / 0, then 1 / 0 |
| 20 entries | 3 / 2, then 7 / 6 | 1 / 0, then 2 / 1 |
| no batch, 15 s | 1 / 0 | 0 / 0 |

A batch of 20 entries reads the tables about as often as a batch of 1; without the lookups every entry loaded the
project and the bridgehead itself (about 20 more reads of each table). The run with 7 / 6 is within the noise of the
background reads (the line without a batch). All entries answered as before (200).

Also checked in the browser: the request view of TEST-2026-0004 as project manager admin and as creator is unchanged
(next steps, More actions, bridgehead overview, documents, results), without errors.

## Proposal: move to structured concurrency when it is final

**When:** with the move to the first LTS version of Java in which structured concurrency is final. If it becomes final
in Java 28 as proposed, that is Java 29 (expected September 2027). The project stays on Java 25 until then.

**What changes:** only `ActionsBatchService.fetchActionsBatch`. The entries are forked in a `StructuredTaskScope`
instead of submitted to the executor, with a semaphore sized like today's pool. `runEntry`, the request and security
hand-over, and the endpoint stay as they are.

**What it gives:** requirement 2 no longer depends on a comment explaining why `join()` and not `get()`; and a batch
whose client has gone away can cancel its remaining entries.

**Check before moving:**

- that the feature is final in that version (no `--enable-preview`);
- whether Spring then offers its own way to carry the security and request context into subtasks (for example through
  scoped values), which would remove our manual hand-over;
- `ActionsBatchServiceTest` covers the behaviour that must not change: parallel entries with their own request scope,
  order of results, one failing entry not affecting the others.

## When to revisit earlier

- The batch pool becomes a bottleneck (entries waiting in the queue while the database pool has free connections).
  First raise the pool sizes; they are configuration.
- A batch needs to be cancelled when its client disconnects. That is the one thing the current solution cannot do.
