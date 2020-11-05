package io.github.harishb2k.easy.database.mysql.lock;

import ch.qos.logback.classic.Level;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Names;
import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.gitbub.harishb2k.easy.helper.CommonBaseTestCase;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.gitbub.harishb2k.easy.helper.mysql.IMySqlTestHelper;
import io.gitbub.harishb2k.easy.helper.mysql.MySqlTestHelper;
import io.github.harishb2k.easy.database.IDatabaseService;
import io.github.harishb2k.easy.database.mysql.IMysqlHelper;
import io.github.harishb2k.easy.database.mysql.MySQLHelperPlugin;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

@Slf4j
public class MySqlLockBuilderTest extends CommonBaseTestCase {
    private static final AtomicReference<MySqlTestHelper> mySQLHelperAtomicReference = new AtomicReference<>();
    private static final AtomicBoolean isIsMySqlRunningCheckDone = new AtomicBoolean(false);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(DistributedLockInterceptor.class).setLevel(Level.TRACE);
        LoggingHelper.getLogger(MySqlDistributedLock.class).setLevel(Level.TRACE);
        LoggingHelper.getLogger(TransactionInterceptor.class).setLevel(Level.TRACE);
        tryToSetupMySQLToRunTests();
    }

    /**
     * Helper to make sure we have MySQL running
     */
    private void tryToSetupMySQLToRunTests() {
        synchronized (isIsMySqlRunningCheckDone) {
            if (isIsMySqlRunningCheckDone.get()) return;
            try {
                MySqlTestHelper primaryMySqlTestHelper = new MySqlTestHelper();
                primaryMySqlTestHelper.installCustomMySqlTestHelper(new MySQLHelperPlugin());
                IMySqlTestHelper.TestMySqlConfig primaryMySqlConfig = IMySqlTestHelper.TestMySqlConfig.withDefaults();
                primaryMySqlTestHelper.startMySql(primaryMySqlConfig);
                mySQLHelperAtomicReference.set(primaryMySqlTestHelper);
            } finally {
                isIsMySqlRunningCheckDone.set(true);
            }
        }
    }

    public void testMySqlLock() {

        // Do not run test if MySQL is not running
        if (mySQLHelperAtomicReference.get() == null || !mySQLHelperAtomicReference.get().isMySqlRunning()) {
            log.error("Did not run testMySqlLock - MySQL is not running");
            return;
        }


        // Unique name for this test
        final String uniqueName = UUID.randomUUID().toString();

        Injector injector = Guice.createInjector(new DatabaseMySQLModule(false, 10), new AbstractModule() {
            @Override
            protected void configure() {

                // Create a lock
                LockConfigs lockConfigs = new LockConfigs();
                LockConfig lockConfig = new LockConfig();
                lockConfig.setTimeoutInMs(2000);
                lockConfig.setType("MYSQL");
                lockConfig.setName("test-" + uniqueName);
                lockConfigs.addLockConfig(lockConfig);
                bind(LockConfigs.class).toInstance(lockConfigs);

                // Setup DB - datasource
                MySqlConfig dbConfig = new MySqlConfig();
                dbConfig.setDriverClassName("com.mysql.jdbc.Driver");
                dbConfig.setJdbcUrl(mySQLHelperAtomicReference.get().getMySqlConfig().getJdbcUrl());
                dbConfig.setUsername(mySQLHelperAtomicReference.get().getMySqlConfig().getUser());
                dbConfig.setPassword(mySQLHelperAtomicReference.get().getMySqlConfig().getPassword());
                dbConfig.setMaxPoolSize(2);
                dbConfig.setShowSql(false);

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

    public void testMySqlLockAnnotation() {
        // Do not run test if MySQL is not running
        if (mySQLHelperAtomicReference.get() == null || !mySQLHelperAtomicReference.get().isMySqlRunning()) {
            log.error("Did not run testMySqlLock - MySQL is not running");
            return;
        }

        Injector injector = Guice.createInjector(new DatabaseMySQLModule(false, 10), new AbstractModule() {
            @Override
            protected void configure() {

                // Create a lock
                LockConfigs lockConfigs = new LockConfigs();
                LockConfig lockConfig = new LockConfig();
                lockConfig.setTimeoutInMs(2000);
                lockConfig.setType("MYSQL");
                lockConfig.setName("dummy-test");
                lockConfigs.addLockConfig(lockConfig);
                bind(LockConfigs.class).toInstance(lockConfigs);

                // Setup DB - datasource
                MySqlConfig dbConfig = new MySqlConfig();
                dbConfig.setDriverClassName("com.mysql.jdbc.Driver");
                dbConfig.setJdbcUrl(mySQLHelperAtomicReference.get().getMySqlConfig().getJdbcUrl());
                dbConfig.setUsername(mySQLHelperAtomicReference.get().getMySqlConfig().getUser());
                dbConfig.setPassword(mySQLHelperAtomicReference.get().getMySqlConfig().getPassword());
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
                    statement.setString(1, r.createLockRequest(new Object[]{id, 10}).getUniqueLockIdForLocking());
                },
                rs -> rs.getString(1),
                String.class
        ).orElse(null);
        assertEquals(lockId, r.createLockRequest(new Object[]{id, 10}).getUniqueLockIdForLocking());
    }

    public void testMySqlLockAnnotation_WithThread_10_times() throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            testMySqlLockAnnotation_WithThread();
        }
    }

    public void testMySqlLockAnnotation_WithThread() throws InterruptedException {
        // Do not run test if MySQL is not running
        if (mySQLHelperAtomicReference.get() == null || !mySQLHelperAtomicReference.get().isMySqlRunning()) {
            log.error("Did not run testMySqlLock - MySQL is not running");
            return;
        }

        Injector injector = Guice.createInjector(new DatabaseMySQLModule(false, 10), new AbstractModule() {
            @Override
            protected void configure() {

                // Create a lock
                LockConfigs lockConfigs = new LockConfigs();
                LockConfig lockConfig = new LockConfig();
                lockConfig.setTimeoutInMs(10000);
                lockConfig.setType("MYSQL");
                lockConfig.setName("dummy-test");
                lockConfigs.addLockConfig(lockConfig);
                bind(LockConfigs.class).toInstance(lockConfigs);

                // Setup DB - datasource
                MySqlConfig dbConfig = new MySqlConfig();
                dbConfig.setDriverClassName("com.mysql.jdbc.Driver");
                dbConfig.setJdbcUrl(mySQLHelperAtomicReference.get().getMySqlConfig().getJdbcUrl());
                dbConfig.setUsername(mySQLHelperAtomicReference.get().getMySqlConfig().getUser());
                dbConfig.setPassword(mySQLHelperAtomicReference.get().getMySqlConfig().getPassword());
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
        latch.await(10, TimeUnit.SECONDS);


        MyIDistributedLockIdResolver r = new MyIDistributedLockIdResolver();
        IMysqlHelper mysqlHelper = injector.getInstance(IMysqlHelper.class);
        String lockId = mysqlHelper.findOne(
                "",
                "SELECT lock_id from locks WHERE lock_id=?",
                statement -> {
                    statement.setString(1, r.createLockRequest(new Object[]{id, 10}).getUniqueLockIdForLocking());
                },
                rs -> rs.getString(1),
                String.class
        ).orElse(null);
        assertEquals(lockId, r.createLockRequest(new Object[]{id, 10}).getUniqueLockIdForLocking());
        assertTrue(lockGetByNextWorker.get() > lockReleasedAt.get());

        databaseService.stopDatabase();
        distributedLockService.shutdown();
    }


    public static class MySQLAnnotationTest {
        @DistributedLock(name = "dummy-test", lockIdResolver = MyIDistributedLockIdResolver.class)
        public void lockMe(String param1, int param2) {

        }

        @DistributedLock(name = "dummy-test", lockIdResolver = MyIDistributedLockIdResolver.class)
        @Transactional
        public void lockMeWithCallback(String param1, int param2, Runnable gotLock, Runnable work) {
            if (gotLock != null) gotLock.run();
            work.run();
        }
    }

    private static class MyIDistributedLockIdResolver implements IDistributedLockIdResolver {

        @Override
        public IDistributedLock.LockRequest createLockRequest(Object[] arguments) {
            return IDistributedLock.LockRequest.builder()
                    .name("default")
                    .lockId(arguments[0].toString() + "--" + arguments[1].toString())
                    .build();
        }
    }
}