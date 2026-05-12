import io.vavr.control.Try;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class SimpleMutexTest {
    private final SimpleNonReEntrantMutex mutex = new SimpleNonReEntrantMutex();

    @Test
    @SneakyThrows
    void testExclusiveAccess_With_Mutex() {
        // Define a shared state
        var employeeList = new ArrayList<String>(2);
        var taskCount = Runtime.getRuntime().availableProcessors();
        var latch = new CountDownLatch(taskCount);
        try(ExecutorService es = Executors.newFixedThreadPool(taskCount)) {
            for (int i = 0; i < taskCount; i++) {
                es.submit(() -> {
                    try {
                        if(mutex.lock()) {
                            employeeList.add(UUID.randomUUID().toString());
                            Thread.sleep(Duration.ofSeconds(2));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } finally {
                        Try.run(mutex::unlock);
                        latch.countDown();
                    }
                });
            }
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Assertions.assertEquals(1, employeeList.size());
    }
}