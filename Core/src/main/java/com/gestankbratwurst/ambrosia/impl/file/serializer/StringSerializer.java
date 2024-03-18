package com.gestankbratwurst.ambrosia.impl.file.serializer;

public interface StringSerializer {

  String serialize(Object object);

  <T> T deserialize(String string, Class<T> type);

}
