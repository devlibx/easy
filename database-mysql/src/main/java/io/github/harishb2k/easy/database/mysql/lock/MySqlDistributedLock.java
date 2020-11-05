package io.github.harishb2k.easy.database.mysql.lock;

import com.zaxxer.hikari.HikariDataSource;
import io.gitbub.harishb2k.easy.helper.Safe;
import io.github.harishb2k.easy.lock.IDistributedLock;
import io.github.harishb2k.easy.lock.IDistributedLockService.ExistingLockWithNoOp;
import io.github.harishb2k.easy.lock.config.LockConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@Slf4j
public class MySqlDistributedLock implements IDistributedLock {
    private static final ThreadLocal<InternalLock> lockInCurrentThread = new ThreadLocal<>();
    private final DataSource dataSource;
    private final String lockTableName;
    private LockConfig lockConfig;

    @Inject
    public MySqlDistributedLock(
            @Named("lock_table_data_source") DataSource dataSource,
            @Named("lock_table_name") String lockTableName
    ) {
        this.dataSource = dataSource;
        this.lockTableName = lockTableName;
    }

    @Override
    public void setup(LockConfig lockConfig) {
        this.lockConfig = lockConfig;
    }

    @Override
    public void tearDown() {
        if (dataSource instanceof HikariDataSource) {
            Safe.safe(((HikariDataSource) dataSource)::close);
        }
    }

    @Override
    public Lock achieveLock(LockRequest request) {

        // If we already have a lock in this thread and same request id made - then just give a no-op lock
        if (lockInCurrentThread.get() != null) {
            if (Objects.equals(lockInCurrentThread.get().request, request)) {
                return new ExistingLockWithNoOp(lockInCurrentThread.get());
            }
        }

        InternalLock internalLock = new InternalLock(dataSource, request, lockTableName, lockConfig);
        internalLock.lock();
        lockInCurrentThread.set(internalLock);

        return internalLock;
    }

    @Override
    public void releaseLock(Lock lock, LockRequest lockRequest) {
        try {
            lock.unlock();
        } finally {
            lockInCurrentThread.set(null);
        }
    }

    @Data
    private static class InternalLock implements Lock {
        private final DataSource dataSource;
        private final LockRequest request;
        private final String lockTableName;
        private Connection connection;
        private Connection insertConnection;
        private PreparedStatement insertStatement;
        private PreparedStatement lockStatement;
        private final LockConfig lockConfig;

        private InternalLock(DataSource dataSource, LockRequest request, String lockTableName, LockConfig lockConfig) {
            this.dataSource = dataSource;
            this.request = request;
            this.lockTableName = lockTableName;
            this.lockConfig = lockConfig;
        }

        @Override
        public void lock() {
            try {

                String lockIdToUse = request.getUniqueLockIdForLocking();

                // Make sure we do have a entry in table for locking
                log.debug("Try to insert lock: id={}", lockIdToUse);
                try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(String.format("INSERT IGNORE INTO %s(lock_id) VALUES(?)", lockTableName))) {
                    statement.setQueryTimeout(lockConfig.getTimeoutInMs());
                    statement.setString(1, lockIdToUse);
                    statement.execute();
                    log.debug("Inserted lock: id={}", lockIdToUse);
                } catch (Exception e) {
                    log.error("Failed to insert lock for the first time", e);
                }

                log.debug("Try to lock with select for update: id={}", lockIdToUse);
                connection = dataSource.getConnection();
                connection.setAutoCommit(false);
                lockStatement = connection.prepareStatement(String.format("SELECT * FROM %s WHERE lock_id=? FOR UPDATE", lockTableName));
                lockStatement.setQueryTimeout(lockConfig.getTimeoutInMs());
                lockStatement.setString(1, lockIdToUse);
                lockStatement.executeQuery();
                log.debug("Lock taken with select for update: id={}", lockIdToUse);

            } catch (Throwable e) {
                Safe.safe(() -> {
                    lockStatement.close();
                    lockStatement = null;
                });
                Safe.safe(() -> {
                    connection.setAutoCommit(true);
                    connection.rollback();
                    connection.close();
                    connection = null;
                });
                throw new RuntimeException(String.format("lock cannot be taken: name=%s, id=%s", request.getName(), request.getLockId()), e);
            }
        }

        @Override
        public void unlock() {
            log.debug("Try to unlock lock: id={}", request.getUniqueLockIdForLocking());
            try {
                Safe.safe(() -> {
                    lockStatement.close();
                    lockStatement = null;
                });
                connection.commit();
                connection.setAutoCommit(true);
                connection.close();
                connection = null;
                log.debug("Unlock done: id={}", request.getUniqueLockIdForLocking());
            } catch (Exception e) {
                throw new RuntimeException(String.format("lock cannot be released: name=%s, id=%s", request.getName(), request.getLockId()), e);
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean tryLock() {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public Condition newCondition() {
            throw new RuntimeException("Not implemented");
        }
    }
}
