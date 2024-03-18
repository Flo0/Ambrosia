package com.gestankbratwurst.ambrosia;

import com.gestankbratwurst.ambrosia.adapter.ConfigurationSerializableTypeAdapter;
import com.gestankbratwurst.ambrosia.impl.file.FileAmbrosia;
import com.gestankbratwurst.ambrosia.impl.mongodb.MongoAmbrosia;
import com.gestankbratwurst.ambrosia.impl.pdc.PDCAmbrosia;
import com.gestankbratwurst.ambrosia.impl.redisson.RedissonAmbrosia;
import com.google.gson.GsonBuilder;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

public class SpigotCapableAmbrosia {

  private static void registerConfigurationSerializable(GsonBuilder builder) {
    builder.registerTypeHierarchyAdapter(ConfigurationSerializable.class, new ConfigurationSerializableTypeAdapter<>());
  }

  public static MongoAmbrosia.AmbrosiaGsonConstructBuilder mongoDB() {
    return MongoAmbrosia.builder()
        .gsonBuild()
        .construct(SpigotCapableAmbrosia::registerConfigurationSerializable)
        .construct(GsonBuilder::enableComplexMapKeySerialization);
  }

  public static RedissonAmbrosia.AmbrosiaGsonConstructBuilder redisson() {
    return RedissonAmbrosia.builder()
        .gsonBuild()
        .construct(SpigotCapableAmbrosia::registerConfigurationSerializable)
        .construct(GsonBuilder::enableComplexMapKeySerialization);
  }

  public static FileAmbrosia.AmbrosiaGsonConstructBuilder toFiles() {
    return FileAmbrosia.builder()
        .gsonBuild()
        .construct(SpigotCapableAmbrosia::registerConfigurationSerializable)
        .construct(GsonBuilder::enableComplexMapKeySerialization);
  }

  public static PDCAmbrosia.AmbrosiaGsonConstructBuilder pdc() {
    return PDCAmbrosia.builder()
        .gsonBuild()
        .construct(SpigotCapableAmbrosia::registerConfigurationSerializable)
        .construct(GsonBuilder::enableComplexMapKeySerialization);
  }

}
