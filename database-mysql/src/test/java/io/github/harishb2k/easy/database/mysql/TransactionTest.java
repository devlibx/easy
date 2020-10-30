package io.github.harishb2k.easy.database.mysql;

import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public abstract class TransactionTest extends TestCase {
    private static String uniqueString = UUID.randomUUID().toString();

    public void testTransaction() {
        ITransactionTestClass transactionTestClass = ApplicationContext.getInstance(ITransactionTestClass.class);
        try {
            transactionTestClass.persistRecordFirst();
        } catch (Exception e) {
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

        results.forEach(s -> {
            log.info("Result after transaction = " + s);
        });

        transactionTestClass.persistWithoutTransaction();
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

        Long persistWithoutTransaction();
    }


    public static class TransactionTestClass implements ITransactionTestClass {
        private final IMysqlHelper mysqlHelper;

        @Inject
        public TransactionTestClass(IMysqlHelper mysqlHelper) {
            this.mysqlHelper = mysqlHelper;
        }

        @Override
        @Transactional(value = "persistRecordFirst", propagation = Propagation.REQUIRED)
        public Long persistRecordFirst() {
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

            throw new RuntimeException("Expected error - Generate exception to fail this transaction");
        }

        @Override
        @Transactional(value = "persistRecordSecond", propagation = Propagation.REQUIRES_NEW)
        public Long persistRecordSecond() {
            return mysqlHelper.persist(
                    "none",
                    "INSERT INTO users(name) VALUES(?)",
                    statement -> {
                        statement.setString(1, "persistRecordSecond-" + uniqueString);
                    }
            );
        }

        @Override
        @Transactional(value = "persistRecordThird", propagation = Propagation.REQUIRED, label = {})
        public Long persistRecordThird() {
            return mysqlHelper.persist(
                    "none",
                    "INSERT INTO users(name) VALUES(?)",
                    statement -> {
                        statement.setString(1, "persistRecordThird-" + uniqueString);
                    }
            );
        }

        @Override
        @Transactional(value = "persistRecordForth", propagation = Propagation.REQUIRES_NEW)
        public Long persistRecordForth() {
            return mysqlHelper.persist(
                    "none",
                    "INSERT INTO users(name) VALUES(?)",
                    statement -> {
                        statement.setString(1, "persistRecordForth-" + uniqueString);
                    }
            );
        }

        @Override
        public Long persistWithoutTransaction() {
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
