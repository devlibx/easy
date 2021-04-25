package io.github.devlibx.easy.testing.mysql.base;

import com.google.inject.Injector;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("ALL")
@NoArgsConstructor
public class PersistenceTestState {
    private static final PersistenceTestState PERSISTENCE_TEST_STATE = new PersistenceTestState();
    private final ThreadLocal<PersistenceTestContext> context = new ThreadLocal();

    public static PersistenceTestState instance() {
        return PERSISTENCE_TEST_STATE;
    }

    public void clear() {
        context.remove();
    }

    public PersistenceTestContext getContext() {
        PersistenceTestContext _context = context.get();
        if (_context == null) {
            _context = new PersistenceTestContext();
            context.set(_context);
        }
        return _context;
    }

    @Data
    public static class PersistenceTestContext {
        private Injector injector;
        private Map<String, Long> longs;
        private Map<String, UUID> uuids;
        private Map<String, String> strings;

        private String mysqlUser = "test";
        private String mysqlPassword = "test";
        private String jdbcUrl = "jdbc:mysql://localhost:3306/users?useSSL=false";
    }
}
