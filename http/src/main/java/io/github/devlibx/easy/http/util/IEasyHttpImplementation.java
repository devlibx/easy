package io.github.devlibx.easy.http.util;

import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.github.devlibx.easy.http.config.Config;
import io.reactivex.rxjava3.core.Observable;

public interface IEasyHttpImplementation {

    /**
     * Setup easy http
     */
    default void setup(Config config, IMetrics metrics) {
        setup(config, metrics);
    }

    /**
     * Setup easy http
     */
    void setup(Config config);

    /**
     * Shutdown easy http
     */
    void shutdown();

    /**
     * Call api in sync
     *
     * @return sync response
     */
    <T> T callSync(Call<T> call);

    /**
     * Call api in async
     *
     * @return observable to notify the final result or error
     */
    <T> Observable<T> callAsync(Call<T> call);
}
