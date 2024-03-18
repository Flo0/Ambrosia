package com.gestankbratwurst.ambrosia.impl.pdc;

import com.google.gson.Gson;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.ref.WeakReference;
import java.util.Base64;
import java.util.UUID;

public class PDCGsonSerializer implements PDCSerializer {

  private final Gson gson;
  private final WeakReference<JavaPlugin> pluginRef;

  public PDCGsonSerializer(Gson gson, JavaPlugin plugin) {
    this.gson = gson;
    if (plugin == null) {
      this.pluginRef = null;
    } else {
      this.pluginRef = new WeakReference<>(plugin);
    }
  }

  @Override
  public <T> void save(NamespacedKey key, PersistentDataContainer container, T value) {
    String json = this.gson.toJson(value);
    container.set(key, PersistentDataType.STRING, json);
  }

  @Override
  public <T> T load(NamespacedKey key, PersistentDataContainer container, Class<T> type) {
    String json = container.get(key, PersistentDataType.STRING);
    return this.gson.fromJson(json, type);
  }

  @Override
  public <T> NamespacedKey serializeKey(T key) {
    String nameKey;

    if (key.getClass().isPrimitive()) {
      nameKey = String.valueOf(key);
    } else if (key instanceof NamespacedKey) {
      nameKey = ((NamespacedKey) key).getKey();
    } else if (key instanceof String) {
      nameKey = (String) key;
    } else if (key instanceof UUID) {
      nameKey = key.toString();
    } else {
      String json = this.gson.toJson(key);
      nameKey = Base64.getEncoder().encodeToString(json.getBytes());
    }

    if (pluginRef == null) {
      return NamespacedKey.minecraft(nameKey);
    }
    JavaPlugin plugin = this.pluginRef.get();
    if (plugin == null) {
      throw new IllegalStateException("Plugin reference was garbage collected.");
    }
    return new NamespacedKey(plugin, nameKey);
  }

  @Override
  public <T> T deserializeKey(NamespacedKey key, Class<T> type) {
    if (type == NamespacedKey.class) {
      return type.cast(key);
    }

    String nameKey = key.getKey();

    if (type.isPrimitive()) {
      if (type == int.class) {
        return type.cast(Integer.parseInt(nameKey));
      } else if (type == long.class) {
        return type.cast(Long.parseLong(nameKey));
      } else if (type == short.class) {
        return type.cast(Short.parseShort(nameKey));
      } else if (type == byte.class) {
        return type.cast(Byte.parseByte(nameKey));
      } else if (type == float.class) {
        return type.cast(Float.parseFloat(nameKey));
      } else if (type == double.class) {
        return type.cast(Double.parseDouble(nameKey));
      } else if (type == char.class) {
        return type.cast(nameKey.charAt(0));
      } else if (type == boolean.class) {
        return type.cast(Boolean.parseBoolean(nameKey));
      } else {
        throw new IllegalArgumentException("Unknown primitive type: " + type);
      }
    } else if (type == String.class) {
      return type.cast(nameKey);
    } else if (type == UUID.class) {
      return type.cast(UUID.fromString(nameKey));
    } else {
      byte[] bytes = Base64.getDecoder().decode(nameKey);
      String json = new String(bytes);
      return this.gson.fromJson(json, type);
    }
  }

  @Override
  public <K> PersistentDataType<?, ?> getDataType(K key) {
    return PersistentDataType.STRING;
  }
}
