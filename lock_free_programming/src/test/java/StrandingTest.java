import one.profiler.AsyncProfiler;
import one.profiler.AsyncProfilerLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StrandingTest {
    private static final Logger log = Logger.getLogger(StrandingTest.class.getName());
    private static final Path PROFILE_DIR = Path.of("target", "async-profiler");
    // async-profiler 3.0 (bundled in ap-loader-all:3.0-9) crashes on macOS aarch64
    // when its signal-based wall/cpu sampler walks the stack of a thread parked in
    // ObjectMonitor::EnterI. event=lock uses JVMTI MonitorContendedEnter callbacks
    // (no async signal handler), so it sidesteps the crash and is also the most
    // direct event for the stranding exercise.
    private static final String PROFILE_EVENT = "lock";

    private int sharedCounter = 0;
    private final ReentrantLock sharedLock = new ReentrantLock();
    private AsyncProfiler profiler;
    private Path profileOutput;

    @BeforeEach
    void setUp(TestInfo testInfo) throws IOException {
        sharedCounter = 0;
        Files.createDirectories(PROFILE_DIR);
        String methodName = testInfo.getTestMethod().orElseThrow().getName();
        profileOutput = PROFILE_DIR.resolve(methodName + "-" + PROFILE_EVENT + ".html");
        profiler = AsyncProfilerLoader.load();
        String startResult = profiler.execute("start,event=" + PROFILE_EVENT);
        log.log(Level.INFO, "Profiler start: {0}", startResult.trim());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (profiler != null) {
            // async-profiler honors file= only on stop/dump, not on start.
            String stopResult = profiler.execute(
                    "stop,file=" + profileOutput.toAbsolutePath());
            log.log(Level.INFO, "Profiler stop: {0}", stopResult.trim());
        }
    }

    @Test
    void synchronization_under_contention() {
        final var writerCount = 200;
        final var incrementsPerWriter = 250_000;
        Instant start = Instant.now();
        try(ExecutorService writerPool = Executors.newFixedThreadPool(writerCount)) {
            for(int i = 0; i < writerCount; i++) {
                writerPool.execute(() -> {
                    for (int j = 0; j < incrementsPerWriter; j++) {
                        incrementSynchronizedCounter();
                    }
                });
            }
        }
        Duration duration = Duration.between(start, Instant.now());
        long expectedCounter = (long) writerCount * incrementsPerWriter;
        log.log(Level.INFO, "synchronized: {0}/{1} in {2} ms",
                new Object[]{sharedCounter, expectedCounter, duration.toMillis()});
        Assertions.assertEquals(expectedCounter, sharedCounter);
    }

    @Test
    void lock_under_contention() {
        final var writerCount = 200;
        final var incrementsPerWriter = 250_000;
        Instant start = Instant.now();
        try(ExecutorService writerPool = Executors.newFixedThreadPool(writerCount)) {
            for(int i = 0; i < writerCount; i++) {
                writerPool.execute(() -> {
                    for (int j = 0; j < incrementsPerWriter; j++) {
                        incrementLockProtectedCounter();
                    }
                });
            }
        }
        Duration duration = Duration.between(start, Instant.now());
        long expectedCounter = (long) writerCount * incrementsPerWriter;
        log.log(Level.INFO, "ReentrantLock: {0}/{1} in {2} ms",
                new Object[]{sharedCounter, expectedCounter, duration.toMillis()});
        Assertions.assertEquals(expectedCounter, sharedCounter);
    }

    private synchronized void incrementSynchronizedCounter() {
        sharedCounter++;
    }

    private void incrementLockProtectedCounter() {
        sharedLock.lock();
        try {
            sharedCounter++;
        } finally {
            sharedLock.unlock();
        }
    }
}
