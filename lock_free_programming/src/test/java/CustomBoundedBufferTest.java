import io.vavr.control.Try;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

class CustomBoundedBufferTest {
    private static final Logger LOGGER = Logger.getLogger(CustomBoundedBufferTest.class.getName());

    @Test
    @Timeout(value = 10)
    void verify_custom_bounded_buffer_under_contention() throws InterruptedException {
        var taskCount = 100;
        CustomBoundedBuffer buffer = new CustomBoundedBuffer(taskCount);
        CountDownLatch latch = new CountDownLatch(taskCount);
        Thread writer = new Thread(() -> {
            LOGGER.log(Level.INFO, "Will write 100 values to buffer");
            for(int i = 0; i < 100; i++) {
                try {
                    buffer.put(Integer.valueOf(i));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        writer.start();
        // Readers
        try(ExecutorService readerPool = Executors.newFixedThreadPool(taskCount)) {
            for(int i = 0; i < taskCount; i++) {
                readerPool.execute(() -> {
                    try {
                        var val = buffer.take();
                        LOGGER.log(Level.INFO, "{0} read data {1} from buffer",
                                new Object[]{Thread.currentThread().getName(), val});
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }
        latch.await();
        LOGGER.log(Level.INFO, "Buffer size: {0}", buffer.size());
        Assertions.assertTrue(buffer.isEmpty());
        Assertions.assertThrows(
                TimeoutException.class,
                () -> CompletableFuture.supplyAsync(() -> Try.of(buffer::take))
                        .get(2, TimeUnit.SECONDS));
    }
}