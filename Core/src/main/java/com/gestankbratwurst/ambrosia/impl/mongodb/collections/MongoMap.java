package com.gestankbratwurst.ambrosia.impl.mongodb.collections;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import com.mongodb.lang.Nullable;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class MongoMap<K, V> implements Map<K, V> {

  private final MongoCollection<V> mongoBackbone;
  private final Class<K> keyClass;

  public MongoMap(MongoCollection<V> mongoBackbone, Class<K> keyClass) {
    this.keyClass = keyClass;
    this.mongoBackbone = mongoBackbone;
  }

  /**
   * Queries the database for the size of the collection.
   * Might overflow if the collections size exceeds Integer.MAX_VALUE.
   * This is a blocking operation.
   *
   * @return The size of the collection.
   */
  @Override
  public int size() {
    return (int) this.mongoBackbone.countDocuments();
  }

  /**
   * Queries if the collection is empty.
   *
   * @return True if the collection is empty.
   */
  @Override
  public boolean isEmpty() {
    return this.size() == 0;
  }

  /**
   * Queries the database for the existence of a key.
   * Only keys of the type K are accepted.
   *
   * @param key The key to check for.
   * @return True if the key exists in the collection.
   */
  @Override
  public boolean containsKey(Object key) {
    if (!this.mongoBackbone.getDocumentClass().isInstance(key)) {
      return false;
    }

    return this.mongoBackbone.countDocuments(Filters.eq(key)) > 0;
  }

  /**
   * Queries the database for the existence of a value.
   * This method consecutively checks all values in the collection and compares them to the given value,
   * which is expensive for large collections.
   *
   * @param value The value to check for.
   * @return True if the value exists in the collection.
   */
  @Override
  public boolean containsValue(Object value) {
    try (MongoCursor<V> cursor = this.mongoBackbone.find().iterator()) {
      while (cursor.hasNext()) {
        if (cursor.next().equals(value)) {
          return true;
        }
      }
    }
    return false;
  }


  /**
   * Queries the database for a value by key.
   * Only keys of the type K are accepted.
   *
   * @param key The key to query for.
   * @return The value associated with the key or null if the key does not exist.
   */
  @Nullable
  @Contract("null -> null")
  @Override
  public V get(Object key) {
    if (!this.keyClass.isInstance(key)) {
      return null;
    }
    return this.mongoBackbone.find(Filters.eq(key)).first();
  }

  /**
   * Puts a value into the collection by key.
   * If the key already exists, the old value is replaced and returned.
   * Uses the upsert option to insert the value if the key does not exist.
   *
   * @param key   The key to put the value under.
   * @param value The value to put.
   * @return The old value associated with the key or null if the key did not exist.
   */
  @Nullable
  @Override
  public V put(@NotNull K key, @NotNull V value) {
    ReplaceOptions options = new ReplaceOptions().upsert(true);
    V replaced = this.get(key);
    this.mongoBackbone.replaceOne(Filters.eq(key), value, options);
    return replaced;
  }

  /**
   * Puts a value into the collection by key.
   * Uses the upsert option to insert the value if the key does not exist.
   * Does not return the old value.
   *
   * @param key   The key to put the value under.
   * @param value The value to put.
   */
  public void fastPut(@NotNull K key, @NotNull V value) {
    ReplaceOptions options = new ReplaceOptions().upsert(true);
    this.mongoBackbone.replaceOne(Filters.eq(key), value, options);
  }

  /**
   * Removes a value from the collection by key.
   * Only keys of the type K are accepted.
   * This method is quite expensive, as it first queries the old value and then removes it.
   *
   * @param key The key to remove the value from.
   * @return The old value associated with the key or null if the key did not exist.
   */
  @Nullable
  @Override
  public V remove(@NotNull Object key) {
    if (!this.keyClass.isInstance(key)) {
      return null;
    }
    V replaced = this.get(key);
    this.mongoBackbone.deleteOne(Filters.eq(key));
    return replaced;
  }

  /**
   * Removes a value from the collection by key.
   * Does not return the old value.
   * Only keys of the type K are accepted.
   *
   * @param key The key to remove the value from.
   */
  public void fastRemove(@NotNull Object key) {
    if (!this.keyClass.isInstance(key)) {
      return;
    }
    this.mongoBackbone.deleteOne(Filters.eq(key));
  }

  /**
   * Puts all key-value pairs from the given map into the collection.
   *
   * @param map The map to put into the collection.
   */
  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> map) {
    map.forEach(this::fastPut);
  }

  /**
   * Removes all key-value pairs from the collection.
   * This method drops the entire collection.
   */
  @Override
  public void clear() {
    this.mongoBackbone.drop();
  }

  /**
   * Queries the database for all keys in the collection.
   * This method queries the distinct values of the _id field.
   *
   * @return A set of all keys in the collection.
   */
  @Override
  @NotNull
  public Set<K> keySet() {
    Set<K> keys = new HashSet<>();
    this.mongoBackbone.distinct("_id", this.keyClass).into(keys);
    return keys;
  }

  /**
   * Queries the database for all values in the collection.
   *
   * @return A collection of all values in the collection.
   */
  @NotNull
  @Override
  public Collection<V> values() {
    List<V> values = new ArrayList<>();
    this.mongoBackbone.find().into(values);
    return values;
  }

  /**
   * Queries the database for all key-value pairs in the collection.
   * This method first queries all keys and then queries all values by key and
   * is therefore expensive for large collections.
   *
   * @return A set of all key-value pairs in the collection.
   */
  @NotNull
  @Override
  public Set<Entry<K, V>> entrySet() {
    Map<K, V> map = new HashMap<>();
    for (K key : this.keySet()) {
      map.put(key, this.get(key));
    }
    return map.entrySet();
  }

  /**
   * Queries the database for all values that have a certain property.
   * The values are eagerly loaded into a list.
   *
   * @param property The property to query for.
   * @param value    The value the property should have.
   * @param <E>      The type of the value.
   * @return A list of all values that have the given property set to the given value.
   */
  @NotNull
  public <E> List<V> findByProperty(@NotNull String property, E value) {
    return this.query(coll -> coll.find(Filters.eq(property, value)), iter -> {
      List<V> values = new ArrayList<>();
      iter.into(values);
      return values;
    });
  }

  /**
   * Queries the database for the top n values sorted by a property.
   * The values are eagerly loaded into a list.
   *
   * @param property  The property to sort by.
   * @param limit     The maximum amount of values to return.
   * @param ascending True if the values should be sorted in ascending order.
   * @return A list of the top n values sorted by the given property.
   */
  @NotNull
  public List<V> queryToplist(@NotNull String property, int limit, boolean ascending) {
    Bson sort = ascending ? Sorts.ascending(property) : Sorts.descending(property);
    return this.query(MongoCollection::find, iter -> {
      List<V> values = new ArrayList<>();
      iter.sort(sort).limit(limit).into(values);
      return values;
    });
  }

  /**
   * Queries the database with a custom query function and a custom result function.
   * The query function is applied to the collection and the result function is applied to the query result.
   *
   * @param queryFunction  The function to query the database with.
   * @param resultFunction The function to process the query result with.
   * @param <I>            The type of the query result.
   * @param <R>            The type of the processed result.
   * @return The processed result.
   */
  @NotNull
  public <I, R> R query(@NotNull Function<MongoCollection<V>, I> queryFunction, @NotNull Function<I, R> resultFunction) {
    return resultFunction.apply(queryFunction.apply(this.mongoBackbone));
  }

  /**
   * Queries the database for a distinct property of a value by key.
   * Only keys of the type K are accepted.
   *
   * @param key      The key to query for.
   * @param property The property to query for.
   * @param type     The type of the property.
   * @param <E>      The type of the property.
   * @return The distinct property of the value associated with the key or null if the key does not exist.
   */
  public <E> E queryProperty(K key, @NotNull String property, Class<E> type) {
    if (!this.keyClass.isInstance(key)) {
      return null;
    }
    return this.mongoBackbone.distinct(property, Filters.eq(key), type).first();
  }

  /**
   * Queries the database for all values associated with a set of keys.
   * The values are eagerly loaded into a map.
   * The returned map only contains the values that exist in the collection and does not contain null values.
   *
   * @param keys The keys to query for.
   * @return A map of all values associated with the given keys.
   */
  @NotNull
  public Map<K, V> getAll(@NotNull Set<K> keys) {
    Map<K, V> map = new HashMap<>();
    for (K key : keys) {
      V value = this.get(key);
      if (value != null) {
        map.put(key, value);
      }
    }
    return map;
  }

  /**
   * Creates a MongoCursor for all keys in the collection.
   * This method queries the distinct values of the _id field.
   *
   * @return An iterator of all distinct keys in the collection.
   */
  public Iterator<K> keyIterator() {
    return this.mongoBackbone.distinct("_id", this.keyClass).iterator();
  }

  /**
   * Applies a consumer to a value by key and replaces the value in the collection.
   * This loads an object into memory and writes it back to the database.
   * This is useful for in-place modifications of values but the query method should be used for a better performance.
   *
   * @param key      The key to apply the consumer to.
   * @param consumer The consumer to apply to the value.
   * @return The value (post modification) associated with the key or null if the key does not exist.
   */
  public V apply(K key, Consumer<V> consumer) {
    V value = this.get(key);
    if (value == null) {
      return null;
    }
    consumer.accept(value);
    this.fastPut(key, value);
    return value;
  }

  /**
   * Applies a function to a value by key.
   * ! This does not modify the value in the collection !
   * This method loads all keys into memory and is therefore expensive for large collections.
   *
   * @param action The action to each key-value pair.
   */
  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    Set<K> keys = this.keySet();
    for (K key : keys) {
      action.accept(key, this.get(key));
    }
  }

  MongoCollection<V> getBackbone() {
    return this.mongoBackbone;
  }
}
