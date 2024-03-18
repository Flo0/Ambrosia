package com.gestankbratwurst.ambrosia.impl.mongodb.codec;

import com.google.gson.Gson;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a Gson backed codec registry for MongoDB.
 */
public class GsonCodecRegistry implements CodecRegistry {

  private final Gson gsonSerializer;
  private final Map<Class<?>, Codec<?>> codecCache;

  public GsonCodecRegistry(Gson gsonSerializer) {
    this.codecCache = new ConcurrentHashMap<>();
    this.gsonSerializer = gsonSerializer;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Codec<T> get(Class<T> clazz) {
    return (Codec<T>) this.codecCache.computeIfAbsent(clazz, (key) -> new MongoGsonCodec<>(key, this.gsonSerializer));
  }

  @Override
  public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
    return this.get(clazz);
  }
}
