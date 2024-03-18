package com.gestankbratwurst.ambrosia.impl.redisson.codec;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.redisson.client.handler.State;
import org.redisson.client.protocol.Decoder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GsonDecoder implements Decoder<Object> {

  private final Map<String, Class<?>> classMap = new ConcurrentHashMap<>();
  private final Gson gson;

  public GsonDecoder(Gson gson) {
    this.gson = gson;
  }

  @Override
  public Object decode(ByteBuf buf, State state) throws IOException {
    try (ByteBufInputStream stream = new ByteBufInputStream(buf)) {
      String string = stream.readUTF();
      String type = stream.readUTF();

      Class<?> clazz = this.getClassFromType(type);

      if (clazz == null) {
        return null;
      }

      return gson.fromJson(string, clazz);
    }
  }

  private Class<?> getClassFromType(String name) {
    return this.classMap.computeIfAbsent(name, key -> {
      try {
        return Class.forName(key);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("Failed to load class: " + key, e);
      }
    });
  }
}
