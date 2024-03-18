package com.gestankbratwurst.ambrosia.impl.file.collections;

import com.gestankbratwurst.ambrosia.impl.file.serializer.StringSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FileMap<K, V> implements Map<K, V> {

  private static final String jsonExtension = ".json";

  private final File folder;
  private final StringSerializer serializer;
  private final Class<K> keyType;
  private final Class<V> valueType;

  public FileMap(File folder, StringSerializer serializer, Class<K> keyType, Class<V> valueType) {
    this.folder = folder;
    this.serializer = serializer;
    this.keyType = keyType;
    this.valueType = valueType;
  }

  private File getFile(K key) {
    String serializedKey = this.serializeKey(key);
    return new File(this.folder, serializedKey + jsonExtension);
  }

  private K keyFromFileName(String fileName) {
    fileName = fileName.substring(0, fileName.length() - jsonExtension.length());

    if (keyType.isPrimitive()) {
      if (keyType == int.class) {
        return keyType.cast(Integer.parseInt(fileName));
      } else if (keyType == long.class) {
        return keyType.cast(Long.parseLong(fileName));
      } else if (keyType == double.class) {
        return keyType.cast(Double.parseDouble(fileName));
      } else if (keyType == float.class) {
        return keyType.cast(Float.parseFloat(fileName));
      } else if (keyType == short.class) {
        return keyType.cast(Short.parseShort(fileName));
      } else if (keyType == byte.class) {
        return keyType.cast(Byte.parseByte(fileName));
      } else if (keyType == char.class) {
        return keyType.cast(fileName.charAt(0));
      } else if (keyType == boolean.class) {
        return keyType.cast(Boolean.parseBoolean(fileName));
      } else {
        throw new IllegalArgumentException("Unknown primitive type: " + keyType);
      }
    }

    if (keyType == String.class) {
      return keyType.cast(fileName);
    } else if (keyType == UUID.class) {
      return keyType.cast(UUID.fromString(fileName));
    }

    String json = new String(Base64.getDecoder().decode(fileName));
    return this.deserializeKey(json);
  }

  private String readValue(File file) {
    try {
      return Files.readString(file.toPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeValue(File file, String json) {
    Path path = file.toPath();
    try {
      if (!file.exists()) {
        Files.createFile(path);
      }
      Files.writeString(path, json);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private V readValueFromKey(K key) {
    File file = this.getFile(key);
    if (!file.exists()) {
      return null;
    }
    String json = this.readValue(file);
    return this.deserializeValue(json);
  }

  private String serializeKey(K key) {

    if (keyType.isPrimitive()) {
      return String.valueOf(key);
    } else if (keyType == String.class) {
      return (String) key;
    } else if (keyType == UUID.class) {
      return key.toString();
    }

    String json = this.serializer.serialize(key);
    return Base64.getEncoder().encodeToString(json.getBytes());
  }

  private K deserializeKey(String serializedKey) {
    return this.serializer.deserialize(serializedKey, this.keyType);
  }

  private V deserializeValue(String serializedValue) {
    return this.serializer.deserialize(serializedValue, valueType);
  }

  private String serializeValue(V value) {
    return this.serializer.serialize(value);
  }

  @Override
  public int size() {
    File[] files = this.folder.listFiles();
    return files == null ? 0 : files.length;
  }

  @Override
  public boolean isEmpty() {
    return this.size() == 0;
  }

  @Override
  public boolean containsKey(Object key) {
    if (!this.keyType.isInstance(key)) {
      return false;
    }
    return getFile(this.keyType.cast(key)).exists();
  }

  @Override
  public boolean containsValue(Object value) {
    File[] files = this.folder.listFiles();
    if (files == null) {
      return false;
    }
    for (File file : files) {
      String json = readValue(file);
      V deserialized = this.deserializeValue(json);
      if (deserialized.equals(value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public V get(Object key) {
    if (!this.keyType.isInstance(key)) {
      return null;
    }
    return this.readValueFromKey(this.keyType.cast(key));
  }

  @Nullable
  @Override
  public V put(K key, V value) {
    File file = this.getFile(key);
    V replaced = null;
    if (file.exists()) {
      replaced = this.readValueFromKey(key);
      if (!file.delete()) {
        throw new IllegalStateException("Failed to delete file: " + file);
      }
    }

    String json = this.serializeValue(value);
    this.writeValue(file, json);

    return replaced;
  }

  public void fastPut(K key, V value) {
    File file = this.getFile(key);
    String json = this.serializeValue(value);
    this.writeValue(file, json);
  }

  @Override
  public V remove(Object key) {
    if (!this.keyType.isInstance(key)) {
      return null;
    }

    K typedKey = this.keyType.cast(key);
    File file = this.getFile(typedKey);

    if (!file.exists()) {
      return null;
    }

    V removed = this.readValueFromKey(typedKey);

    if (!file.delete()) {
      throw new IllegalStateException("Failed to delete file: " + file);
    }

    return removed;
  }

  public void fastRemove(K key) {
    File file = this.getFile(key);
    if (file.exists()) {
      if (!file.delete()) {
        throw new IllegalStateException("Failed to delete file: " + file);
      }
    }
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> map) {
    map.forEach(this::put);
  }

  @Override
  public void clear() {
    File[] files = this.folder.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      if (!file.delete()) {
        throw new IllegalStateException("Failed to delete file: " + file);
      }
    }
  }

  @NotNull
  @Override
  public Set<K> keySet() {
    File[] files = this.folder.listFiles();
    if (files == null) {
      return Set.of();
    }
    Set<K> keys = new HashSet<>();
    for (File file : files) {
      keys.add(this.keyFromFileName(file.getName()));
    }
    return keys;
  }

  @NotNull
  @Override
  public Collection<V> values() {
    File[] files = this.folder.listFiles();
    if (files == null) {
      return Set.of();
    }
    List<V> values = new ArrayList<>();
    for (File file : files) {
      String json = this.readValue(file);
      values.add(this.deserializeValue(json));
    }
    return values;
  }

  @NotNull
  @Override
  public Set<Entry<K, V>> entrySet() {
    File[] files = this.folder.listFiles();
    if (files == null) {
      return Set.of();
    }
    Set<Entry<K, V>> entries = new HashSet<>();
    for (File file : files) {
      K key = this.keyFromFileName(file.getName());
      V value = this.readValueFromKey(key);
      if (value == null) {
        continue;
      }
      entries.add(Map.entry(key, value));
    }
    return entries;
  }
}
