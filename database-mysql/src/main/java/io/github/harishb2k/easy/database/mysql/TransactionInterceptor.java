package io.github.harishb2k.easy.database.mysql;

import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;

import javax.sql.DataSource;

@Slf4j
public class TransactionInterceptor implements MethodInterceptor {
    private DataSourceTransactionManager transactionManager;
    private boolean doNotTryToInitializeTransactionManager = false;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Transactional transactional = invocation.getMethod().getAnnotation(Transactional.class);
        if (transactional == null) {
            log.trace("Execute method without transaction - method is not marked with Transactional");
            return invocation.proceed();
        }

        // Make sure we initialized transaction manager if required
        ensureDataSourceTransactionManager();

        // Do not do anything if we do not have transaction manager
        if (transactionManager == null) {
            log.trace("Execute method without transaction - method is marked with Transactional, but we do not have transaction manager");
            return invocation.proceed();
        }

        DefaultTransactionDefinition definition = new DefaultTransactionDefinition(transactional.propagation().value());
        definition.setName(transactional.value());
        definition.setTimeout(transactional.timeout());
        DefaultTransactionStatus transactionStatus = (DefaultTransactionStatus) transactionManager.getTransaction(definition);

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
        }
    }

    private synchronized void ensureDataSourceTransactionManager() {

        // If we have already created or we found we can not create transaction manager then exit
        if (doNotTryToInitializeTransactionManager) return;

        DataSource dataSource = ApplicationContext.getInstance(DataSource.class);
        if (dataSource instanceof DataSourceProxy) {
            DataSource transactionAwareDataSource = ((DataSourceProxy) dataSource).getDataSource();
            if (transactionAwareDataSource instanceof TransactionAwareDataSourceProxy) {
                transactionManager = new DataSourceTransactionManager(transactionAwareDataSource);
            }
        }

        // Do not try next time
        doNotTryToInitializeTransactionManager = true;
    }
}
