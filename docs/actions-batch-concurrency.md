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
