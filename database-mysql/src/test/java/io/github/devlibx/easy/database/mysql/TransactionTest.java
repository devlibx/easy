package io.github.devlibx.easy.database.mysql;

import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.Safe;
import io.github.devlibx.easy.lock.DistributedLock;
import io.github.devlibx.easy.lock.IDistributedLockService.NoOpDistributedLockIdResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.devlibx.easy.lock.IDistributedLockService.NO_OP_LOCK_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public abstract class TransactionTest {
    private static String uniqueString = UUID.randomUUID().toString();

    // This test is executed from MySqlEndToEndTestCase
    public void testTransactionBulk() throws Exception {
        ITransactionTestClass transactionTestClass = ApplicationContext.getInstance(ITransactionTestClass.class);
        for (int i = 0; i < 10; i++) {
            log.info("Running testTransactionBulk = {}", i);
            CountDownLatch latch = new CountDownLatch(20);
            for (int j = 0; j < 20; j++) {
                new Thread(() -> {
                    try {
                        transactionTestClass.persistWithoutTransaction(100);
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }
            latch.await(10, TimeUnit.SECONDS);
        }
    }

    // This test is executed from MySqlEndToEndTestCase
    public void testTransaction() {
        ITransactionTestClass transactionTestClass = ApplicationContext.getInstance(ITransactionTestClass.class);
        try {
            transactionTestClass.persistRecordFirst();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Expected error - " + e.getMessage());
        }

        IMysqlHelper mysqlHelper = ApplicationContext.getInstance(IMysqlHelper.class);
        List<String> results = mysqlHelper.findAll(
                "na",
                "SELECT name from users where name like ?",
                statement -> {
                    statement.setString(1, "%" + uniqueString);
                },
                rs -> rs.getString(1),
                String.class
        ).orElse(new ArrayList<>());
        assertEquals(2, results.size());
        assertEquals("persistRecordSecond-" + uniqueString, results.get(0));
        assertEquals("persistRecordForth-" + uniqueString, results.get(1));
        assertNull(transactionTestClass.getAfterCommitCalled().get("persistRecordFirst"));
        assertNull(transactionTestClass.getAfterCommitCalled().get("persistRecordThird"));
        assertTrue(transactionTestClass.getAfterCommitCalled().get("persistRecordSecond").get());
        assertTrue(transactionTestClass.getAfterCommitCalled().get("persistRecordForth").get());

        results.forEach(s -> {
            log.info("Result after transaction = " + s);
        });

        transactionTestClass.persistWithoutTransaction(1);
        results = mysqlHelper.findAll(
                "na",
                "SELECT name from users where name like ?",
                statement -> {
                    statement.setString(1, "persistWithoutTransaction-" + uniqueString);
                },
                rs -> rs.getString(1),
                String.class
        ).orElse(new ArrayList<>());
        assertEquals(1, results.size());
        assertEquals("persistWithoutTransaction-" + uniqueString, results.get(0));
    }

    interface ITransactionTestClass {
        Long persistRecordFirst();

        Long persistRecordSecond();

        Long persistRecordThird();

        Long persistRecordForth();

        Long persistWithoutTransaction(int sleep);

        Map<String, AtomicBoolean> getAfterCommitCalled();
    }


    public static class TransactionTestClass implements ITransactionTestClass {
        private final IMysqlHelper mysqlHelper;
        private final Map<String, AtomicBoolean> getAfterCommitCalled;

        @Inject
        public TransactionTestClass(IMysqlHelper mysqlHelper) {
            this.mysqlHelper = mysqlHelper;
            this.getAfterCommitCalled = new HashMap<>();
        }

        @Override
        public Map<String, AtomicBoolean> getAfterCommitCalled() {
            return getAfterCommitCalled;
        }

        private void setupAfterCommitHook(String name) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    Safe.safe(() -> {
                        getAfterCommitCalled.put(name, new AtomicBoolean(true));
                    }, "got error after commit");
                }
            });
        }

        @Override
        @Transactional(propagation = Propagation.REQUIRED, label = {"name=persistRecordFirst"})
        @DistributedLock(name = NO_OP_LOCK_NAME, lockIdResolver = NoOpDistributedLockIdResolver.class)
        public Long persistRecordFirst() {
            setupAfterCommitHook("persistRecordFirst");

            mysqlHelper.persist(
                    "none",
                    "INSERT INTO users(name) VALUES(?)",
                    statement -> {
                        statement.setString(1, "persistRecordFirst-" + uniqueString);
                    }
            );

            ITransactionTestClass transactionTestClass = ApplicationContext.getInstance(ITransactionTestClass.class);
            transactionTestClass.persistRecordSecond();
            transactionTestClass.persistRecordThird();
            transactionTestClass.persistRecordForth();

            throw new RuntimeException("Expected error - Generate exception to fail this transaction to test REQUIRES_NEW works or no - Ignore this error");
        }

        @Override
        @Transactional(propagation = Propagation.REQUIRES_NEW, label = {"name=persistRecordSecond"})
        public Long persistRecordSecond() {
            setupAfterCommitHook("persistRecordSecond");

            return mysqlHelper.persist(
                    "none",
                    "INSERT INTO users(name) VALUES(?)",
                    statement -> {
                        statement.setString(1, "persistRecordSecond-" + uniqueString);
                    }
            );
        }

        @Override
        @Transactional(propagation = Propagation.REQUIRED, label = {"name=persistRecordThird"})
        public Long persistRecordThird() {
            setupAfterCommitHook("persistRecordThird");

            return mysqlHelper.persist(
                    "none",
                    "INSERT INTO users(name) VALUES(?)",
                    statement -> {
                        statement.setString(1, "persistRecordThird-" + uniqueString);
                    }
            );
        }

        @Override
        @Transactional(propagation = Propagation.REQUIRES_NEW, label = {"name=persistRecordForth"})
        public Long persistRecordForth() {
            setupAfterCommitHook("persistRecordForth");

            return mysqlHelper.persist(
                    "none",
                    "INSERT INTO users(name) VALUES(?)",
                    statement -> {
                        statement.setString(1, "persistRecordForth-" + uniqueString);
                    }
            );
        }

        @Override
        public Long persistWithoutTransaction(int sleep) {
            try {
                if (sleep > 0) {
                    Thread.sleep(sleep);
                }
            } catch (InterruptedException ignored) {
            }
            return mysqlHelper.persist(
                    "none",
                    "INSERT INTO users(name) VALUES(?)",
                    statement -> {
                        statement.setString(1, "persistWithoutTransaction-" + uniqueString);
                    }
            );
        }
    }
}
