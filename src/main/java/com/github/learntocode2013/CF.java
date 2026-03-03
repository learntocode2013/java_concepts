package com.github.learntocode2013;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CF {

  public static void main(String[] args) {
    CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));
      } catch (Exception ex) {
        //do nothing
      }
      return "Dibakar";
    });

    CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));
      } catch (Exception ex) {
        //do nothing
      }
      return "Sen";
//      throw new RuntimeException("failed");
    });

    CompletableFuture<Void> allCfs = CompletableFuture.allOf(cf1, cf2)
        .whenComplete((__, throwable) -> {
          if (Objects.isNull(throwable)) {
            System.err.println("All passed");
          }
        });
    allCfs.thenAccept(__ -> System.out.println("batch update completed successfully"))
        .exceptionally(t ->  {System.err.println("Something failed due to: " + t); return null;});
    System.out.println("Blocking wait....");
    allCfs.join();
  }
}

