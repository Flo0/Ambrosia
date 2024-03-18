package com.gestankbratwurst.ambrosia.impl.pdc;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public interface PDCSerializer {

  <T> void save(NamespacedKey key, PersistentDataContainer container, T value);

  <T> T load(NamespacedKey key, PersistentDataContainer container, Class<T> type);

  <T> NamespacedKey serializeKey(T key);

  <T> T deserializeKey(NamespacedKey key, Class<T> type);

  <K> PersistentDataType<?, ?> getDataType(K key);

}
