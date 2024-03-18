package com.gestankbratwurst.ambrosia.impl.mongodb.codec;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * This class is a codec for MongoDB that uses Gson to serialize and deserialize objects
 * to and from BSON
 * <p>
 * It is used in the {@link GsonCodecRegistry}.
 *
 * @param <T> The type of the object to be serialized and deserialized.
 */
public class MongoGsonCodec<T> implements Codec<T> {

  private static final Map<Class<? extends Number>, BiConsumer<Number, BsonWriter>> NUM_WRITERS = Map.of(
      Double.class, (num, writer) -> writer.writeDouble(num.doubleValue()),
      Integer.class, (num, writer) -> writer.writeInt32(num.intValue()),
      Long.class, (num, writer) -> writer.writeInt64(num.longValue()),
      Float.class, (num, writer) -> writer.writeDouble(num.floatValue()),
      Short.class, (num, writer) -> writer.writeDouble(num.shortValue()),
      Byte.class, (num, writer) -> writer.writeDouble(num.byteValue()),
      BigInteger.class, (num, writer) -> writer.writeString(num.toString()),
      BigDecimal.class, (num, writer) -> writer.writeDecimal128(new Decimal128((BigDecimal) num))
  );

  private final Class<T> typeClass;
  private final Gson gson;

  public MongoGsonCodec(Class<T> typeClass, Gson gson) {
    this.typeClass = typeClass;
    this.gson = gson;
  }

  @Override
  public Class<T> getEncoderClass() {
    return this.typeClass;
  }

  @Override
  public T decode(BsonReader reader, DecoderContext decoderContext) {
    JsonObject rootObject = readObject(reader);
    return this.gson.fromJson(rootObject, this.typeClass);
  }

  @Override
  public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
    JsonElement jsonElement = this.gson.toJsonTree(value);
    writeJsonElement(writer, jsonElement);
  }

  private static void writeJsonElement(BsonWriter writer, JsonElement element) {
    if (element.isJsonObject()) {
      writeJsonObject(writer, element.getAsJsonObject());
    } else if (element.isJsonPrimitive()) {
      writeJsonPrimitive(writer, element);
    } else if (element.isJsonNull()) {
      writer.writeNull();
    } else if (element.isJsonArray()) {
      writeJsonArray(writer, element);
    } else {
      throw new IllegalStateException("Unidentified json type");
    }
  }

  private static void writeJsonArray(BsonWriter writer, JsonElement element) {
    writer.writeStartArray();
    element.getAsJsonArray().forEach(value -> writeJsonElement(writer, value));
    writer.writeEndArray();
  }

  private static void writeJsonPrimitive(BsonWriter writer, JsonElement element) {
    JsonPrimitive jsonPrimitive = element.getAsJsonPrimitive();
    if (jsonPrimitive.isString()) {
      writer.writeString(jsonPrimitive.getAsString());
    } else if (jsonPrimitive.isNumber()) {
      Number jsonNumber = jsonPrimitive.getAsNumber();
      NUM_WRITERS.get(jsonNumber.getClass()).accept(jsonNumber, writer);
    } else if (jsonPrimitive.isBoolean()) {
      boolean jsonBoolean = jsonPrimitive.getAsBoolean();
      writer.writeBoolean(jsonBoolean);
    } else {
      throw new IllegalStateException("Json primitive is of unknown type.");
    }
  }

  private static void writeJsonObject(BsonWriter writer, JsonObject element) {
    writer.writeStartDocument();

    element.getAsJsonObject().asMap().forEach((key, value) -> {
      writer.writeName(key);
      writeJsonElement(writer, value);
    });

    writer.writeEndDocument();
  }

  private static JsonObject readObject(BsonReader reader) {
    JsonObject object = new JsonObject();
    reader.readStartDocument();

    BsonType type;
    while ((type = reader.readBsonType()) != BsonType.END_OF_DOCUMENT) {
      String key = reader.readName();
      object.add(key, readElement(reader, type));
    }

    reader.readEndDocument();
    return object;
  }

  private static JsonElement readElement(BsonReader reader, BsonType type) {
    return switch (type) {
      case DOUBLE -> new JsonPrimitive(reader.readDouble());
      case STRING -> new JsonPrimitive(reader.readString());
      case DOCUMENT -> readObject(reader);
      case ARRAY -> readArray(reader);
      case BOOLEAN -> new JsonPrimitive(reader.readBoolean());
      case INT32 -> new JsonPrimitive(reader.readInt32());
      case INT64 -> new JsonPrimitive(reader.readInt64());
      case DECIMAL128 -> new JsonPrimitive(reader.readDecimal128());
      case DATE_TIME -> new JsonPrimitive(reader.readDateTime());
      case TIMESTAMP -> new JsonPrimitive(reader.readTimestamp().getValue());
      case JAVASCRIPT -> new JsonPrimitive(reader.readJavaScript());
      case JAVASCRIPT_WITH_SCOPE -> new JsonPrimitive(reader.readJavaScriptWithScope());
      case REGULAR_EXPRESSION -> new JsonPrimitive(reader.readRegularExpression().getPattern());
      case BINARY -> {
        byte[] data = reader.readBinaryData().getData();
        String base64 = java.util.Base64.getEncoder().encodeToString(data);
        yield new JsonPrimitive(base64);
      }
      case SYMBOL -> new JsonPrimitive(reader.readSymbol());
      case OBJECT_ID -> new JsonPrimitive(reader.readObjectId().toHexString());
      case DB_POINTER -> new JsonPrimitive(reader.readDBPointer().getNamespace());
      case MIN_KEY -> {
        reader.readMinKey();
        yield new JsonPrimitive("$minKey");
      }
      case MAX_KEY -> {
        reader.readMaxKey();
        yield new JsonPrimitive("$maxKey");
      }
      case NULL -> {
        reader.readNull();
        yield JsonNull.INSTANCE;
      }
      default -> {
        reader.skipValue();
        yield JsonNull.INSTANCE;
      }
    };
  }

  private static JsonArray readArray(BsonReader reader) {
    JsonArray jsonArray = new JsonArray();
    reader.readStartArray();

    BsonType type;
    while ((type = reader.readBsonType()) != BsonType.END_OF_DOCUMENT) {
      jsonArray.add(readElement(reader, type));
    }

    reader.readEndArray();
    return jsonArray;
  }

}
