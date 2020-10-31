package io.github.harishb2k.easy.database.mysql.transaction;

import com.google.common.base.Strings;
import org.springframework.transaction.annotation.Transactional;

import static io.github.harishb2k.easy.database.DatabaseConstant.DATASOURCE_DEFAULT;

/**
 * Resolve the name of the transaction manager to be used.
 * <p>
 * See @{@link DefaultTransactionManagerResolver} for default implementation, which will resolve
 * transaction manager using @{@link Transactional} annotation
 */
public interface ITransactionManagerResolver {

    /**
     * Provides the name of the transaction resolver
     */
    String resolveTransactionManager(Transactional transactional);

    /**
     * Default transaction manager is "default". It will give value from @{@link Transactional}
     * as the transaction manager.
     */
    class DefaultTransactionManagerResolver implements ITransactionManagerResolver {

        @Override
        public String resolveTransactionManager(Transactional transactional) {
            String transactionManager = DATASOURCE_DEFAULT;
            if (!Strings.isNullOrEmpty(transactional.value())) {
                transactionManager = transactional.value();
            }
            return transactionManager;
        }
    }
}
