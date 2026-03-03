package com.github.learntocode2013;

import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClientBuilder;
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient;
import com.amazonaws.services.dynamodbv2.streamsadapter.StreamsWorkerFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.util.StringUtils;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class DynamoUpdate {
  private static final AtomicInteger counter = new AtomicInteger(1);
  private static final SystemPropertiesCredentialsProvider credentialsProvider = new SystemPropertiesCredentialsProvider();
  private static final Regions usEast2 = Regions.US_EAST_2;
  private static final String streamArn = "arn:aws:dynamodb:us-east-2:912103496200:table/flex.domain-gateway.tenant-attributes/stream/2024-03-06T03:16:51.201";

  public static void main(String[] args) throws Exception {
    printAccessKeyInfo();
    Thread[] threads = new Thread[3];
    Worker[] workers = new Worker[3];
    AsyncLoadingCache<Object, String>[] caches = new AsyncLoadingCache[3];
    for(int i = 0; i < 3; i++) {
      ExecutorService tPoolSvc = Executors.newFixedThreadPool(1);
      AsyncLoadingCache<Object, String> cache = Caffeine.newBuilder()
          .initialCapacity(10)
          .maximumSize(100)
          .buildAsync(k -> "us-1");
      final int c = counter.getAndIncrement();
      workers[i] = createWorker(tPoolSvc, cache, "streams-demo-worker-"+c);
      threads[i] = new Thread(() -> {
        while(true) {
          cache.get("activeRealm")
              .whenComplete((v, err) -> System.out.printf("Host-%d - Realm value in cache is:%s%n",c,v));
          try {
            Thread.sleep(Duration.ofSeconds(10).toMillis());
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }

      });
    }

    Arrays.asList(threads).forEach(Thread::start);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      Arrays.asList(workers).forEach(Worker::shutdown);
    }));
  }

  private static void printAccessKeyInfo() {
    String accessKey = StringUtils.trim(System.getProperty("aws.accessKeyId"));
    String secretKey = StringUtils.trim(System.getProperty("aws.secretKey"));
    System.out.printf("accessKey: %s | secretKey: %s %n", accessKey, secretKey);
  }

  private static Worker createWorker(ExecutorService tPoolSvc, AsyncLoadingCache<Object, String> cache, String workerId) {
    AmazonDynamoDB dynamoDBClient = AmazonDynamoDBClientBuilder.standard().withRegion(usEast2)
        .withCredentials(credentialsProvider).build();
    AmazonCloudWatch cloudWatchClient = AmazonCloudWatchClientBuilder.standard().withRegion(usEast2)
        .withCredentials(credentialsProvider).build();
    AmazonDynamoDBStreams dynamoDBStreamsClient = AmazonDynamoDBStreamsClientBuilder.standard()
        .withRegion(usEast2)
        .withCredentials(credentialsProvider)
        .build();
    AmazonDynamoDBStreamsAdapterClient streamsAdapterClient = new AmazonDynamoDBStreamsAdapterClient(
        dynamoDBStreamsClient);
    StreamsRecordProcessorFactory recordProcessorFactory = new StreamsRecordProcessorFactory(cache);
    KinesisClientLibConfiguration workerConfig = new KinesisClientLibConfiguration(
        String.format("flex.domain-gateway.%s",UUID.randomUUID()),
        streamArn,
        credentialsProvider,
        workerId
    ).withMaxRecords(1000).withIdleTimeBetweenReadsInMillis(500)
        .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON);
    System.out.printf("Creating worker for stream: %s%n",streamArn);
    Worker worker = StreamsWorkerFactory.createDynamoDbStreamsWorker(
        recordProcessorFactory,
        workerConfig,
        streamsAdapterClient,
        dynamoDBClient,
        cloudWatchClient);
    tPoolSvc.submit(worker);
    return worker;
  }
}
