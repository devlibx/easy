Helper Module
===

Convert Java object to JSON string
```xml
<!-- POM Dependency -->
<dependency>
  <groupId>io.github.harishb2k.easy</groupId>
  <artifactId>helper</artifactId>
  <version>0.0.6</version>
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

MySQL Helper
===
database-mysql module provides support for easy MySQL helper.
```shell script
See "io.github.harishb2k.easy.database.mysql.ExampleApp" example
``` 
```xml
<!-- POM Dependency -->
<dependency>
  <groupId>io.github.harishb2k.easy</groupId>
  <artifactId>database-mysql</artifactId>
  <version>0.0.6</version>
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

Http Module
===
[Http Module Wiki] (https://github.com/harishb2k/easy/wiki/Http-Module)

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
  getPostsAsync:
    method: GET
    path: /posts/${id}
    server: jsonplaceholder
    timeout: 1000
    async: true
```

Source File: io.github.harishb2k.easy.http.DemoApplication

```java
package io.github.harishb2k.easy.http;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.gitbub.harishb2k.easy.helper.json.JsonUtils;
import io.gitbub.harishb2k.easy.helper.yaml.YamlUtils;
import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.module.EasyHttpModule;
import io.github.harishb2k.easy.http.sync.SyncRequestProcessor;
import io.github.harishb2k.easy.http.util.Call;
import io.github.harishb2k.easy.http.util.EasyHttp;
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
    }
}

```
