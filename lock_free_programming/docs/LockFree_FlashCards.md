# Lock-Free Programming — Flash Cards

## How to use this in RemNote

Each card is a single bullet line with `>>` separating front (question) from back (answer). When you paste these bullets into a RemNote document, the `>>` automatically marks each bullet as a forward-only flashcard, and they're scheduled into the spaced-repetition queue.

If you want a card to be bidirectional (Q→A *and* A→Q), change `>>` to `::` after import. For cloze-deletion style, wrap the answer term in `{{...}}` inside the front.

Tip: paste each section under its own H2 heading in RemNote so the deck stays organized.

---

## Section 1 — The Concurrency Hierarchy

- What are the three layers of concurrency control on the JVM, from highest-level to lowest-level? >> (1) JVM-managed: `synchronized` / `ObjectMonitor` (C++). (2) Java-managed via AQS: `ReentrantLock`, `Semaphore`, etc., parking through `LockSupport`. (3) Hardware: `Atomic*` / `VarHandle` via CAS.
- Which layer does `synchronized` talk to, which does `ReentrantLock` talk to, and which do atomics talk to? >> `synchronized` → JVM internals (ObjectMonitor in C++). `ReentrantLock` → OS scheduler via `LockSupport.park/unpark`. `Atomic*` → CPU directly via CAS instructions.
- What is `LockSupport.park` / `LockSupport.unpark` and why is it preferred over `ObjectMonitor`'s parking? >> The OS-level park primitive AQS-based locks use. Its handoff is explicit and predictable — far tighter than the heuristic responsible-thread choreography inside the JVM's ObjectMonitor.

## Section 2 — `synchronized`, `ObjectMonitor`, Stranding

- What is "stranding" in `synchronized`? >> Threads remain parked while the lock itself sits unowned — between a release and the successor actually being scheduled by the OS, the lock is empty but no one is allowed to acquire. The gap (10 µs to several ms) is wasted wall time.
- What does an object's Mark Word store for locking state? >> Two-bit lock state: `01` unlocked (also stores hashCode/age), `00` lightweight locked (points to a stack record on the owner thread), `10` heavyweight locked (points to an ObjectMonitor). Plus biased-lock and GC age info.
- What is "lock inflation"? >> The Mark Word's transition from lightweight (stack-recorded) lock to heavyweight ObjectMonitor when a second thread contends. A new ObjectMonitor is allocated and the Mark Word is rewritten to point to it.
- Why is `synchronized` slower than an unfair `ReentrantLock` under contention? >> ObjectMonitor's C++ wakeup choreography (responsible-thread succession, adaptive spinning, safepoint interactions) leaves gaps where the lock is free but no one runs. ReentrantLock's AQS uses an explicit FIFO queue and a tight `LockSupport.unpark` handoff with no such gaps.
- 50M increments by 200 threads on one counter: roughly what's the ratio of `synchronized` time to unfair `ReentrantLock` time? >> About 8×. (`synchronized` ≈ 3.2 s vs `ReentrantLock` ≈ 0.4 s in the StrandingTest run.) The gap is cumulative stranding.

## Section 3 — `AbstractQueuedSynchronizer` (AQS)

- What does AQS manage internally? >> An integer `state` and a FIFO CLH-style wait queue of threads. Subclasses override `tryAcquire`/`tryRelease` (exclusive) or `tryAcquireShared`/`tryReleaseShared` (shared) to define the meaning of state.
- Difference between `tryAcquire(arg)` and `acquire(arg)` in AQS? >> `tryAcquire` is a protected hook the subclass overrides — it attempts the state transition once and returns true/false. `acquire` is the public final blocking entry: calls `tryAcquire`; on failure, enqueues and parks the thread until it can re-attempt.
- In AQS, when do you use `setState` vs `compareAndSetState`? >> `setState` (plain write) inside `tryRelease` — only the owner can release, so there's no contention. `compareAndSetState` (atomic) inside `tryAcquire` where multiple threads race for the 0→1 transition.
- What is the canonical bug when writing a custom AQS-based mutex? >> Calling `sync.tryAcquire(1)` from `lock()` instead of `sync.acquire(1)`. `tryAcquire` is a hook, not the blocking entry — contended threads fall through and the mutex provides no mutual exclusion.
- In an AQS-based `tryRelease`, what's the correct ownership check? >> `if (Thread.currentThread() != getExclusiveOwnerThread()) throw new IllegalMonitorStateException();` — identity comparison against the recorded owner, not a state == 0 check (which misses the wrong-thread-tries-to-release case).
- Why throw `IllegalMonitorStateException` rather than return `false` from a non-reentrant `tryRelease` when the caller isn't the owner? >> JDK convention. Returning `false` is reserved for partial release in reentrant locks ("not fully released yet"). For a binary mutex, "wrong thread is releasing" is a programming error and IMSE is the contract.

## Section 4 — Locks that don't use AQS

- Name three lock / lock-like primitives in Java that do **not** extend AQS. >> `synchronized` (JVM-native ObjectMonitor); `StampedLock` (its own CLH-style queue with 64-bit stamped state); any CAS-based spinlock built directly on `Atomic*` or `VarHandle`.
- Why does `StampedLock` not extend AQS? >> It needs optimistic reads (returning a stamp that's later validated), a 64-bit state, and doesn't need AQS's `Condition` machinery. AQS's state is 32-bit and its model assumes acquire/release semantics that don't fit optimistic reads.
- What's the alternative to AQS when you need a larger state than an int? >> `AbstractQueuedLongSynchronizer` — same idea as AQS but with a `long` state field.

## Section 5 — Conditions and Bounded Buffer

- Why must condition `await()` be inside `while (predicate)` not `if (predicate)`? >> Two reasons: (1) Spurious wakeups — the OS can unpark a thread without a signal. (2) Between signal and re-acquire, another waiter may have already consumed the condition. The while re-checks after re-acquiring.
- In a bounded buffer with two Conditions (`notFull`, `notEmpty`), why is `signal()` sufficient — when would `signalAll()` be required? >> Each Condition has only one role waiting on it (producers on `notFull`, consumers on `notEmpty`); waking any one waiter makes progress. With a single shared Condition, `signalAll()` is needed to avoid the lost-wakeup hazard where the wrong-role thread is woken.
- Why is logging inside a critical section a benchmarking trap? >> The lock is held across `Logger.log`, which formats, may acquire internal locks, and writes I/O. The lock's hold time is dominated by logging cost, not by the operation under test. Different lock implementations all look equally bad.

## Section 6 — `AtomicReference` and CAS

- What is the semantic of `AtomicReference.compareAndSet(expected, update)`? >> Atomically: if current == expected, write update and return true; else leave unchanged and return false. The check-and-write is a single uninterruptible CPU instruction (CMPXCHG on x86, LDXR/STXR on ARM).
- What is the "rebase loop" CAS idiom? >> Read current → derive new state as a pure function of current → CAS → retry if CAS lost. The thread never blocks; it just recomputes against the latest state until its CAS wins.
- What does `AtomicReference.updateAndGet(unaryOp)` do internally? >> Wraps the rebase loop. Reads current; applies the function; CAS's; retries on loss; returns the new value. The function may be invoked multiple times under contention, so it must be pure.
- In a Treiber stack, why must `Node` be immutable (final fields)? >> Safe publication. Once a Node is published via the head CAS, other threads may walk its `next` chain. Mutable fields could be observed half-written. Final fields plus the atomic head write give the JMM publication guarantee.
- In Treiber `push`, why construct the new Node BEFORE the CAS, not after winning a lock? >> There is no lock. Each push optimistically builds its candidate, then CAS's. If the CAS loses, the candidate is discarded and a new one built. The cost of contention is wasted allocation, not blocking.
- When should you use `AtomicLong` / `LongAdder` instead of `AtomicReference<Long>`? >> Always — they're specialized and faster. `AtomicLong` for moderately contended counters; `LongAdder` for high-contention counters (it stripes counts across multiple cells and sums on read, avoiding cache-line war).
- Why is `AtomicReference` insufficient for atomically updating multiple independent fields? >> CAS only orders accesses to one reference. To atomically change multiple fields, you bundle them into one immutable object held by the AtomicReference (extra allocation per update) — or you fall back to a Lock.
- What does it mean that lock-free is not "free"? >> Under sustained heavy contention, CAS loops spin, burn CPU, and cause cache-line bouncing between cores. A parking lock can outperform a CAS loop in that regime because threads sleep instead of fighting. Benchmark for your workload.

## Section 7 — Lock-Freedom Properties

- What is the formal definition of "lock-free"? >> The system as a whole always makes progress in a bounded number of steps. At least one thread completes its operation regardless of how slowly others run.
- What's the difference between lock-free and wait-free? >> Lock-free: system-wide progress (some thread always finishes). Wait-free: per-thread progress (every thread completes in a bounded number of its own steps). Wait-free is strictly stronger and much harder to achieve.
- Is lock-free the same as fast? >> No. Lock-freedom is a progress / liveness guarantee, not a throughput guarantee. CAS-based code can be slower than a lock-based one under high contention on a single hot atomic.
- What is the practical test for lock-freedom in a JUnit suite? >> "If any subset of threads stops participating mid-operation, the remaining threads must still complete in bounded time." A blocking lock can be violated by a thread exiting while holding the lock; CAS-based code cannot, because there's no held state to leak.
- What is the "linearization point" of a Treiber `push`? >> The successful `head.compareAndSet(...)` call. Before that point, no observer can see the new node; after it, the new node is the head. The whole operation appears to take effect at that instant.

## Section 8 — ABA Problem

- What is the ABA problem in lock-free code? >> Thread A reads value X, plans to CAS X→Y. In between, other threads change the state X→Z→X. Thread A's CAS sees X again and succeeds — but the world really did change. Logical invariants tied to history can be violated.
- Why is ABA rare in pure Java Treiber stacks specifically? >> `push` always allocates a fresh `Node`. The GC keeps popped nodes alive while any thread references them, but `push` will never reinstall the same `Node` object at head. CAS compares object identity, so a "second A" with a different Node reference makes the CAS fail.
- When does ABA still bite Java code? >> Object pooling (recycling Node references); counters / sequence numbers that wrap or reset; slab allocators that reuse addresses; any scheme where the same value or reference legitimately reappears.
- What's the standard fix for ABA, and how does it work? >> `AtomicStampedReference<V>`: pairs the reference with an integer stamp incremented on every mutation. A stale CAS with the right value but the wrong stamp is rejected. "A → B → A" becomes detectable as "stamp 0 → 1 → 2."
- What is `AtomicMarkableReference` and when is it preferred over `AtomicStampedReference`? >> Pairs a reference with a single boolean instead of an int counter. Cheaper. Useful when one bit is enough — typically "is this node logically deleted?" in lock-free linked-list designs.

## Section 9 — async-profiler / Profiling

- For investigating lock contention specifically, which async-profiler event is most direct: `cpu`, `wall`, or `lock`? >> `lock`. It uses JVMTI MonitorContendedEnter callbacks (event-driven, not signal-based), records every monitor contention with its Java stack, and doesn't suffer the signal-handler crashes the `wall`/`cpu` samplers hit on macOS arm64.
- What do `-XX:+DebugNonSafepoints` and `-XX:+PreserveFramePointer` do in profiling context? >> `DebugNonSafepoints` emits JIT debug info at non-safepoint locations so async-profiler samples don't snap to the nearest safepoint and skew the flamegraph. `PreserveFramePointer` keeps the frame-pointer register so the profiler can unwind through JIT-compiled and native frames.
- In async-profiler's command grammar, where must `file=` be placed: on `start`, on `stop`, or both? >> On `stop` (or on a separate `dump` command). It is a *dump* option, not a *start* option. Putting it on `start` is silently ignored, and at stop time the profiler reports "No dump options specified" and writes nothing.
- In a `lock`-event flamegraph for a `synchronized` block, why does the call stack stop at the Java boundary and not show `ObjectMonitor::enter`? >> The `lock` event captures only the Java stack at the point of contention via JVMTI. The actual parking happens in C++ (`ObjectMonitor::EnterI` → `pthread_cond_wait`), invisible to JVMTI. To see those frames you'd need a `wall` or `cpu` flamegraph.
- What is the meaning of "samples" in a `lock`-event flamegraph: time, count, or something else? >> Count of MonitorContendedEnter events — i.e., how many times a thread had to block to acquire the lock. It does not directly equal wall-time spent waiting; for time, compare overall wall-clock duration of the test run.

## Section 10 — Practical Heuristics

- Default rule for picking between `synchronized`, `ReentrantLock`, and `Atomic*`. >> `synchronized` for simple, low-contention code where you want one keyword. `ReentrantLock` when you need fairness, tryLock, multiple Conditions, or you're hitting `synchronized` stranding. `Atomic*` for high-frequency single-variable updates or building non-blocking structures.
- When is the right time to reach for `AtomicReference` for state larger than a primitive? >> When the state can be modelled as an immutable snapshot, updates are cheap to recompute, and readers don't need to lock. Counterexample: large objects with frequent partial updates — re-creating the whole object per update churns the allocator and hurts more than a lock would.
- For benchmarking concurrent code, what is the bare-minimum hygiene? >> (1) Run the workload long enough that JIT has compiled the hot paths (warm-up pass or use JMH). (2) Use `toNanos()`/`toMillis()`, not `toSeconds()`. (3) Assert correctness as well as time. (4) Avoid I/O (especially logging) inside the critical section. (5) Pin to a specific JDK and document it.
