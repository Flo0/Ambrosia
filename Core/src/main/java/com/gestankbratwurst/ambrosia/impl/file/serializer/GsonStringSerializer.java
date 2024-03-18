package com.gestankbratwurst.ambrosia.impl.file.serializer;

import com.google.gson.Gson;

public class GsonStringSerializer implements StringSerializer {

  private final Gson gson;

  public GsonStringSerializer(Gson gson) {
    this.gson = gson;
  }

  @Override
  public String serialize(Object object) {
    return gson.toJson(object);
  }

  @Override
  public <T> T deserialize(String string, Class<T> type) {
    return gson.fromJson(string, type);
  }
}
