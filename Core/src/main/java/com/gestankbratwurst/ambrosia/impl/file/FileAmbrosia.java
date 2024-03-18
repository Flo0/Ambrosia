package com.gestankbratwurst.ambrosia.impl.file;

import com.gestankbratwurst.ambrosia.Ambrosia;
import com.gestankbratwurst.ambrosia.impl.file.collections.FileMap;
import com.gestankbratwurst.ambrosia.impl.file.serializer.GsonStringSerializer;
import com.gestankbratwurst.ambrosia.impl.file.serializer.StringSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.util.function.Consumer;

public class FileAmbrosia extends Ambrosia<File, FileMap<?, ?>> {

  public static Builder<?> builder() {
    return new Builder<>();
  }

  private final StringSerializer serializer;

  public FileAmbrosia(File folderBackbone, StringSerializer serializer) {
    super(folderBackbone);
    if (folderBackbone.isFile()) {
      throw new IllegalArgumentException("FileAmbrosia requires a folder as backbone.");
    }
    this.serializer = serializer;
  }

  @Override
  public <K, V> FileMap<K, V> createMapView(String mapName, Class<K> keyType, Class<V> valueType) {
    File mapFolder = new File(this.getBackbone() + "/" + mapName);
    if (!mapFolder.exists()) {
      if (!mapFolder.mkdirs()) {
        throw new IllegalStateException("Failed to create folder: " + mapFolder);
      }
    }
    return new FileMap<>(mapFolder, this.serializer, keyType, valueType);
  }

  public static sealed class Builder<SELF extends Builder<SELF>> permits AmbrosiaSerializerBuilder, AmbrosiaGsonBuilder, AmbrosiaGsonConstructBuilder {

    protected StringSerializer serializer;
    protected File folder;

    private Builder() {
    }

    @SuppressWarnings("unchecked")
    protected SELF self() {
      return (SELF) this;
    }

    public AmbrosiaSerializerBuilder serializer(StringSerializer serializer) {
      return new AmbrosiaSerializerBuilder(serializer).folder(this.folder);
    }

    public AmbrosiaGsonBuilder gson(Gson gson) {
      return new AmbrosiaGsonBuilder(gson).folder(this.folder);
    }

    public AmbrosiaGsonConstructBuilder gsonBuild() {
      return new AmbrosiaGsonConstructBuilder().folder(this.folder);
    }

    public SELF folder(File folder) {
      this.folder = folder;
      return this.self();
    }

    public FileAmbrosia build() {
      if (this.serializer == null) {
        throw new IllegalStateException("Serializer is not set.");
      }
      if (this.folder == null) {
        throw new IllegalStateException("Folder is not set.");
      }
      return new FileAmbrosia(this.folder, this.serializer);
    }

  }

  public static final class AmbrosiaSerializerBuilder extends Builder<AmbrosiaSerializerBuilder> {

    private AmbrosiaSerializerBuilder(StringSerializer serializer) {
      this.serializer = serializer;
    }

  }

  public static final class AmbrosiaGsonBuilder extends Builder<AmbrosiaGsonBuilder> {

    private final Gson gson;

    private AmbrosiaGsonBuilder(Gson gson) {
      this.gson = gson;
    }

    @Override
    public FileAmbrosia build() {
      this.serializer = new GsonStringSerializer(this.gson);
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
    public FileAmbrosia build() {
      this.serializer = new GsonStringSerializer(this.gsonBuilder.create());
      return super.build();
    }
  }

}
