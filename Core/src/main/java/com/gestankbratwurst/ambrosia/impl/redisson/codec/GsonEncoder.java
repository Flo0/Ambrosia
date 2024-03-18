package com.gestankbratwurst.ambrosia.impl.redisson.codec;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import org.redisson.client.protocol.Encoder;

import java.io.IOException;

public class GsonEncoder implements Encoder {

  private final Gson gson;

  public GsonEncoder(Gson gson) {
    this.gson = gson;
  }

  @Override
  public ByteBuf encode(Object in) throws IOException {
    ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
    try (ByteBufOutputStream os = new ByteBufOutputStream(out)) {
      os.writeUTF(gson.toJson(in));
      os.writeUTF(in.getClass().getName());
    } catch (Exception e) {
      throw new IOException(e);
    }
    return out;
  }
}
