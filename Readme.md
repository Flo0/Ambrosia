[![](https://jitpack.io/v/Flo0/Ambrosia.svg)](https://jitpack.io/#Flo0/Ambrosia)

# Ambrosia

Ambrosia is an easy-to-use library for interacting with MongoDB.
It provides a java.util.Map implementation which enables quick key-value storage and retrieval,
while still allowing complex queries on the underlying MongoDB collection.

The default Codec is backed by Gson.

# SpigotAmbrosia

This module extends Ambrosia and adds support for all ConfigurationSerailizable types on default.

## Quickstart

For the non-Spigot version simply replace `AmbrosiaSpigot` with `Ambrosia` in the following examples.


### Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
```xml
<dependency>
    <groupId>com.gestankbratwurst.ambrosia</groupId>
    <artifactId>AmbrosiaSpigot</artifactId>
    <version>1.0-SNAPSHOT</version>
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
    implementation("com.gestankbratwurst.ambrosia:AmbrosiaSpigot:1.0-SNAPSHOT")
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
    Ambrosia ambrosia = SpigotCapableAmbrosia.create(database);
    
    // Alternative:
    // Adding additional configuration to the Spigot capable Gson instance
    Ambrosia ambrosia = SpigotCapableAmbrosia.create(database, builder -> {
      builder.disableHtmlEscaping();
      builder.enableComplexMapKeySerialization();
      builder.registerTypeAdapter(UUID.class, new UUIDAdapter());
      ...
    });
  }
}
```
### General
```java
public static void main(String[] args) {
  ...
  MongoClient mongoClient = MongoClients.create(settings);
  MongoDatabase database = mongoClient.getDatabase("SandboxDB");

  // Using a custom codec registry
  CodecRegistry yourCustomCodec = ...;
  Ambrosia ambrosia = Ambrosia.builder()
    .codecRegistry(yourCustomCodec)
    .database(database)
    .build();

  // Using a custom Gson instance for the codec registry
  Gson someGsonInstance = ...;
  Ambrosia ambrosia = Ambrosia.builder()
    .gson(someGsonInstance)
    .database(database)
    .build();

  // Constructing a new Gson instance with custom settings
  Ambrosia ambrosia = Ambrosia.builder()
    .gsonBuild()
    .construct(builder -> builder.disableHtmlEscaping())
    .construct(builder -> builder.enableComplexMapKeySerialization())
    .construct(builder -> builder.registerTypeAdapter(...))
    .database(database)
    .build();
}
```

## Using Ambrosia

### Creating a codec backed collection
```java
MongoCollection<SomeCoolObj> collection = ambrosia.createMongoCollection("CollectionName", SomeCoolObj.class);
```

### Creating and using a MongoMap
```java
// Create a MongoMap with UUID keys and SomeCoolObj values
// Preferably use a field to store the map
Map<UUID, SomeCoolObj> map = ambrosia.createMongoMap("CollectionName", UUID.class, SomeCoolObj.class);
...
SomeCoolObj coolObj = ...;
UUID key = coolObj.getUuid();
// Store the object in MongoDB
map.put(key, coolObj);
```

## Queries

MongoMap has a method to create a query builder.
It also provides a toplist query method and a method to query single properties.

### Toplist queries

```java
MongoMap<UUID, SomeCoolObj> map = ambrosia.createMongoMap("CollectionName", UUID.class, SomeCoolObj.class);

// Query a top list of 10 objects, ordered by the "killCount" field
boolean ascending = false;
List<SomeCoolObj> orderedList = remoteMap.queryToplist("killCount", 10, ascending);
```
### Query single properties
Sometimes you would like to get a single property of an object, without loading the entire object.
```java
MongoMap<UUID, SomeCoolObj> map = ambrosia.createMongoMap("CollectionName", UUID.class, SomeCoolObj.class);

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