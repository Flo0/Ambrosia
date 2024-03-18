package com.gestankbratwurst.ambrosia.impl.pdc;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PDCMap<K, V> implements Map<K, V> {

  private final PersistentDataContainer container;
  private final PDCSerializer serializer;
  private final Class<K> keyType;
  private final Class<V> valueType;

  public PDCMap(PersistentDataContainer container, PDCSerializer serializer, Class<K> keyType, Class<V> valueType) {
    this.serializer = serializer;
    this.container = container;
    this.keyType = keyType;
    this.valueType = valueType;
  }

  @Override
  public int size() {
    return this.container.getKeys().size();
  }

  @Override
  public boolean isEmpty() {
    return this.container.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    if (!this.keyType.isInstance(key)) {
      return false;
    }
    K typedKey = this.keyType.cast(key);
    NamespacedKey namespacedKey = this.serializer.serializeKey(typedKey);
    return this.container.has(namespacedKey, PersistentDataType.STRING);
  }

  @Override
  public boolean containsValue(Object value) {
    Set<K> keys = this.keySet();
    for (K key : keys) {
      V val = this.get(key);
      if (val.equals(value)) {
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
    K typedKey = this.keyType.cast(key);
    NamespacedKey namespacedKey = this.serializer.serializeKey(typedKey);
    return this.serializer.load(namespacedKey, this.container, this.valueType);
  }

  @Override
  public V put(K key, V value) {
    NamespacedKey namespacedKey = this.serializer.serializeKey(key);
    V oldValue = this.get(key);
    this.serializer.save(namespacedKey, this.container, value);
    return oldValue;
  }

  public void fastPut(K key, V value) {
    NamespacedKey namespacedKey = this.serializer.serializeKey(key);
    this.serializer.save(namespacedKey, this.container, value);
  }

  @Override
  public V remove(Object key) {
    if (!this.keyType.isInstance(key)) {
      return null;
    }
    K typedKey = this.keyType.cast(key);
    NamespacedKey namespacedKey = this.serializer.serializeKey(typedKey);
    V oldValue = this.get(key);
    this.container.remove(namespacedKey);
    return oldValue;
  }

  public void fastRemove(K key) {
    NamespacedKey namespacedKey = this.serializer.serializeKey(key);
    this.container.remove(namespacedKey);
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> map) {
    map.forEach(this::put);
  }

  @Override
  public void clear() {
    this.container.getKeys().forEach(this.container::remove);
  }

  @Override
  public @NotNull Set<K> keySet() {
    return this.container.getKeys().stream().map(nKey -> this.serializer.deserializeKey(nKey, this.keyType)).collect(Collectors.toSet());
  }

  @Override
  public @NotNull Collection<V> values() {
    return this.keySet().stream().map(this::get).collect(Collectors.toList());
  }

  @Override
  public @NotNull Set<Entry<K, V>> entrySet() {
    return this.keySet().stream().map(key -> Map.entry(key, this.get(key))).collect(Collectors.toSet());
  }
}
