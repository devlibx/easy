package io.github.devlibx.easy.database.exception;

public interface DatabaseExceptions {

    class BaseDatabaseException extends RuntimeException {
        public BaseDatabaseException(Throwable e) {
            super(e);
        }

        public BaseDatabaseException(String message, Throwable e) {
            super(message, e);
        }
    }

    class PersistException extends BaseDatabaseException {
        public PersistException(String sql, Throwable e) {
            super(String.format("Failed to persist: %s", sql), e);
        }
    }

    class FindException extends BaseDatabaseException {
        public FindException(String sql, Throwable e) {
            super(String.format("Failed to find: %s", sql), e);
        }
    }

    class FindAllException extends BaseDatabaseException {
        public FindAllException(String sql, Throwable e) {
            super(String.format("Failed to find all: %s", sql), e);
        }
    }

    class ExecuteException extends BaseDatabaseException {
        public ExecuteException(String sql, Throwable e) {
            super(String.format("Failed to execute: %s", sql), e);
        }
    }
}
