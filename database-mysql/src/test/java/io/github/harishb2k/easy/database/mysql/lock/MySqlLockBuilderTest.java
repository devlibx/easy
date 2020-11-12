package io.github.harishb2k.easy.database.mysql.lock;

import ch.qos.logback.classic.Level;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Names;
import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.gitbub.harishb2k.easy.helper.CommonBaseTestCase;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.github.harishb2k.easy.database.IDatabaseService;
import io.github.harishb2k.easy.database.mysql.IMysqlHelper;
import io.github.harishb2k.easy.database.mysql.config.MySqlConfig;
import io.github.harishb2k.easy.database.mysql.config.MySqlConfigs;
import io.github.harishb2k.easy.database.mysql.module.DatabaseMySQLModule;
import io.github.harishb2k.easy.database.mysql.transaction.TransactionInterceptor;
import io.github.harishb2k.easy.lock.DistributedLock;
import io.github.harishb2k.easy.lock.IDistributedLock;
import io.github.harishb2k.easy.lock.IDistributedLockIdResolver;
import io.github.harishb2k.easy.lock.IDistributedLockService;
import io.github.harishb2k.easy.lock.config.LockConfig;
import io.github.harishb2k.easy.lock.config.LockConfigs;
import io.github.harishb2k.easy.lock.interceptor.DistributedLockInterceptor;
import io.github.harishb2k.easy.testing.mysql.MySqlExtension;
import io.github.harishb2k.easy.testing.mysql.TestingMySqlConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class MySqlLockBuilderTest extends CommonBaseTestCase {
    @RegisterExtension
    public static MySqlExtension defaultMysql = MySqlExtension.builder()
            .withDatabase("users")
            .withHost("localhost")
            .withUsernamePassword("test", "test")
            .build();

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(DistributedLockInterceptor.class).setLevel(Level.TRACE);
        LoggingHelper.getLogger(MySqlDistributedLock.class).setLevel(Level.TRACE);
        LoggingHelper.getLogger(MySqlDistributedLockV2.class).setLevel(Level.TRACE);
        LoggingHelper.getLogger(TransactionInterceptor.class).setLevel(Level.TRACE);
    }


    private void setupLockTable(IMysqlHelper mysqlHelper) {
        boolean executeResult = mysqlHelper.execute(
                "",
                "CREATE TABLE IF NOT EXISTS locks (ID int NOT NULL PRIMARY KEY AUTO_INCREMENT, lock_id varchar(255)); ",
                preparedStatement -> {
                }
        );
    }

    @Test
    @DisplayName("Test MySQL lock - without annotation")
    public void testMySqlLock(TestingMySqlConfig mySqlConfig) {
        Assumptions.assumeTrue(mySqlConfig.isRunning(), "MySQL must be running for this test - skip MySQL is not running");

        // Unique name for this test
        final String uniqueName = UUID.randomUUID().toString();

        Injector injector = Guice.createInjector(new DatabaseMySQLModule(false, 10), new AbstractModule() {
            @Override
            protected void configure() {

                // Create a lock
                LockConfigs lockConfigs = new LockConfigs();
                LockConfig lockConfig = new LockConfig();
                lockConfig.setTimeoutInSec(2000);
                lockConfig.setType("MYSQL");
                lockConfig.setName("test-" + uniqueName);
                lockConfigs.addLockConfig(lockConfig);
                bind(LockConfigs.class).toInstance(lockConfigs);

                // Setup DB - datasource
                MySqlConfig dbConfig = new MySqlConfig();
                dbConfig.setDriverClassName("com.mysql.jdbc.Driver");
                dbConfig.setJdbcUrl(mySqlConfig.getJdbcUrl());
                dbConfig.setUsername(mySqlConfig.getUsername());
                dbConfig.setPassword(mySqlConfig.getPassword());
                dbConfig.setMaxPoolSize(2);
                dbConfig.setShowSql(false);
                MySqlConfigs mySqlConfigs = new MySqlConfigs();
                mySqlConfigs.addConfig(dbConfig);
                bind(MySqlConfigs.class).toInstance(mySqlConfigs);

                // Set data source to lock
                OptionalBinder.newOptionalBinder(binder(), Key.get(DataSource.class, Names.named("lock_table_data_source")))
                        .setBinding()
                        .toInstance(dbConfig.buildHikariDataSource());

                // Set default lock table name
                OptionalBinder.newOptionalBinder(binder(), Key.get(String.class, Names.named("lock_table_name")))
                        .setBinding()
                        .toInstance("locks");
            }
        });
        ApplicationContext.setInjector(injector);

        // Setup lock service
        IDistributedLockService distributedLockService = injector.getInstance(IDistributedLockService.class);
        distributedLockService.initialize();

        // Setup required table
        IDatabaseService databaseService = injector.getInstance(IDatabaseService.class);
        databaseService.startDatabase();
        setupLockTable(injector.getInstance(IMysqlHelper.class));

        // Get the lock
        IDistributedLock lock = distributedLockService.getLock("test-" + uniqueName);

        // Acquire lock for given id
        String lockId = UUID.randomUUID().toString();
        Lock lockObj = lock.achieveLock(IDistributedLock.LockRequest.builder().lockId(lockId).build());
        try {

        } finally {
            lock.releaseLock(lockObj, null);
        }
    }

    @Test
    @DisplayName("Test MySQL lock works with method DistributedLock")
    public void testMySqlLockAnnotation(TestingMySqlConfig mySqlConfig) {
        Assumptions.assumeTrue(mySqlConfig.isRunning(), "MySQL must be running for this test - skip MySQL is not running");

        Injector injector = Guice.createInjector(new DatabaseMySQLModule(false, 10), new AbstractModule() {
            @Override
            protected void configure() {

                // Create a lock
                LockConfigs lockConfigs = new LockConfigs();
                LockConfig lockConfig = new LockConfig();
                lockConfig.setTimeoutInSec(2000);
                lockConfig.setType("MYSQL");
                lockConfig.setName("dummy-test");
                lockConfigs.addLockConfig(lockConfig);
                bind(LockConfigs.class).toInstance(lockConfigs);

                // Setup DB - datasource
                MySqlConfig dbConfig = new MySqlConfig();
                dbConfig.setDriverClassName("com.mysql.jdbc.Driver");
                dbConfig.setJdbcUrl(mySqlConfig.getJdbcUrl());
                dbConfig.setUsername(mySqlConfig.getUsername());
                dbConfig.setPassword(mySqlConfig.getPassword());
                dbConfig.setShowSql(false);
                dbConfig.setMaxPoolSize(2);
                MySqlConfigs mySqlConfigs = new MySqlConfigs();
                mySqlConfigs.addConfig(dbConfig);
                bind(MySqlConfigs.class).toInstance(mySqlConfigs);

                // Set data source to lock
                OptionalBinder.newOptionalBinder(binder(), Key.get(DataSource.class, Names.named("lock_table_data_source")))
                        .setBinding()
                        .toInstance(dbConfig.buildHikariDataSource());

                // Set default lock table name
                OptionalBinder.newOptionalBinder(binder(), Key.get(String.class, Names.named("lock_table_name")))
                        .setBinding()
                        .toInstance("locks");
            }
        });
        ApplicationContext.setInjector(injector);

        IDatabaseService databaseService = injector.getInstance(IDatabaseService.class);
        databaseService.startDatabase();

        // Setup required table
        setupLockTable(injector.getInstance(IMysqlHelper.class));

        // Setup lock service
        IDistributedLockService distributedLockService = injector.getInstance(IDistributedLockService.class);
        distributedLockService.initialize();

        String id = UUID.randomUUID().toString();
        MySQLAnnotationTest testClass = injector.getInstance(MySQLAnnotationTest.class);
        testClass.lockMe(id, 10);

        MyIDistributedLockIdResolver r = new MyIDistributedLockIdResolver();
        IMysqlHelper mysqlHelper = injector.getInstance(IMysqlHelper.class);
        String lockId = mysqlHelper.findOne(
                "",
                "SELECT lock_id from locks WHERE lock_id=?",
                statement -> {
                    statement.setString(1, r.createLockRequest(null, new Object[]{id, 10}).getUniqueLockIdForLocking());
                },
                rs -> rs.getString(1),
                String.class
        ).orElse(null);
        assertEquals(lockId, r.createLockRequest(null, new Object[]{id, 10}).getUniqueLockIdForLocking());
    }

    @RepeatedTest(20)
    @DisplayName("Run MySQL lock with concurrent thread to make sure lock is working properly")
    public void testMySqlLockAnnotation_WithThread(TestingMySqlConfig mySqlConfig) throws InterruptedException {
        Assumptions.assumeTrue(mySqlConfig.isRunning(), "MySQL must be running for this test - skip MySQL is not running");

        Injector injector = Guice.createInjector(new DatabaseMySQLModule(false, 10), new AbstractModule() {
            @Override
            protected void configure() {

                // Create a lock
                LockConfigs lockConfigs = new LockConfigs();
                LockConfig lockConfig = new LockConfig();
                lockConfig.setTimeoutInSec(2);
                lockConfig.setType("MYSQL");
                lockConfig.setName("dummy-test");
                lockConfigs.addLockConfig(lockConfig);

                LockConfig lockInsideLock_Parent = new LockConfig();
                lockInsideLock_Parent.setTimeoutInSec(10);
                lockInsideLock_Parent.setType("MYSQL");
                lockInsideLock_Parent.setName("lockInsideLock_Parent");
                lockConfigs.addLockConfig(lockInsideLock_Parent);

                LockConfig lockInsideLock_Child = new LockConfig();
                lockInsideLock_Child.setTimeoutInSec(10);
                lockInsideLock_Child.setType("MYSQL");
                lockInsideLock_Child.setName("lockInsideLock_Child");
                lockConfigs.addLockConfig(lockInsideLock_Child);

                bind(LockConfigs.class).toInstance(lockConfigs);

                // Setup DB - datasource
                MySqlConfig dbConfig = new MySqlConfig();
                dbConfig.setDriverClassName("com.mysql.jdbc.Driver");
                dbConfig.setJdbcUrl(mySqlConfig.getJdbcUrl());
                dbConfig.setUsername(mySqlConfig.getUsername());
                dbConfig.setPassword(mySqlConfig.getPassword());
                dbConfig.setShowSql(false);
                dbConfig.setMaxPoolSize(2);
                MySqlConfigs mySqlConfigs = new MySqlConfigs();
                mySqlConfigs.addConfig(dbConfig);
                bind(MySqlConfigs.class).toInstance(mySqlConfigs);

                // Set data source to lock
                OptionalBinder.newOptionalBinder(binder(), Key.get(DataSource.class, Names.named("lock_table_data_source")))
                        .setBinding()
                        .toInstance(dbConfig.buildHikariDataSource());

                // Set default lock table name
                OptionalBinder.newOptionalBinder(binder(), Key.get(String.class, Names.named("lock_table_name")))
                        .setBinding()
                        .toInstance("locks");
            }
        });
        ApplicationContext.setInjector(injector);

        IDatabaseService databaseService = injector.getInstance(IDatabaseService.class);
        databaseService.startDatabase();

        // Setup required table
        setupLockTable(injector.getInstance(IMysqlHelper.class));

        // Setup lock service
        IDistributedLockService distributedLockService = injector.getInstance(IDistributedLockService.class);
        distributedLockService.initialize();

        String id = UUID.randomUUID().toString();
        CountDownLatch latch = new CountDownLatch(2);
        CountDownLatch waitForThread1 = new CountDownLatch(1);

        // Thread 1
        AtomicLong lockReleasedAt = new AtomicLong();
        new Thread(() -> {
            try {
                MySQLAnnotationTest testClass = injector.getInstance(MySQLAnnotationTest.class);
                testClass.lockMeWithCallback(id, 10,
                        waitForThread1::countDown,
                        () -> {
                            for (int i = 0; i < 10; i++) {
                                // System.out.println("Doing work in lock - " + i);
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException ignored) {
                                }
                            }
                            lockReleasedAt.set(System.currentTimeMillis());
                        });
            } finally {
                latch.countDown();
            }
        }).start();

        // Thread 2
        AtomicLong lockGetByNextWorker = new AtomicLong();
        new Thread(() -> {
            try {
                waitForThread1.await(5, TimeUnit.SECONDS);
                Thread.sleep(200);
                MySQLAnnotationTest testClass = injector.getInstance(MySQLAnnotationTest.class);
                testClass.lockMeWithCallback(
                        id, 10, null,
                        () -> {
                            lockGetByNextWorker.set(System.currentTimeMillis());
                            System.out.println("Doing work in lock in other thread");
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException ignored) {
                            }
                        });
            } catch (Exception ignored) {
            } finally {
                latch.countDown();
            }
        }).start();
        latch.await(1000, TimeUnit.SECONDS);


        MyIDistributedLockIdResolver r = new MyIDistributedLockIdResolver();
        IMysqlHelper mysqlHelper = injector.getInstance(IMysqlHelper.class);
        String lockId = mysqlHelper.findOne(
                "",
                "SELECT lock_id from locks WHERE lock_id=?",
                statement -> {
                    statement.setString(1, r.createLockRequest(null, new Object[]{id, 10}).getUniqueLockIdForLocking());
                },
                rs -> rs.getString(1),
                String.class
        ).orElse(null);
        assertEquals(lockId, r.createLockRequest(null, new Object[]{id, 10}).getUniqueLockIdForLocking());
        assertTrue(lockGetByNextWorker.get() > lockReleasedAt.get());


        // Test 2 - Lock inside lock
        String lockInsideLock_Parent_Lock_Id = UUID.randomUUID().toString();
        MySQLAnnotationTest testClass = injector.getInstance(MySQLAnnotationTest.class);
        String result = testClass.lockInsideLock_Parent(lockInsideLock_Parent_Lock_Id);
        assertEquals("lockInsideLock_Child_" + lockInsideLock_Parent_Lock_Id, result);

        databaseService.stopDatabase();
        distributedLockService.shutdown();
    }

    public static class MySQLAnnotationTest {
        private final LockInsideLockChildClass lockInsideLockChildClass;

        @Inject
        public MySQLAnnotationTest(LockInsideLockChildClass lockInsideLockChildClass) {
            this.lockInsideLockChildClass = lockInsideLockChildClass;
        }

        @DistributedLock(name = "dummy-test", lockIdResolver = MyIDistributedLockIdResolver.class)
        public void lockMe(String param1, int param2) {

        }

        @DistributedLock(name = "dummy-test", lockIdResolver = MyIDistributedLockIdResolver.class)
        @Transactional
        public void lockMeWithCallback(String param1, int param2, Runnable gotLock, Runnable work) {
            if (gotLock != null) gotLock.run();
            work.run();
        }

        @DistributedLock(name = "lockInsideLock_Parent", lockIdResolver = LockInsideLockDistributedLockIdResolver.class)
        public String lockInsideLock_Parent(String param) {
            log.info("Inside lockInsideLock_Parent");
            return lockInsideLockChildClass.lockInsideLock_Child(param);
        }
    }

    public static class LockInsideLockChildClass {
        @SneakyThrows
        @DistributedLock(name = "lockInsideLock_Child", lockIdResolver = LockInsideLockDistributedLockIdResolver.class)
        public String lockInsideLock_Child(String param) {
            log.info("Inside lockInsideLock_Child");
            Thread.sleep(100);
            return "lockInsideLock_Child_" + param;
        }
    }

    private static class MyIDistributedLockIdResolver implements IDistributedLockIdResolver {
        @Override
        public IDistributedLock.LockRequest createLockRequest(MethodInvocation invocation, Object[] arguments) {
            return IDistributedLock.LockRequest.builder()
                    .name("default")
                    .lockId(arguments[0].toString() + "--" + arguments[1].toString())
                    .build();
        }
    }

    private static class LockInsideLockDistributedLockIdResolver implements IDistributedLockIdResolver {
        @Override
        public IDistributedLock.LockRequest createLockRequest(MethodInvocation invocation, Object[] arguments) {
            return IDistributedLock.LockRequest.builder()
                    .name("default")
                    .lockId(arguments[0].toString())
                    .build();
        }
    }
}