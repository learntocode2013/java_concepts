package com.github.learntocode2013;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;

public class StreamsRecordProcessorFactory implements IRecordProcessorFactory {
  private final AsyncLoadingCache<Object, String> cache;
  public StreamsRecordProcessorFactory(AsyncLoadingCache<Object, String> i_cache) {
    this.cache = i_cache;
  }

  @Override
  public IRecordProcessor createProcessor() {
    return new StreamsRecordProcessor(cache);
  }
}
