package com.gestankbratwurst.ambrosia;

import com.gestankbratwurst.ambrosia.impl.file.FileAmbrosia;
import com.gestankbratwurst.ambrosia.impl.mongodb.MongoAmbrosia;
import com.gestankbratwurst.ambrosia.impl.redisson.RedissonAmbrosia;

import java.util.Map;

public abstract class Ambrosia<T, M extends Map<?, ?>> {

  public static MongoAmbrosia.Builder<?> mongoDB() {
    return MongoAmbrosia.builder();
  }

  public static RedissonAmbrosia.Builder<?> redisson() {
    return RedissonAmbrosia.builder();
  }

  public static FileAmbrosia.Builder<?> toFiles() {
    return FileAmbrosia.builder();
  }

  private final T backbone;

  public Ambrosia(T backbone) {
    this.backbone = backbone;
  }

  public T getBackbone() {
    return this.backbone;
  }

  public abstract <K, V> M createMapView(String mapName, Class<K> keyType, Class<V> valueType);

}
