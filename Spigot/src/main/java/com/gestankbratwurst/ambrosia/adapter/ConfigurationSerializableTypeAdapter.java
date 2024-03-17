package com.gestankbratwurst.ambrosia.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.lang.reflect.Type;
import java.util.Map;

public class ConfigurationSerializableTypeAdapter<T extends ConfigurationSerializable> implements JsonSerializer<T>, JsonDeserializer<T> {

  private final Type mapType = new TypeToken<Map<String, Object>>() {
  }.getType();

  @Override
  @SuppressWarnings("unchecked")
  public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    Map<String, Object> data = context.deserialize(json, mapType);
    return (T) ConfigurationSerialization.deserializeObject(data);
  }

  @Override
  public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
    return context.serialize(src.serialize());
  }
}
