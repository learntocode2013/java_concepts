package com.github.learntocode2013;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import com.amazonaws.services.kinesis.model.Record;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class StreamsRecordProcessor implements IRecordProcessor {
  private Integer checkpointCounter;
  private final AsyncLoadingCache<Object, String> cache;

  //TODO: Pass in a cache instance for local caching
  public StreamsRecordProcessor(AsyncLoadingCache<Object, String> i_cache) {
    this.cache = i_cache;
  }

  @Override
  public void initialize(InitializationInput initializationInput) {
    checkpointCounter = 0;
  }

  @Override
  public void processRecords(ProcessRecordsInput processRecordsInput) {
    for (Record record : processRecordsInput.getRecords()) {
      String data = new String(record.getData().array(), StandardCharsets.UTF_8);
      System.out.println(data);
      if (record instanceof RecordAdapter) {
        com.amazonaws.services.dynamodbv2.model.Record streamRecord = ((RecordAdapter) record)
            .getInternalObject();

        switch (streamRecord.getEventName()) {
          case "INSERT":
          case "MODIFY":
            Map<String, AttributeValue> newImage = streamRecord.getDynamodb().getNewImage();
            if (newImage.get("metakey").getS().equals("activeRealm")) {
              cache.put("activeRealm",
                  CompletableFuture.supplyAsync(() -> {
                    System.out.printf("Active realm was updated to: %s %n",newImage.get("value").getS());
                    return newImage.get("value").getS();
                  }));
            }

            break;
          case "REMOVE":
            if(streamRecord.getDynamodb().getKeys().get("metakey").getS().equals("activeRealm")) {
              System.out.printf("Global setting %s was removed. %n","activeRealm");
              cache.asMap().remove("activeRealm")
                  .whenComplete((pVal, err) -> System.out.printf(
                      "Removed entry:%s since global setting %s was removed.%n",
                      "activeRealm","activeRealm"));
            }

        }
      }
      checkpointCounter += 1;
      if (checkpointCounter % 10 == 0) {
        try {
          processRecordsInput.getCheckpointer().checkpoint();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

  }

  @Override
  public void shutdown(ShutdownInput shutdownInput) {
    if (shutdownInput.getShutdownReason() == ShutdownReason.TERMINATE) {
      try {
        shutdownInput.getCheckpointer().checkpoint();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }
}
