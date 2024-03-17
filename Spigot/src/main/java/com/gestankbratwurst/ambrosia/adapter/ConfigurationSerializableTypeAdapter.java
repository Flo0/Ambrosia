package com.gestankbratwurst.ambrosia.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Base64;

public class ConfigurationSerializableTypeAdapter<T extends ConfigurationSerializable> implements JsonSerializer<T>, JsonDeserializer<T> {

  @Override
  @SuppressWarnings("unchecked")
  public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    try {
      byte[] data = Base64.getDecoder().decode(json.getAsString());
      ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
      BukkitObjectInputStream bukkitObjectInputStream = new BukkitObjectInputStream(inputStream);
      T result = (T) bukkitObjectInputStream.readObject();
      bukkitObjectInputStream.close();
      return result;
    } catch (IOException | ClassNotFoundException e) {
      throw new JsonParseException(e);
    }
  }

  @Override
  public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      BukkitObjectOutputStream bukkitObjectOutputStream = new BukkitObjectOutputStream(outputStream);
      bukkitObjectOutputStream.writeObject(src);
      bukkitObjectOutputStream.close();
      String data = Base64.getEncoder().encodeToString(outputStream.toByteArray());
      return new JsonPrimitive(data);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
