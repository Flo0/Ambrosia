[![](https://jitpack.io/v/Flo0/Ambrosia.svg)](https://jitpack.io/#Flo0/Ambrosia)

# Ambrosia

Ambrosia is an easy-to-use library for persisting your objects. It was primarily designed for MongoDB, but also
comes with implementations for storing your data in Files, Redis and PDCs.

It provides a java.util.Map implementations which enable quick key-value storage and retrieval,
while still allowing complex queries on the underlying backbone.

The default Codec is backed by Gson.

# SpigotAmbrosia

This module extends Ambrosia and adds support for all ConfigurationSerailizable types on default.

## Quickstart

Using the [MongoDB](https://mvnrepository.com/artifact/org.mongodb/mongodb-driver-sync) or [Redisson](https://mvnrepository.com/artifact/org.redisson/redisson) implementation requires the respective client libraries to be present in your classpath.

If you are using the default [Gson](https://mvnrepository.com/artifact/com.google.code.gson/gson) codec, you need to include the Gson library in your classpath.
Spigot users dont need to include Gson, as it is already included in the Spigot API.

Unused implementations can just be ignored and don't require any additional dependencies.

### Maven
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```
```xml
<dependency>
    <groupId>com.github.Flo0</groupId>
    <artifactId>Ambrosia</artifactId>
    <version>Tag</version>
</dependency>
```

### Gradle (Kotlin DSL)
```kt
repositories {
    ...
    maven {
        url = uri("https://jitpack.io")
    }
}
```
```kt
dependencies {
    implementation("com.gestankbratwurst.ambrosia:AmbrosiaSpigot:Tag")
}
```

## Creating an Ambrosia instance

Ambrosia uses a builder pattern to create an instance.

The default SpigotCapableAmbrosia implementation only requires a MongoDatabase instance and is backed by Gson.
### Spigot
```java
public final class SpigotSandbox extends JavaPlugin {
  @Override
  public void onEnable() {
    // Setup MongoDB (UuidRepresentation.STANDARD is important for UUID keys)
    ConnectionString connectionString = new ConnectionString("mongodb://user:pw@127.0.0.1:27017");
    MongoClientSettings settings = MongoClientSettings.builder()
        .uuidRepresentation(UuidRepresentation.STANDARD)
        .applyConnectionString(connectionString)
        .build();
    MongoClient mongoClient = MongoClients.create(settings);
    MongoDatabase database = mongoClient.getDatabase("SandboxDB");

    // Create an Ambrosia instance
    MongoAmbrosia mongoAmbrosia = SpigotCapableAmbrosia.mongoDB()
        .database(database)
        .build();
    
    // Alternative:
    // Adding additional configuration to the Spigot capable Gson instance
    MongoAmbrosia mongoAmbrosia = SpigotCapableAmbrosia.mongoDB()
        .construct(GsonBuilder::disableHtmlEscaping)
        .construct(builder -> builder.registerTypeAdapter(CustomObj.class, new CustomObjAdapter()))
        .database(database)
        .build();
  }
}
```
### General
```java
public class Sandbox {
  public static void main(String[] args) {
    ...
    MongoClient mongoClient = MongoClients.create(settings);
    MongoDatabase database = mongoClient.getDatabase("SandboxDB");

    // Using a custom codec registry
    CodecRegistry yourCustomCodec = ...;
    MongoAmbrosia ambrosia = Ambrosia.mongoDB()
        .codecRegistry(yourCustomCodec)
        .database(database)
        .build();

    // Using a custom Gson instance for the codec registry
    Gson someGsonInstance = ...;
    MongoAmbrosia ambrosia = Ambrosia.mongoDB()
        .gson(someGsonInstance)
        .database(database)
        .build();

    // Constructing a new Gson instance with custom settings
    MongoAmbrosia ambrosia = Ambrosia.mongoDB()
        .gsonBuild()
        .construct(GsonBuilder::disableHtmlEscaping)
        .construct(GsonBuilder::enableComplexMapKeySerialization)
        .construct(builder -> builder.registerTypeAdapter(...))
        .database(database)
        .build();
  }
}
```
### Other implementations

Each implementation can be provided with a custom Codec which is not backed by Gson if needed.

Storing data in files is also supported. Spigot users can use the getDataFolder() method to get the folder where the data should be stored in.
The FileAmbrosia implementation is backed by Gson and creates .json files in the specified folder.

### Storing in a Folder
```java
public final class SpigotSandbox extends JavaPlugin {
  @Override
  public void onEnable() {
    // Get the folder where the data should be stored in (Spigot users can use getDataFolder())
    File dataFolder = getDataFolder();

    // Create an Ambrosia instance, backed by files
    FileAmbrosia fileAmbrosia = SpigotCapableAmbrosia.toFiles()
        .folder(dataFolder)
        .construct(GsonBuilder::setPrettyPrinting)
        .build();
  }
}
```

### Storing in Redis
```java
public final class SpigotSandbox extends JavaPlugin {
  @Override
  public void onEnable() {
    // Create a RedissonClient instance
    RedissonClient redissonClient = Redisson.create();

    // Create an Ambrosia instance, backed by files
    RedissonAmbrosia redissonAmbrosia = SpigotCapableAmbrosia.redisson()
        .client(redissonClient)
        .build();
  }
}
```

Non-Spigot users can use the general builder pattern to create an Ambrosia instance by calling the static builder method on the Ambrosia class.


### Storing in a PDC
```java
public final class SpigotSandbox extends JavaPlugin {
  @Override
  public void onEnable() {
    // Get the PersistentDataContainer of the main world (Can be any PDC on runtime)
    // and you can create ambrosia instances and use PDC on the main thread.
    PersistentDataContainer container = Bukkit.getWorlds().get(0).getPersistentDataContainer();

    // Create an Ambrosia instance, backed by files
    PDCAmbrosia pdcAmbrosia = SpigotCapableAmbrosia.pdc()
        .container(container)
        .plugin(this) // This is optional. If not used, the namespace will be minecraft
        .build();
  }
}
```

# Using Ambrosia
## MongoDB
### Creating a codec backed collection
```java
MongoCollection<SomeCoolObj> collection = ambrosia.createMongoCollection("CollectionName", SomeCoolObj.class);
```

### Creating and using a MongoMap
```java
// Create a MongoMap with UUID keys and SomeCoolObj values
// Preferably use a field to store the map
MongoMap<UUID, SomeCoolObj> map = ambrosia.createMapView("CollectionName", UUID.class, SomeCoolObj.class);
...
SomeCoolObj coolObj = ...;
UUID key = coolObj.getUuid();
// Store the object in MongoDB
SomeCoolObj replaced = map.put(key, coolObj);
// If return value is not used
map.fastPut(key, coolObj);
```

### Queries

MongoMap has a method to create a query builder.
It also provides a toplist query method and a method to query single properties.

### Toplist queries

```java
MongoMap<UUID, SomeCoolObj> map = ambrosia.createMapView("CollectionName", UUID.class, SomeCoolObj.class);

// Query a top list of 10 objects, ordered by the "killCount" field
boolean ascending = false;
List<SomeCoolObj> orderedList = remoteMap.queryToplist("killCount", 10, ascending);
```
### Query single properties
Sometimes you would like to get a single property of an object, without loading the entire object.
```java
MongoMap<UUID, SomeCoolObj> map = ambrosia.createMapView("CollectionName", UUID.class, SomeCoolObj.class);

UUID somePlayerId = ...;
int kills = map.queryProperty(somePlayerId, "killCount", Integer.class);
```
### Custom queries
The MongoMap also allows you to use custom queries directly on the underlying MongoCollection.

In this example we query all objects with a "killCount" greater than 10 and less than 20.
```java
Bson greaterLimit = Filters.gt("killCount", 10);
Bson lesserLimit = Filters.lt("killCount", 20);
Bson combinedLimit = Filters.and(lesserLimit, greaterLimit);
List<SomeCustomObj> objects = map.query(MongoCollection::find, cursor -> {
  List<SomeCustomObj> results = new ArrayList<>();
  cursor.filter(combinedLimit);
  cursor.into(results);
  return results;
});
```

## Files

### Creating a file backed Map
```java
String subFolderName = "SomeCoolData"
FileMap<UUID, SomeCoolObj> map = ambrosia.createMapView(subFolderName, UUID.class, SomeCoolObj.class);

// Store an object in the map
SomeCoolObj coolObj = ...;
UUID key = coolObj.getUuid();

// This will create a file in the specified folder with the <UUID>.json as the file name
SomeCoolObj replaced = map.put(key, coolObj);
// If return value is not used
map.fastPut(key, coolObj);
```

## Redisson

### Creating a Redisson backed Map
```java
RMap<UUID, SomeObj> map = ambrosia.createMapView("SomeMapName", UUID.class, SomeObj.class);

// Store an object in the map
SomeCoolObj coolObj = ...;
UUID key = coolObj.getUuid();

// This can be directly used as a normal map in Redis
SomeCoolObj replaced = map.put(key, coolObj);
// If return value is not used
map.fastPut(key, coolObj);
```

## PDC

### Creating a PDC backed Map
```java
Map<String, SomeObj> map = ambrosia.createMapView(String.class, SomeObj.class);
// Store an object in the map
SomeCoolObj coolObj = ...;
UUID key = coolObj.getUuid();

// This can be directly used as a normal map in Redis
SomeCoolObj replaced = map.put(key, coolObj);
```