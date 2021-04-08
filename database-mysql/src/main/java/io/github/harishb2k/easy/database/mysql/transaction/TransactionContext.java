package io.github.devlibx.easy.database.mysql.transaction;

import lombok.Data;

public class TransactionContext {
    private final ThreadLocal<Context> context = new ThreadLocal<>();
    private static final TransactionContext INSTANCE = new TransactionContext();

    public static TransactionContext getInstance() {
        return INSTANCE;
    }

    // Context getter method
    public Context getContext() {
        Context _context = context.get();
        if (_context == null) {
            _context = new Context();
            context.set(_context);
        }
        return _context;
    }

    public void clear() {
        context.remove();
    }

    @Data
    public static class Context {
        private String datasourceName;

        // Create a copy of context
        public Context cloneContext() {
            Context context = new Context();
            context.setDatasourceName(datasourceName);
            return context;
        }
    }
}
