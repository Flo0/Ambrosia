package com.gestankbratwurst.ambrosia.impl.redisson;

import com.gestankbratwurst.ambrosia.Ambrosia;
import com.gestankbratwurst.ambrosia.impl.redisson.codec.RedissonGsonCodec;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.BaseCodec;

import java.util.function.Consumer;

public class RedissonAmbrosia extends Ambrosia<RedissonClient, RMap<?, ?>> {

  public static Builder<?> builder() {
    return new Builder<>();
  }

  private final BaseCodec codec;

  public RedissonAmbrosia(RedissonClient backbone, BaseCodec codec) {
    super(backbone);
    this.codec = codec;
  }

  @Override
  public <K, V> RMap<K, V> createMapView(String mapName, Class<K> keyType, Class<V> valueType) {
    return getBackbone().getMap(mapName, codec);
  }

  public static sealed class Builder<SELF extends Builder<SELF>> permits AmbrosiaCodecBuilder, AmbrosiaGsonBuilder, AmbrosiaGsonConstructBuilder {

    protected BaseCodec codec;
    protected RedissonClient redissonClient;

    private Builder() {
    }

    @SuppressWarnings("unchecked")
    protected SELF self() {
      return (SELF) this;
    }

    public AmbrosiaCodecBuilder codec(BaseCodec codec) {
      return new AmbrosiaCodecBuilder(codec).client(this.redissonClient);
    }

    public AmbrosiaGsonBuilder gson(Gson gson) {
      return new AmbrosiaGsonBuilder(gson).client(this.redissonClient);
    }

    public AmbrosiaGsonConstructBuilder gsonBuild() {
      return new AmbrosiaGsonConstructBuilder().client(this.redissonClient);
    }

    public SELF client(RedissonClient redissonClient) {
      this.redissonClient = redissonClient;
      return this.self();
    }

    public RedissonAmbrosia build() {
      if (this.codec == null) {
        throw new IllegalStateException("Codec is not set.");
      }
      if (this.redissonClient == null) {
        throw new IllegalStateException("RedissonClient is not set.");
      }
      return new RedissonAmbrosia(this.redissonClient, this.codec);
    }

  }

  public static final class AmbrosiaCodecBuilder extends Builder<AmbrosiaCodecBuilder> {

    private AmbrosiaCodecBuilder(BaseCodec codec) {
      this.codec = codec;
    }

  }

  public static final class AmbrosiaGsonBuilder extends Builder<AmbrosiaGsonBuilder> {

    private final Gson gson;

    private AmbrosiaGsonBuilder(Gson gson) {
      this.gson = gson;
    }

    @Override
    public RedissonAmbrosia build() {
      this.codec = new RedissonGsonCodec(this.gson);
      return super.build();
    }
  }

  public static final class AmbrosiaGsonConstructBuilder extends Builder<AmbrosiaGsonConstructBuilder> {

    private final GsonBuilder gsonBuilder;

    private AmbrosiaGsonConstructBuilder() {
      this.gsonBuilder = new GsonBuilder();
    }

    public AmbrosiaGsonConstructBuilder construct(Consumer<GsonBuilder> consumer) {
      consumer.accept(this.gsonBuilder);
      return this.self();
    }

    @Override
    public RedissonAmbrosia build() {
      this.codec = new RedissonGsonCodec(this.gsonBuilder.create());
      return super.build();
    }
  }
}
