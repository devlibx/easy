Helper Module
===

Convert Java object to JSON string
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
// Setup module
injector = Guice.createInjector(new AbstractModule() {
    @Override
    protected void configure() {
        bind(IMetrics.class).to(IMetrics.NoOpMetrics.class);
    }
}, new DatabaseMySQLModule());
ApplicationContext.setInjector(injector);

// Setup DB - datasource
DbConfig dbConfig = new DbConfig();
dbConfig.setDriverClassName("com.mysql.jdbc.Driver");
dbConfig.setJdbcUrl("YOUR JDBC URL");
dbConfig.setUsername("username");
dbConfig.setPassword("password");

// Add default datasource to factory 
injector.getInstance(DataSourceFactory.class).register(dbConfig.buildHikariDataSource());

// Start DB
IDatabaseService databaseService = injector.getInstance(IDatabaseService.class);
databaseService.startDatabase();
```