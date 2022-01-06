#### Latest Maven Dependency

```xml

<properties>
    <easy.version>0.0.59</adrastea.version>
</properties>
<!-- Get the latest version from https://mvnrepository.com/artifact/io.github.devlibx.easy/http -->
```

## Helper Library
The link to helper library which has many common useful utilities.
[Helper Library](https://github.com/devlibx/easy/tree/master/helper)

## Kafka Producer/Consumer Library
The link to Kafka library which has a easy way to produce or consume on Kafka.
[Kafka Library](https://github.com/devlibx/easy/tree/master/messaging-kafka)


## Easy Http

Http Module provides API to make HTTP calls. It ensures that APIs are called with circuit-breaker, time limit.

> Maven Dependency

```xml

<dependency>
    <groupId>io.github.devlibx.easy</groupId>
    <artifactId>http</artifactId>
    <version>${easy.version}</version>
</dependency>
```

> Calling http in Sync

```shell
// Example 1 - Make a call and get response in a Map
Map result = EasyHttp.callSync(
                Call.builder(Map.class)
                        .withServerAndApi("jsonplaceholder", "getPosts")
                        .addPathParam("id", 1)
                        .withBody("Any object - it will be converted to json string internally")
                        .build()
              );
                


// Example 2 - Make a call and get response in a Pojo                
@Data
private static class ResponsePojo {
  @JsonProperty("userId")
  private Integer userId;
  @JsonProperty("id")
  private Integer id;
  @JsonProperty("title")
  private String title;
  @JsonProperty("completed")
  private boolean completed;
}
                  
ResponsePojo resultWithPojo = EasyHttp.callSync(
        Call.builder(ResponsePojo.class)
                .withServerAndApi("jsonplaceholder", "getPosts")
                .addPathParam("id", 1)
                .build()
);
String jsonString = JsonUtils.asJson(resultWithPojo);
log.info("Print Result as Json String = " + jsonString);
// Print Result as Json String = {"userId":1,"id":1,"title":"sunt aut facere repellat provident occaecati excepturi optio reprehenderit","completed":false}



// Example 3 - Make a call and get process error
// EasyHttpExceptions.EasyHttpRequestException - this is the super class to catch all error (or you can use specific sub-classes) 
try {
    ResponsePojo resultWithPojoError = EasyHttp.callSync(
            Call.builder(ResponsePojo.class)
                    .withServerAndApi("jsonplaceholder", "getPosts")
                    .addPathParam("id_make_it_fail", 1)
                    .build()
    );

    // You can catch 
    // EasyHttpExceptions.Easy4xxException e1;
    // EasyHttpExceptions.EasyUnauthorizedRequestException e;
    // EasyHttpExceptions.EasyRequestTimeOutException e;
} catch (EasyHttpExceptions.Easy5xxException e) {
    // You can cache specific errors
    log.error("Api failed (5xx error): status=" + e.getStatusCode() + " byteBody=" + e.getBody());
} catch (EasyHttpExceptions.EasyHttpRequestException e) {
    log.error("Api failed: status=" + e.getStatusCode() + " byteBody=" + e.getBody());
}                
```

> Calling http in Async

```shell
EasyHttp.callAsync(
                Call.builder(Map.class)
                        .withServerAndApi("jsonplaceholder", "getPostsAsync")
                        .addPathParam("id", 1)
                        .withBody("Any object - it will be converted to json string internally")
                        .build()
        ).subscribe(
                result -> {
                    log.info("Print Result as Json String = " + JsonUtils.asJson(result));
                    // Result = {"userId":1,"id":1,"title":"some text ..."}
                },
                throwable -> {
                    // throwable is a EasyHttpRequestException
                    // You can visit sub-classes EasyHttpRequestException to get catch exact issue 
                    // e.g. EasyNotFoundException - for Http 404
                });
```

> Custom request and response body function. e.g. proto-buf API (over HTTP)

```shell script
AddUserRequest request = AddUserRequest.newBuilder()
                .setNameProvided(name)
                .build();
AddUserResponse response = EasyHttp.callSync(
        Call.builder(AddUserResponse.class)
                .withServerAndApi("someService", "someApi")
                .asContentTypeProtoBuffer()
                .withResponseBuilder(AddUserResponse::parseFrom)
                .withRequestBodyFunc(request::toByteArray)
                .build()
);
``` 

##### Example

```java
package io.github.devlibx.easy.http;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.LoggingHelper;
import io.gitbub.devlibx.easy.helper.json.JsonUtils;
import io.gitbub.devlibx.easy.helper.yaml.YamlUtils;
import io.github.devlibx.easy.http.config.Config;
import io.github.devlibx.easy.http.module.EasyHttpModule;
import io.github.devlibx.easy.http.sync.SyncRequestProcessor;
import io.github.devlibx.easy.http.util.Call;
import io.github.devlibx.easy.http.util.EasyHttp;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.apache.log4j.Level.TRACE;

@Slf4j
public class DemoApplication extends TestCase {
    private Injector injector;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LoggingHelper.setupLogging();
        Logger.getLogger(SyncRequestProcessor.class).setLevel(TRACE);

        // Setup injector (Onetime MUST setup before we call EasyHttp.setup())
        injector = Guice.createInjector(new EasyHttpModule());
        ApplicationContext.setInjector(injector);

        // Read config and setup EasyHttp
        Config config = YamlUtils.readYamlCamelCase("demo_app_config.yaml", Config.class);
        EasyHttp.setup(config);
    }

    public void testSyncApiCall() {
        Map result = EasyHttp.callSync(
                Call.builder(Map.class)
                        .withServerAndApi("jsonplaceholder", "getPosts")
                        .addPathParam("id", 1)
                        .build()
        );

        log.info("Print Result as Json String = " + JsonUtils.asJson(result));
        // Result = {"userId":1,"id":1,"title":"some text ..."}
    }

    public void testAsyncApiCall() throws Exception {
        CountDownLatch waitForComplete = new CountDownLatch(1);
        EasyHttp.callAsync(
                Call.builder(Map.class)
                        .withServerAndApi("jsonplaceholder", "getPostsAsync")
                        .addPathParam("id", 1)
                        .build()
        ).subscribe(
                result -> {
                    log.info("Print Result as Json String = " + JsonUtils.asJson(result));
                    // Result = {"userId":1,"id":1,"title":"some text ..."}
                },
                throwable -> {

                });
        waitForComplete.await(5, TimeUnit.SECONDS);
        // Or you can use blockingSubscribe(); 
    }
}
```

###### YAML File to configure all APIs and Server URL for above example

demo_app_config.yaml

```yaml
servers:
  jsonplaceholder:
    host: jsonplaceholder.typicode.com
    port: 443
    https: true
    connectTimeout: 1000
    connectionRequestTimeout: 1000

apis:
  getPosts:
    method: GET
    path: /posts/${id}
    server: jsonplaceholder
    timeout: 10000
    concurrency: 3
  getPostsAsync:
    method: GET
    path: /posts/${id}
    server: jsonplaceholder
    timeout: 1000
    concurrency: 3
    async: true
```

##### Details of parameters

1. timeout - timeout for the API. Your EasyHttp.call**() Api will timeout after the given time
2. concurrency - how many parallel calls can be made to this API.
3. rps - if you know "rps" of API call, then you should set `rps` e.g. rps: 100. The EasyHttp will automatically setup
   required threads to support concurrent calls. You don't need to set `concurrency` manually. For example, if
   timeout=20 and rps=100 then EasyHttp will set `concurrency=2`

When you set `rps` then you have to consider `rps` from the single node i.e. how many requests this single node is going
to call. For example, if you call an external API with 1000 `rps`; and you run 10 nodes, then a single node has rps=100

---


MySQL Helper
===
database-mysql module provides support for easy MySQL helper.

```shell script
See "io.github.devlibx.easy.database.mysql.ExampleApp" example
``` 

```xml
<!-- POM Dependency -->
<dependency>
    <groupId>io.github.devlibx.easy</groupId>
    <artifactId>database-mysql</artifactId>
    <version>${easy.version}</version>
</dependency>
```

You must setup IMysqlHelper before using it. A sample setup is als given below.

```shell script
// Insert to DB
IMysqlHelper mysqlHelper = injector.getInstance(IMysqlHelper.class);
Long id = mysqlHelper.persist(
        "",
        "INSERT INTO my_table(col) VALUES(?)",
        preparedStatement -> {
            preparedStatement.setString(1, "some value");
        }
);

// Find a row
String result = mysqlHelper.findOne(
        "",
        "SELECT col from my_table",
        statement -> {
        },
        rs -> rs.getString(1),
        String.class
).orElse("");
```

Setup to use this MySQL helper:

```shell script
Create test database for tests:
create database users;
create database test_me;

// Setup DB - datasource
DbConfig dbConfig = new DbConfig();
dbConfig.setDriverClassName("com.mysql.jdbc.Driver");
dbConfig.setJdbcUrl("YOUR JDBC URL");
dbConfig.setUsername("username");
dbConfig.setPassword("password");
MySqlConfigs mySqlConfigs = new MySqlConfigs();
mySqlConfigs.addConfig(dbConfig);

// Setup module
injector = Guice.createInjector(new AbstractModule() {
    @Override
    protected void configure() {
        bind(IMetrics.class).to(IMetrics.NoOpMetrics.class);
        bind(MySqlConfigs.class).toInstance(mySqlConfigs);
    }
}, new DatabaseMySQLModule());
ApplicationContext.setInjector(injector);

// Start DB
IDatabaseService databaseService = injector.getInstance(IDatabaseService.class);
databaseService.startDatabase();
```

---

Distributed Lock
===
This module provides a distributed lock e.g. a MySQL based distributed lock is implemented by easy libs. This example
shows a class `ResourceWithLocking` with a method which should take a lock before it is called.

Note - you will see MySQL and database dependency in the example code.

```java
package com.devlibx.pack.resources.lock;

import io.github.devlibx.easy.lock.DistributedLock;
import io.github.devlibx.easy.lock.IDistributedLock;
import io.github.devlibx.easy.lock.IDistributedLockIdResolver;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class ResourceWithLocking {
    private static final AtomicLong COUNTER = new AtomicLong();

    // When this method is called - it will first take a distributed lock
    //
    // MySQL based lock:
    // ================
    // For example if we use MySQL lock provider then we take a lock using the "lockId" in MySQL.
    // 
    // A implementation of IDistributedLockIdResolver class (InternalDistributedLockIdResolver in this case)
    // will be called to get the value of "lockId".
    // "createLockRequest()" method os called with the method arguments. You can extract the ID to lock against.
    //
    @DistributedLock(lockIdResolver = InternalDistributedLockIdResolver.class)
    public Map<String, Object> methodWhichShouldBeLocked(String someId) {
        try {
            Thread.sleep(1);
        } catch (InterruptedException ignored) {
        }
        log.trace("Called method methodWhichShouldBeLocked - id={}", someId);

        Map<String, Object> result = new HashMap<>();
        result.put("counter", COUNTER.incrementAndGet());
        result.put("id", someId);
        return result;
    }

    @Singleton
    public static class InternalDistributedLockIdResolver implements IDistributedLockIdResolver {
        @Override
        public IDistributedLock.LockRequest createLockRequest(MethodInvocation invocation, Object[] arguments) {
            return IDistributedLock.LockRequest.builder()
                    .lockId(arguments[0].toString())
                    .build();
        }
    }
}

public class Application {
    public static void main(String[] args) {

        // Setup DB - datasource
        DbConfig dbConfig = new DbConfig();
        dbConfig.setDriverClassName("com.mysql.jdbc.Driver");
        dbConfig.setJdbcUrl("YOUR JDBC URL");
        dbConfig.setUsername("username");
        dbConfig.setPassword("password");
        MySqlConfigs mySqlConfigs = new MySqlConfigs();
        mySqlConfigs.addConfig(dbConfig);

        // Setup module
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IMetrics.class).to(IMetrics.NoOpMetrics.class);
                bind(MySqlConfigs.class).toInstance(mySqlConfigs);
            }
        }, new DatabaseMySQLModule());
        ApplicationContext.setInjector(injector);

        // Start DB
        IDatabaseService databaseService = injector.getInstance(IDatabaseService.class);
        databaseService.startDatabase();

        // Setup lock service
        IDistributedLockService distributedLockService = injector.getInstance(IDistributedLockService.class);
        distributedLockService.initialize();

        // Example ResourceWithLocking
        ResourceWithLocking resourceWithLocking = injector.getInstance(ResourceWithLocking.class);

        // This API call will lock before running
        // For example if you run this method concurrently in many threads, then all execution 
        // will be sequential (for same lock id) 
        Map<String, Object> response = resourceWithLocking.methodWhichShouldBeLocked("1234");
        System.out.println(response);
    }
}
```

Helper Module
===


Convert Java object to JSON string

```xml
<!-- POM Dependency -->
<dependency>
    <groupId>io.github.devlibx.easy</groupId>
    <artifactId>helper</artifactId>
    <version>${easy.version}</version>
</dependency>
```

```shell script

// A pojo object to stringify
@Data
public class PojoClass {
    private String str;
    private int anInt;
}

    
PojoClass testClass = new PojoClass();
testClass.setStr("some string");
testClass.setAnInt(11);

StringHelper stringHelper = new StringHelper();
stringHelper.stringify(testClass); 

// Output - {"str":"some string","an_int":11} 
```

### Testing Module

Testing module is create to help testing with MySQL, DynamoDB, Kafaka

Following setup is needed to ues testing module

```shell
# Run this command if you are using DynamoDB testing
====================================================
docker pull testcontainers/ryuk:0.3.0

# Only If you are buulding "easy" sourcecode
==========================================
Create following databases in MySQL
create database users;
create database test_me;



```