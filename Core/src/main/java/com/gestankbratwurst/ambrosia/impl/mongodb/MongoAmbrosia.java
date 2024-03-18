package com.gestankbratwurst.ambrosia.impl.mongodb;

import com.gestankbratwurst.ambrosia.Ambrosia;
import com.gestankbratwurst.ambrosia.impl.mongodb.codec.GsonCodecRegistry;
import com.gestankbratwurst.ambrosia.impl.mongodb.collections.MongoMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.function.Consumer;

public final class MongoAmbrosia extends Ambrosia<MongoDatabase, MongoMap<?, ?>> {

  public static Builder<?> builder() {
    return new Builder<>();
  }

  private final CodecRegistry codecRegistry;

  private MongoAmbrosia(CodecRegistry codecRegistry, MongoDatabase mongoDatabase) {
    super(mongoDatabase);
    this.codecRegistry = codecRegistry;
  }

  public <T> MongoCollection<T> createMongoCollection(String collectionName, Class<T> elementType) {
    return this.getBackbone().getCollection(collectionName, elementType).withCodecRegistry(this.codecRegistry);
  }

  @Override
  public <K, V> MongoMap<K, V> createMapView(String mapName, Class<K> keyType, Class<V> valueType) {
    MongoCollection<V> mongoBackbone = this.createMongoCollection(mapName, valueType);
    return new MongoMap<>(mongoBackbone, keyType);
  }

  public static sealed class Builder<SELF extends Builder<SELF>> permits AmbrosiaCodecBuilder, AmbrosiaGsonBuilder, AmbrosiaGsonConstructBuilder {

    protected CodecRegistry codecRegistry;
    protected MongoDatabase mongoDatabase;

    private Builder() {
    }

    @SuppressWarnings("unchecked")
    protected SELF self() {
      return (SELF) this;
    }

    public AmbrosiaCodecBuilder codecRegistry(CodecRegistry codecRegistry) {
      return new AmbrosiaCodecBuilder(codecRegistry).database(this.mongoDatabase);
    }

    public AmbrosiaGsonBuilder gson(Gson gson) {
      return new AmbrosiaGsonBuilder(gson).database(this.mongoDatabase);
    }

    public AmbrosiaGsonConstructBuilder gsonBuild() {
      return new AmbrosiaGsonConstructBuilder().database(this.mongoDatabase);
    }

    public SELF database(MongoDatabase mongoDatabase) {
      this.mongoDatabase = mongoDatabase;
      return this.self();
    }

    public MongoAmbrosia build() {
      if (this.codecRegistry == null) {
        throw new IllegalStateException("CodecRegistry is not set.");
      }
      if (this.mongoDatabase == null) {
        throw new IllegalStateException("MongoDatabase is not set.");
      }
      return new MongoAmbrosia(this.codecRegistry, this.mongoDatabase);
    }

  }

  public static final class AmbrosiaCodecBuilder extends Builder<AmbrosiaCodecBuilder> {

    private AmbrosiaCodecBuilder(CodecRegistry codecRegistry) {
      this.codecRegistry = codecRegistry;
    }

  }

  public static final class AmbrosiaGsonBuilder extends Builder<AmbrosiaGsonBuilder> {

    private final Gson gson;

    private AmbrosiaGsonBuilder(Gson gson) {
      this.gson = gson;
    }

    @Override
    public MongoAmbrosia build() {
      this.codecRegistry = new GsonCodecRegistry(this.gson);
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
    public MongoAmbrosia build() {
      this.codecRegistry = new GsonCodecRegistry(this.gsonBuilder.create());
      return super.build();
    }
  }

}