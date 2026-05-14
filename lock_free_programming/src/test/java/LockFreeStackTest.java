import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LockFreeStackTest {
    private static final Logger log = Logger.getLogger(LockFreeStackTest.class.getName());

    @Test
    void single_threaded_lifo_ordering() {
        LockFreeStack<Integer> stack = new LockFreeStack<>();
        assertNull(stack.pop(), "pop on empty stack must return null");
        stack.push(1);
        stack.push(2);
        stack.push(3);
        assertEquals(3, stack.pop());
        assertEquals(2, stack.pop());
        assertEquals(1, stack.pop());
        assertNull(stack.pop());
    }

    @Test
    @Timeout(30)
    void concurrent_pushes_lose_no_items() throws Exception {
        var threads = Runtime.getRuntime().availableProcessors();
        var perThread = 100_000;
        var total = threads * perThread;
        var stack = new LockFreeStack<Integer>();
        var start = new CountDownLatch(1);
        var done = new CountDownLatch(threads);

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int t = 0; t < threads; t++) {
                final int tid = t;
                pool.execute(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < perThread; i++) {
                            stack.push(tid * perThread + i);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            done.await();
        }

        Set<Integer> popped = new HashSet<>();
        Integer v;
        while ((v = stack.pop()) != null) {
            assertTrue(popped.add(v), "duplicate value popped: " + v);
        }
        assertEquals(total, popped.size(),
                "lost items under concurrent push: expected " + total + " got " + popped.size());
    }

    @Test
    @Timeout(30)
    void mixed_producers_and_consumers_preserve_multiset() throws Exception {
        int producers = 4, consumers = 4, perProducer = 50_000;
        int total = producers * perProducer;
        var stack = new LockFreeStack<Integer>();
        Set<Integer> consumed = ConcurrentHashMap.newKeySet();
        var consumedCount = new AtomicInteger();
        var firstError = new AtomicReference<Throwable>();
        var done = new CountDownLatch(producers + consumers);

        try (ExecutorService pool = Executors.newFixedThreadPool(producers + consumers)) {
            for (int p = 0; p < producers; p++) {
                final int pid = p;
                pool.execute(() -> {
                    try {
                        for (int i = 0; i < perProducer; i++) {
                            stack.push(pid * perProducer + i);
                        }
                    } catch (Throwable t) {
                        firstError.compareAndSet(null, t);
                    } finally {
                        done.countDown();
                    }
                });
            }
            for (int c = 0; c < consumers; c++) {
                pool.execute(() -> {
                    try {
                        while (consumedCount.get() < total) {
                            Integer val = stack.pop();
                            if (val != null) {
                                if (!consumed.add(val)) {
                                    throw new AssertionError("duplicate consumption: " + val);
                                }
                                consumedCount.incrementAndGet();
                            } else {
                                Thread.onSpinWait();
                            }
                        }
                    } catch (Throwable t) {
                        firstError.compareAndSet(null, t);
                    } finally {
                        done.countDown();
                    }
                });
            }
            done.await();
        }

        if (firstError.get() != null) {
            throw new AssertionError("worker failed: " + firstError.get(), firstError.get());
        }
        assertEquals(total, consumed.size(), "multiset of consumed != produced");
        assertNull(stack.pop(), "stack should be empty after all items consumed");
    }

    /**
     * The defining test for lock-freedom: if some threads stop participating
     * (simulating "infinite delay"), the surviving threads MUST still complete
     * in bounded time. A blocking lock could deadlock here if a stopped thread
     * held the lock; CAS cannot strand anyone because there is no lock to hold.
     */
    @Test
    @Timeout(15)
    void surviving_workers_complete_when_others_exit_mid_run() throws Exception {
        int totalWorkers = 8;
        int quittersCount = 4;
        int perSurvivor = 250_000;
        var stack = new LockFreeStack<Integer>();
        var pushesBySurvivors = new AtomicInteger();
        var survivorsDone = new CountDownLatch(totalWorkers - quittersCount);
        var start = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(totalWorkers)) {
            for (int q = 0; q < quittersCount; q++) {
                pool.execute(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < 100; i++) {
                            stack.push(-1);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            for (int w = 0; w < totalWorkers - quittersCount; w++) {
                final int wid = w;
                pool.execute(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < perSurvivor; i++) {
                            stack.push(wid * perSurvivor + i);
                            pushesBySurvivors.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        survivorsDone.countDown();
                    }
                });
            }
            start.countDown();

            boolean finishedInTime = survivorsDone.await(10, TimeUnit.SECONDS);
            assertTrue(finishedInTime,
                    "survivors did not complete after quitters exited — implementation is not lock-free");
        }
        int expected = (totalWorkers - quittersCount) * perSurvivor;
        assertEquals(expected, pushesBySurvivors.get());
    }

    /**
     * Characterization, not a pass/fail proof. Lock-freedom guarantees system-wide
     * progress, NOT throughput; under maximum CAS contention on a single hot atomic
     * (every thread doing back-to-back push/pop on the same head), cache-line bouncing
     * dominates and the CAS-based stack can be slower than a serializing lock. We log
     * both numbers so you can see when CAS pays off. The @Timeout enforces "completes
     * in bounded time" — the only throughput-shaped property lock-freedom actually gives
     * you.
     */
    @Test
    @Timeout(60)
    void characterize_throughput_vs_synchronized() throws Exception {
        int totalOps = 1_000_000;
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors());

        long lockFreeMs = measureThroughput(threads, totalOps, /*lockFree=*/true);
        long synchronizedMs = measureThroughput(threads, totalOps, /*lockFree=*/false);

        log.log(Level.INFO,
                "{0} threads, {1} ops each: LockFreeStack = {2} ms, ArrayDeque+synchronized = {3} ms",
                new Object[]{threads, totalOps / threads, lockFreeMs, synchronizedMs});
    }

    /**
     * Reproduces the ABA hazard on the LockFreeStack.
     *
     * <p>Why this needs reflection: in plain Java, push() always allocates a fresh
     * Node. The previous head Node becomes unreachable from the stack once popped,
     * and even if a paused thread still holds a stale reference to it, push()
     * cannot make that same Node object reappear at head — push() only ever
     * installs a brand-new Node. So a CAS on head will always see a different
     * object reference and correctly fail. Java's GC + immutable Nodes essentially
     * paper over ABA at the Treiber-stack level.
     *
     * <p>The canonical ABA hazard the Devoxx talk describes (and that bites C/C++
     * code) is memory reuse: an allocator hands out the same address again, and a
     * stale CAS succeeds because the bit pattern at head matches. To reproduce
     * that hazard on this stack we simulate the reuse step by directly restoring
     * an old Node reference at head via reflection. Everything else Thread A and
     * Thread B do is reachable through the public API.
     */
    @Test
    void aba_hazard_demonstrated_via_simulated_memory_reuse() throws Exception {
        var stack = new LockFreeStack<Integer>();
        stack.push(1);
        stack.push(2);
        stack.push(3);
        // head -> Node(3) -> Node(2) -> Node(1)

        Field headField = LockFreeStack.class.getDeclaredField("head");
        headField.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<Object> headRef = (AtomicReference<Object>) headField.get(stack);

        // === Thread A: snapshot head and plan its CAS, then "pause" ===
        Object threadA_oldHead = headRef.get();                                 // Node(3, next=Node(2,...))
        Field nextField = threadA_oldHead.getClass().getDeclaredField("next");
        nextField.setAccessible(true);
        Object threadA_plannedNewHead = nextField.get(threadA_oldHead);         // Node(2, next=Node(1,...))

        // === Thread B: drains the stack legitimately ===
        assertEquals(3, stack.pop());
        assertEquals(2, stack.pop());
        assertEquals(1, stack.pop());
        assertNull(stack.pop());
        // head is now null

        // === Simulated memory reuse: the old Node(3) reappears at head ===
        // In C/C++ this is what a slab allocator handing back the same address
        // looks like. In Java we have to do it by hand via reflection because
        // push() will not reinstall a popped Node object.
        headRef.set(threadA_oldHead);

        // === Thread A resumes and performs its CAS ===
        boolean abaCasSucceeded = headRef.compareAndSet(threadA_oldHead, threadA_plannedNewHead);

        assertTrue(abaCasSucceeded,
                "CAS succeeded even though the stack was drained and a stale reference was restored — this IS ABA");

        // The observable corruption: pop() now returns values that were already
        // returned to earlier callers. A real consumer would see duplicates or
        // process the same work twice.
        assertEquals(2, stack.pop(),
                "stack returned a previously-popped value — silent integrity violation caused by ABA");
        assertEquals(1, stack.pop());
        assertNull(stack.pop());
    }

    /**
     * The fix from Exercise 3 of the Devoxx notes. AtomicStampedReference pairs
     * the reference with a version counter; every mutation increments it. A stale
     * CAS that has the right value but the wrong stamp is rejected, so the
     * "value reverted to its original form" trick that fools plain CAS no longer
     * works.
     *
     * <p>Demonstrated at the bare-reference level for clarity. The same idea
     * applied to LockFreeStack would replace {@code AtomicReference<Node<T>>}
     * with {@code AtomicStampedReference<Node<T>>} and bump the stamp on every
     * push/pop CAS.
     */
    @Test
    void atomic_stamped_reference_detects_the_same_aba_scenario() {
        var stampedRef = new AtomicStampedReference<>("A", 0);

        // Thread A reads value AND stamp
        int[] stampHolder = new int[1];
        String threadA_observedValue = stampedRef.get(stampHolder);
        int threadA_observedStamp = stampHolder[0];

        // Thread B: A -> B -> A, bumping the stamp on each transition
        assertTrue(stampedRef.compareAndSet("A", "B", 0, 1));
        assertTrue(stampedRef.compareAndSet("B", "A", 1, 2));

        // Thread A's CAS now arrives with the right value but the wrong stamp
        boolean cas = stampedRef.compareAndSet(
                threadA_observedValue, "C",
                threadA_observedStamp, threadA_observedStamp + 1);

        assertFalse(cas,
                "stale stamp must cause CAS to fail even when the value looks unchanged");
        assertEquals("A", stampedRef.getReference(), "underlying value reverted to 'A'");
        assertEquals(2, stampedRef.getStamp(), "stamp advanced past Thread A's observation");
    }

    private long measureThroughput(int threads, int totalOps, boolean lockFree) throws InterruptedException {
        var lockFreeStack = lockFree ? new LockFreeStack<Integer>() : null;
        var lockedStack = lockFree ? null : new ArrayDeque<Integer>();
        int perThread = totalOps / threads;
        var start = new CountDownLatch(1);
        var done = new CountDownLatch(threads);
        long t0;
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int t = 0; t < threads; t++) {
                pool.execute(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < perThread; i++) {
                            if (lockFree) {
                                lockFreeStack.push(i);
                                lockFreeStack.pop();
                            } else {
                                synchronized (lockedStack) {
                                    lockedStack.push(i);
                                    lockedStack.pop();
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            t0 = System.nanoTime();
            start.countDown();
            done.await();
        }
        return (System.nanoTime() - t0) / 1_000_000;
    }
}
