package io.github.harishb2k.easy.database.mysql;

import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("ConstantConditions")
@Slf4j
public abstract class SimpleMysqlHelperTest {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runTest() {
        IMysqlHelper mysqlHelper = ApplicationContext.getInstance(IMysqlHelper.class);

        // Step 1 - Create a DB
        boolean executeResult = mysqlHelper.execute(
                "",
                "CREATE TABLE IF NOT EXISTS users (ID int NOT NULL PRIMARY KEY AUTO_INCREMENT, name varchar(255)); ",
                preparedStatement -> {
                }
        );
        assertFalse(executeResult);


        // Step 2 - Insert to DB
        Long id = mysqlHelper.persist(
                "",
                "INSERT INTO users(name) VALUES(?)",
                preparedStatement -> {
                    preparedStatement.setString(1, "HI");
                }
        );
        assertNotNull(id);
        assertTrue(id > 0);

        // Try insert in threads
        int count = 0;
        CountDownLatch countDownLatch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            new Thread(() -> {
                Long _id = mysqlHelper.persist(
                        "",
                        "INSERT INTO users(name) VALUES(?)",
                        preparedStatement -> {
                            preparedStatement.setString(1, "HI");
                        }
                );
                countDownLatch.countDown();
                assertNotNull(_id);
                assertTrue(_id > 0);
            }).start();
        }
        try {
            if (count > 0) {
                countDownLatch.await();
            }
        } catch (InterruptedException ignored) {
        }

        // Step 2 - Read from DB
        String result = mysqlHelper.findOne(
                "",
                "SELECT name from users WHERE id=?",
                statement -> {
                    statement.setLong(1, id);
                },
                rs -> rs.getString(1),
                String.class
        ).orElse("");
        assertEquals("HI", result);
        log.debug("Result from MySQL Select: {}", result);
    }
}
