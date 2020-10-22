package io.github.harishb2k.easy.database.mysql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings("UnusedReturnValue")
public interface IMysqlHelper {

    /**
     * Execute a SQL (Do not use it for UPDATE query - use executeUpdate)
     * <p>
     * NOTE - a "CREATE TABLE" using this returns false (as per the Java Doc of PreparedStatement)
     *
     * @param metric           metric name to log this execution
     * @param sql              SQL to execute
     * @param statementBuilder callback hook to set param in SQL statement
     * @return true if success otherwise false
     */
    boolean execute(String metric, String sql, IStatementBuilder statementBuilder);

    /**
     * Execute a SQL
     *
     * @param metric           metric name to log this execution
     * @param sql              SQL to execute
     * @param statementBuilder callback hook to set param in SQL statement
     * @return true if success otherwise false
     */
    boolean executeUpdate(String metric, String sql, IStatementBuilder statementBuilder);

    /**
     * Persist a record
     *
     * @param metric           metric name to log this execution
     * @param sql              SQL to execute
     * @param statementBuilder callback hook to set param in SQL statement
     * @return PK for this row as long
     */
    Long persist(String metric, String sql, IStatementBuilder statementBuilder);

    /**
     * Persist a record
     *
     * @param metric           metric name to log this execution
     * @param sql              SQL to execute
     * @param statementBuilder callback hook to set param in SQL statement
     * @param keyFunction      function to extract key of this row
     * @return PK for this row
     */
    <T> T persist(String metric, String sql, IStatementBuilder statementBuilder, Function<ResultSet, T> keyFunction);

    /**
     * Fina a single record
     *
     * @param metric           metric name to log this execution
     * @param sql              SQL to execute
     * @param statementBuilder callback hook to set param in SQL statement
     * @param rowMapper        mapper to build object from DB row
     * @return Optional object with found record
     */
    <T> Optional<T> findOne(String metric, String sql, IStatementBuilder statementBuilder, IRowMapper<T> rowMapper, Class<T> cls);

    /**
     * Fina all record
     *
     * @param metric           metric name to log this execution
     * @param sql              SQL to execute
     * @param statementBuilder callback hook to set param in SQL statement
     * @param rowMapper        mapper to build object from DB row
     * @return Optional object (of type list) with found record
     */
    <T> Optional<List<T>> findAll(String metric, String sql, IStatementBuilder statementBuilder, IRowMapper<T> rowMapper, Class<T> cls);

    /**
     * A callback interface to set query param in SQL
     */
    interface IStatementBuilder {
        void prepare(PreparedStatement statement) throws SQLException;
    }

    /**
     * Mapper to build Java object from DB result
     *
     * @param <T>
     */
    interface IRowMapper<T> {
        T map(ResultSet rs) throws SQLException;

        default Optional<List<T>> rows(ResultSet rs) throws SQLException {
            List<T> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list.isEmpty() ? Optional.empty() : Optional.of(list);
        }
    }
}
