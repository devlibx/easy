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
    method: POST
    path: /posts/${id}
    server: jsonplaceholder
    timeout: 1000
```

```java
public class Example {
    public static void main(String[] args) {
        Config config = YamlUtils.readYamlCamelCase("sync_processor_config.yaml", Config.class);
        
        Map<String, Object> qp = new HashMap<>();
        qp.put("id", 1);
        try {
            Map resultSync = EasyHttp.callSync(
                    "jsonplaceholder",  // Name of the service
                    "getPosts",         // Name of the API
                    qp,                 // Path Params
                    null,               // Query Params
                    null,               // Header
                    null,               // Request Body
                    Map.class           // Response Class
            );
            System.out.println(resultSync);
        } catch (EasyHttpRequestException e) {
            System.out.println(e.getResponseAsString());
            System.out.println(e);
        }
    }
}
```