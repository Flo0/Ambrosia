package com.gestankbratwurst.ambrosia.impl.pdc;

import com.gestankbratwurst.ambrosia.Ambrosia;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

public class PDCAmbrosia extends Ambrosia<PersistentDataContainer, PDCMap<?, ?>> {

  public static Builder<?> builder() {
    return new Builder<>();
  }

  private final PDCSerializer serializer;

  public PDCAmbrosia(PersistentDataContainer backbone, PDCSerializer serializer) {
    super(backbone);
    this.serializer = serializer;
  }

  @Override
  public <K, V> PDCMap<K, V> createMapView(String unused, Class<K> keyType, Class<V> valueType) {
    return new PDCMap<>(this.getBackbone(), this.serializer, keyType, valueType);
  }

  public <K, V> PDCMap<K, V> createMapView(Class<K> keyType, Class<V> valueType) {
    return createMapView("default", keyType, valueType);
  }

  public static sealed class Builder<SELF extends Builder<SELF>> permits AmbrosiaSerializerBuilder, AmbrosiaGsonBuilder, AmbrosiaGsonConstructBuilder {

    protected PDCSerializer serializer;
    protected PersistentDataContainer container;

    private Builder() {
    }

    @SuppressWarnings("unchecked")
    protected SELF self() {
      return (SELF) this;
    }

    public AmbrosiaSerializerBuilder serializer(PDCSerializer serializer) {
      return new AmbrosiaSerializerBuilder(serializer).container(this.container);
    }

    public AmbrosiaGsonBuilder gson(Gson gson) {
      return new AmbrosiaGsonBuilder(gson).container(this.container);
    }

    public AmbrosiaGsonConstructBuilder gsonBuild() {
      return new AmbrosiaGsonConstructBuilder().container(this.container);
    }

    public SELF container(PersistentDataContainer container) {
      this.container = container;
      return this.self();
    }

    public PDCAmbrosia build() {
      if (this.serializer == null) {
        throw new IllegalStateException("Serializer is not set.");
      }
      if (this.container == null) {
        throw new IllegalStateException("PDC is not set.");
      }
      return new PDCAmbrosia(this.container, this.serializer);
    }

  }

  public static final class AmbrosiaSerializerBuilder extends Builder<AmbrosiaSerializerBuilder> {

    private AmbrosiaSerializerBuilder(PDCSerializer serializer) {
      this.serializer = serializer;
    }

  }

  public static final class AmbrosiaGsonBuilder extends Builder<AmbrosiaGsonBuilder> {

    private final Gson gson;
    private JavaPlugin plugin;

    private AmbrosiaGsonBuilder(Gson gson) {
      this.gson = gson;
    }

    public AmbrosiaGsonBuilder plugin(JavaPlugin plugin) {
      this.plugin = plugin;
      return this;
    }

    @Override
    public PDCAmbrosia build() {
      this.serializer = new PDCGsonSerializer(this.gson, this.plugin);
      return super.build();
    }
  }

  public static final class AmbrosiaGsonConstructBuilder extends Builder<AmbrosiaGsonConstructBuilder> {

    private final GsonBuilder gsonBuilder;
    private JavaPlugin plugin;

    private AmbrosiaGsonConstructBuilder() {
      this.gsonBuilder = new GsonBuilder();
    }

    public AmbrosiaGsonConstructBuilder construct(Consumer<GsonBuilder> consumer) {
      consumer.accept(this.gsonBuilder);
      return this.self();
    }

    public AmbrosiaGsonConstructBuilder plugin(JavaPlugin plugin) {
      this.plugin = plugin;
      return this.self();
    }

    @Override
    public PDCAmbrosia build() {
      this.serializer = new PDCGsonSerializer(this.gsonBuilder.create(), this.plugin);
      return super.build();
    }
  }

}
