package com.gestankbratwurst.ambrosia;

import com.gestankbratwurst.ambrosia.adapter.ConfigurationSerializableTypeAdapter;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoDatabase;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.function.Consumer;

public class SpigotCapableAmbrosia {

  public static Ambrosia create(MongoDatabase database) {
    return create(database, GsonBuilder::enableComplexMapKeySerialization);
  }

  public static Ambrosia create(MongoDatabase database, Consumer<GsonBuilder> consumer) {
    return Ambrosia.builder()
        .database(database)
        .gsonBuild()
        .construct(builder -> builder.registerTypeHierarchyAdapter(ConfigurationSerializable.class, new ConfigurationSerializableTypeAdapter<>()))
        .construct(consumer)
        .build();
  }

}
