package com.github.learntocode2013;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Hello world!
 *
 */
public class PublicApiProvider
{
    private static final ExecutorService executor = Executors.newFixedThreadPool(1);
    private static final Logger LOGGER = Logger.getLogger(PublicApiProvider.class.getName());
    // Note: Throwing checked exceptions from the API
    public static void checkProcessorType()
        throws InterruptedException, ExecutionException {
        List<Callable<String>> tasks = List.of(
            () -> System.getProperty("os.arch"),
            () -> String.valueOf(Runtime.getRuntime().availableProcessors())
        );
        var futures = tasks.stream()
            .map(executor::submit)
            .toList();

        System.out.printf("System running on %s with %s processors %n",
            futures.getFirst().get(),
            futures.getLast().get());
    }

    // Note: Throwing an unchecked exception from the API
    /**
     * Declaring unchecked exceptions in a method signature can serve the purpose of documentation.
     * Such documentation may be better because there are greater chances that the developer using
     * our API will read it.
     */
    private static boolean isRunning = false;
    public static void setupService(int concurrency)
        throws IllegalStateException, IllegalArgumentException {
        if (concurrency < 1) {
            throw new IllegalArgumentException("Number of concurrency must be greater than 0.");
        }
        if (isRunning) {
            throw new IllegalStateException("Service is already running");
        }
        isRunning = true;
    }

    public static void makeRemoteCallWithoutResourceLeak() {
        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
            processRequest(httpClient);
        } catch (IOException ioex) {
            LOGGER.log(Level.SEVERE, "Error while making remote call", ioex);
        }
    }

    private static void processRequest(CloseableHttpClient httpClient) {
        
    }
}
