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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@Slf4j
public class MySqlDistributedLockV2 implements IDistributedLock {
    private static final ThreadLockStore lockStore = new ThreadLockStore();
    private final DataSource dataSource;
    private final String lockTableName;
    private LockConfig lockConfig;

    @Inject
    public MySqlDistributedLockV2(
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
        } else {
            throw new RuntimeException("Could not close datasource - dataSource Class=" + dataSource.getClass());
        }
    }

    @Override
    public void releaseResources() {
        lockStore.reset();
    }

    @Override
    public Lock achieveLock(LockRequest request) {

        // If we already have a lock in this thread and same request id made - then just give a no-op lock
        InternalLock existingLock = lockStore.hasExistingRequest(request);
        if (existingLock != null) {
            return new ExistingLockWithNoOp(existingLock);
        }

        InternalLock internalLock = new InternalLock(dataSource, request, lockTableName, lockConfig);
        internalLock.lock();
        lockStore.set(request, internalLock);

        return internalLock;
    }

    @Override
    public void releaseLock(Lock lock, LockRequest lockRequest) {
        try {
            lock.unlock();
        } finally {
            if (!(lock instanceof ExistingLockWithNoOp)) {
                if (lock instanceof InternalLock) {
                    lockStore.remove(((InternalLock) lock).getRequest());
                } else {
                    lockStore.remove(lockRequest);
                }
            }
        }
    }

    @Data
    private static class InternalLock implements Lock {
        private final DataSource dataSource;
        private final LockRequest request;
        private final String lockTableName;
        private final LockConfig lockConfig;
        private Connection connection;
        private PreparedStatement selectStatement;

        private InternalLock(DataSource dataSource, LockRequest request, String lockTableName, LockConfig lockConfig) {
            this.dataSource = dataSource;
            this.request = request;
            this.lockTableName = lockTableName;
            this.lockConfig = lockConfig;
        }

        // Run select for update to take lock
        private boolean tryLockWithSelect(Connection connection, String lockIdToUse) throws SQLException {

            // Close any open statement if exist
            if (selectStatement != null) {
                Safe.safe(() -> selectStatement.close());
            }

            // Run select for update
            selectStatement = connection.prepareStatement(
                    String.format("SELECT * FROM %s WHERE lock_id=? FOR UPDATE", lockTableName)
            );
            selectStatement.setString(1, lockIdToUse);
            selectStatement.setQueryTimeout(lockConfig.getTimeoutInSec());
            ResultSet rs = selectStatement.executeQuery();

            // This will return true if we already have a lock in DB - otherwise we will insert an new now in
            // next method
            return rs.next();
        }

        private boolean tryInsertLock(Connection connection, String lockIdToUse) {
            try (PreparedStatement insertStatement = connection.prepareStatement(
                    String.format("INSERT IGNORE INTO %s(lock_id) VALUES(?)", lockTableName))
            ) {
                log.trace("Try to inserted lock row in db: id={}", lockIdToUse);
                insertStatement.setString(1, lockIdToUse);
                insertStatement.setQueryTimeout(lockConfig.getTimeoutInSec());
                insertStatement.execute();
                log.trace("Lock row inserted: id={}", lockIdToUse);
            } catch (Throwable e) {
                log.error("Failed to insert lock for the first time", e);
                return false;
            }
            return true;
        }

        @Override
        public void lock() {

            //  We may have to re-try lock (for concurrent request)
            int retryPending = 1;

            // Lock to be used
            String lockIdToUse = request.getUniqueLockIdForLocking();
            log.debug("Try to take lock: id={}", lockIdToUse);

            do {
                try {

                    // Just is just a safety check - this should never happen
                    if (connection != null) {
                        Safe.safe(() -> {
                            connection.setAutoCommit(true);
                            connection.close();
                            connection = null;
                        });
                    }

                    // Step 1 - Take connection to do all the work
                    connection = dataSource.getConnection();
                    connection.setAutoCommit(false);

                    // Step 2 - select for update will take a lock for this row
                    boolean foundLockInDB = tryLockWithSelect(connection, lockIdToUse);
                    if (foundLockInDB) {
                        log.debug("Lock taken: id={}", lockIdToUse);
                        return;
                    }

                    // Step 3 - Lock not found - try to insert a new row
                    log.debug("(first time lock) Try to insert lock: id={}", lockIdToUse);
                    boolean insertLock = tryInsertLock(connection, lockIdToUse);
                    if (insertLock) {
                        log.debug("Lock inserted (we acquired lock): id={}", lockIdToUse);
                        return;
                    }

                } catch (Exception e) {
                    log.error("Error in getting lock - we will retry: id={}, error={}", lockIdToUse, e.getMessage());
                    Safe.safe(() -> {
                        selectStatement.close();
                        selectStatement = null;
                    });
                    Safe.safe(() -> {
                        connection.rollback();
                        connection.setAutoCommit(true);
                        connection.close();
                        connection = null;
                    });
                    Safe.safe(() -> {
                        if (connection != null) {
                            connection.close();
                            connection = null;
                        }
                    });
                } finally {
                    retryPending--;
                }

                log.warn("Lock not taken: id={}, retry_count={}", lockIdToUse, retryPending);
            } while (retryPending >= 0);

            // Lock not taken - throw exception
            throw new RuntimeException(String.format("lock cannot be taken: name=%s, id=%s", request.getName(), request.getLockId()));
        }

        @Override
        public void unlock() {
            log.debug("Try to unlock lock: id={}", request.getUniqueLockIdForLocking());
            try {
                Safe.safe(() -> {
                    selectStatement.close();
                    selectStatement = null;
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

        @SuppressWarnings("NullableProblems")
        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            throw new RuntimeException("Not implemented");
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Condition newCondition() {
            throw new RuntimeException("Not implemented");
        }
    }

    private static class ThreadLockStore {
        private static final ThreadLocal<Map<String, InternalLock>> locksInCurrentThread = new ThreadLocal<>();

        public InternalLock hasExistingRequest(LockRequest request) {

            if (locksInCurrentThread.get() == null) return null;

            // Dump error if we see a leak, and somehow we have > 10 locks. It is not very common that you see
            // call stack of > 10 and each method has a lock
            if (locksInCurrentThread.get().size() > 10) {
                Thread.dumpStack();
                log.error("Potential leak in MySqlDistributedLockV2 class: thread local has > 10 locks. It could only happen if your " +
                        "call stack is > 10 and more than 10 method in call stack are taking distributed lock with different lock IDs. " +
                        "If this is not the case then this is a bug in MySqlDistributedLockV2 implementation.");
            }

            Map.Entry<String, InternalLock> key = locksInCurrentThread.get().entrySet().stream()
                    .filter(entry -> Objects.equals(entry.getKey(), request.getUniqueLockIdForLocking()))
                    .findFirst()
                    .orElse(null);
            return key != null ? key.getValue() : null;
        }

        public void set(LockRequest request, InternalLock lock) {
            if (locksInCurrentThread.get() == null) {
                locksInCurrentThread.set(new HashMap<>());
            }
            Map<String, InternalLock> locks = locksInCurrentThread.get();
            locks.put(request.getUniqueLockIdForLocking(), lock);
        }

        public void remove(LockRequest lockRequest) {
            if (locksInCurrentThread.get() != null) {
                locksInCurrentThread.get().remove(lockRequest.getUniqueLockIdForLocking());
            }
        }

        public void reset() {
            if (locksInCurrentThread.get() != null) {
                locksInCurrentThread.get().clear();
            }
            locksInCurrentThread.remove();
        }
    }
}
