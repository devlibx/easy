package io.github.harishb2k.easy.database.mysql.transaction;

import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.github.harishb2k.easy.database.mysql.DataSourceFactory;
import io.github.harishb2k.easy.database.mysql.transaction.TransactionContext.Context;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TransactionInterceptor implements MethodInterceptor {
    private final Map<String, DataSourceTransactionManager> transactionManagerMap;
    private final int defaultTimeout;
    private ITransactionManagerResolver transactionManagerResolver;

    public TransactionInterceptor(int defaultTimeout) {
        this.transactionManagerMap = new ConcurrentHashMap<>();
        this.defaultTimeout = defaultTimeout;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Transactional transactional = invocation.getMethod().getAnnotation(Transactional.class);
        if (transactional == null) {
            log.trace("Execute method without transaction - method is not marked with Transactional");
            return invocation.proceed();
        }

        // Process labels provided in Transactional
        TransactionContext.getInstance().clear();
        resolveTransactionManagerByName(transactional);

        // Keep a clone copy of context. This is needed because we can have recursive call of  Transactional methods
        // Those methods will update the context
        Context context = TransactionContext.getInstance().getContext().cloneContext();

        // Make sure we initialized transaction manager if required
        DataSourceTransactionManager transactionManager = ensureDataSourceTransactionManager();

        // Do not do anything if we do not have transaction manager
        if (transactionManager == null) {
            log.trace("Execute method without transaction - method is marked with Transactional, but we do not have transaction manager");
            return invocation.proceed();
        }

        DefaultTransactionDefinition definition = new DefaultTransactionDefinition(transactional.propagation().value());
        definition.setName(transactional.value());
        definition.setTimeout(transactional.timeout() > 0 ? transactional.timeout() : 10);
        DefaultTransactionStatus transactionStatus = (DefaultTransactionStatus) transactionManager.getTransaction(definition);

        // Set the user provided name - it is useful in debugging
        resolveName(transactional, definition);

        Object result;
        try {
            result = invocation.proceed();
            log.trace("Execute with transaction (commit) - result={}", result);
            transactionManager.commit(transactionStatus);
            return result;
        } catch (Exception e) {
            log.trace("Execute with transaction failed (rollback) - exception={}", e.getMessage(), e);
            transactionManager.rollback(transactionStatus);
            throw e;
        } finally {
            TransactionContext.getInstance().clear();
        }
    }

    public void resolveTransactionManagerByName(Transactional transactional) {
        if (transactionManagerResolver == null) {
            transactionManagerResolver = ApplicationContext.getInstance(ITransactionManagerResolver.class);
        }
        String transactionManagerToUse = transactionManagerResolver.resolveTransactionManager(transactional);
        Context context = TransactionContext.getInstance().getContext();
        context.setDatasourceName(transactionManagerToUse);
    }

    public void resolveName(Transactional transactional, DefaultTransactionDefinition definition) {
        if (transactional.label().length == 0) return;
        for (String label : transactional.label()) {
            StringTokenizer st = new StringTokenizer(label, "=");
            String token = st.nextToken();
            if (Objects.equals("name", token)) {
                definition.setName(st.nextToken());
            }
        }
    }

    private synchronized DataSourceTransactionManager ensureDataSourceTransactionManager() {
        Context context = TransactionContext.getInstance().getContext();
        String dataSourceName = context.getDatasourceName();

        if (transactionManagerMap.containsKey(dataSourceName)) {
            return transactionManagerMap.get(dataSourceName);
        }

        DataSourceFactory dataSourceFactory = ApplicationContext.getInstance(DataSourceFactory.class);
        DataSource dataSource = dataSourceFactory.getDataSource(dataSourceName);
        if (dataSource instanceof TransactionAwareDataSourceProxy) {
            DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
            transactionManager.setDefaultTimeout(defaultTimeout);
            transactionManagerMap.put(dataSourceName, transactionManager);
            log.info("Created new dataSourceManager for dataSource={}", dataSourceName);
        }
        return transactionManagerMap.get(dataSourceName);
    }
}
