package com.gestankbratwurst.ambrosia.impl.redisson.codec;

import com.google.gson.Gson;
import org.redisson.client.codec.BaseCodec;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;

public class RedissonGsonCodec extends BaseCodec {

  private final GsonEncoder encoder;
  private final GsonDecoder decoder;
  private final ClassLoader classLoader;

  public RedissonGsonCodec(Gson gson, ClassLoader classLoader) {
    this.encoder = new GsonEncoder(gson);
    this.decoder = new GsonDecoder(gson);
    this.classLoader = classLoader;
  }

  public RedissonGsonCodec(Gson gson) {
    this(gson, gson.getClass().getClassLoader());
  }

  @Override
  public Decoder<Object> getValueDecoder() {
    return this.decoder;
  }

  @Override
  public Encoder getValueEncoder() {
    return this.encoder;
  }

  @Override
  public ClassLoader getClassLoader() {
    return this.classLoader;
  }
}
