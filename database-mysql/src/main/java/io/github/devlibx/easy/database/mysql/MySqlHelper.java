package io.github.devlibx.easy.database.mysql;

import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.github.devlibx.easy.database.exception.DatabaseExceptions.ExecuteException;
import io.github.devlibx.easy.database.exception.DatabaseExceptions.FindException;
import io.github.devlibx.easy.database.exception.DatabaseExceptions.PersistException;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

@Slf4j
public class MySqlHelper implements IMysqlHelper {
    private final DataSource dataSource;
    private final IMetrics metrics;
    private final Set<String> metricsRegistered;

    @Inject
    public MySqlHelper(DataSource dataSource, IMetrics metrics) {
        this.dataSource = dataSource;
        this.metrics = metrics;
        this.metricsRegistered = new HashSet<>();
    }

    @Override
    public boolean execute(String metricsName, String sql, IStatementBuilder statementBuilder) {
        safeRegisterMetric(metricsName);
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statementBuilder.prepare(statement);
            return metrics.time(metricsName, statement::execute);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                e.printStackTrace();
            }
            throw new ExecuteException(sql, e);
        }
    }

    @Override
    public boolean executeUpdate(String metricsName, String sql, IStatementBuilder statementBuilder) {
        safeRegisterMetric(metricsName);
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statementBuilder.prepare(statement);
            return metrics.time(metricsName, () -> statement.executeUpdate() > 0);
        } catch (Exception e) {
            throw new ExecuteException(sql, e);
        }
    }

    @Override
    public Long persist(String metricsName, String sql, IStatementBuilder statementBuilder) {
        safeRegisterMetric(metricsName);
        return (Long) persist(metricsName, sql, statementBuilder, resultSet -> {
            try {
                return resultSet.getLong(1);
            } catch (SQLException e) {
                return new PersistException(sql, e);
            }
        });
    }

    @Override
    public <T> T persist(String metricsName, String sql, IStatementBuilder statementBuilder, Function<ResultSet, T> keyFunction) {
        safeRegisterMetric(metricsName);
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql, RETURN_GENERATED_KEYS)) {
            statementBuilder.prepare(statement);
            metrics.time(metricsName, statement::execute);
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return keyFunction.apply(generatedKeys);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new PersistException(sql, e);
        }
    }

    @Override
    public <T> Optional<T> findOne(String metricsName, String sql, IStatementBuilder statementBuilder, IRowMapper<T> rowMapper, Class<T> cls) {
        safeRegisterMetric(metricsName);
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statementBuilder.prepare(statement);
            return metrics.time(metricsName, () -> {
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? Optional.of(rowMapper.map(rs)) : Optional.empty();
                }
            });
        } catch (Exception e) {
            throw new FindException(sql, e);
        }
    }

    @Override
    public <T> Optional<List<T>> findAll(String metricsName, String sql, IStatementBuilder statementBuilder, IRowMapper<T> rowMapper, Class<T> cls) {
        safeRegisterMetric(metricsName);
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statementBuilder.prepare(statement);
            return metrics.time(metricsName, () -> {
                try (ResultSet rs = statement.executeQuery()) {
                    return rowMapper.rows(rs);
                }
            });
        } catch (Exception e) {
            throw new FindException(sql, e);
        }
    }

    private void safeRegisterMetric(String name) {
        if (metricsRegistered.contains(name)) return;
        metrics.registerTimer(name, name + " Help");
        metricsRegistered.add(name);
    }
}
