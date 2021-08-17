### StringObjectMap

A useful lib to work with Map with string key andy any objecy
<hr>

```xml

<dependency>
    <groupId>io.github.devlibx.easy</groupId>
    <artifactId>helper</artifactId>
    <version>0.0.56</version>
</dependency>

```

#### Path finding

This map allows a way to find the data in sub-path. It is like finding path using json path "$.body.id". However, we
only support non-array path e.g. "body.id". <br><br>
This is a custom implementation and does not uses JsonPath lib

```java
public class Example {
    public void pathExample() {
        StringObjectMap map = JsonUtils.convertAsStringObjectMap("some json string");

        // Read a value in path "body.entity.id"
        Integer intValue = map.path(".", "body.user.id", Integer.class);
        System.out.println(intValue);

        // Read a value in path "body.entity.name"
        String stringValue = map.path(".", "body.user.name", String.class);
        System.out.println(stringValue);

        // Check value at path
        boolean result = false;
        result = map.isPathValueTrue("body.user.enabled");
        result = map.isPathValueFalse("body.user.deleted");

        // Check value at path (you can pass int, boolean, or any other object)
        result = map.isPathValueEqual("body.user.id", 11);
    }
}
```

<hr>

#### Processing Queue for Spark

There are times when you can read from underlying source, but the sink is slow. You may have to put data in sink with
many threads. You can use ProcessQueue for the same.

FYI - I used this code in scala project - mix of java and scala syntax 
```scala
class Example {
  public void example() {

    // Part 1 - Client Code - What you want to do with Item?
    // No need to handle execution - if exception is thrown then we will retry
    val processor = new IProcessor[Item] {
      override def process(t: Item): Unit = {

        // Client code - Here you are doing the slow work (for example we ar putting data to DynamoDb table)
        table.putItem(t)
      }
    }

    // Part 2 - Setup and start the queue
    val queue = new ProcessorQueue[Item](
      100, /* Threads count - this will spin 100 threads and run the client code in parallel e.g. hit 
              DynamoDB from 100 threads */

      1024 * 10, /* Queue buffer - you can put any number. Your below code where you call "processItem"  will block if 
                    events are in this queue is not processed by worker threads */

      30, /* The worker threads wait for N second to get items from Queue - if anytime they don't get items for N sec 
             they consider that they don't have work and die */

      -1, // How many time a items will retry i.e. your IProcessor.process() function retry count

      -1, // How long to wait for retry for your IProcessor.process() function

      processor // Your function which will do the work
    )

    // Part 3 - start this queue (you will get back the latch - you can wait on latch to see if processed is completed or not)
    // Use this latch to wait 
    val latch = queue.start();

    // Client Code - this is the data source
    spark.read.parquet(file).foreach(row => {
      var item = new Item()
        .withPrimaryKey("id", "new" + row.getString(0), "something", "*")
        .withString("description", "some stuff")

      // Part 4 - Submit in queue to process the object
      queue.processItem(item)

    })

    // Part 5 - wait for queue to empty everything
    latch.await()

  }
}
```

###### Dependency
You may have to exclude jackson from easy lib if it conflicts with Spark. Also use the latest version for lib. 
```xml
 <dependency>
    <groupId>io.github.devlibx.easy</groupId>
    <artifactId>helper</artifactId>
    <version>0.0.57</version>
    <exclusions>
        <exclusion>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>*</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```