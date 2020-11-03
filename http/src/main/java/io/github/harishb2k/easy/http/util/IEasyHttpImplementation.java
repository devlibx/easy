package io.github.harishb2k.easy.http.util;

import io.github.harishb2k.easy.http.config.Config;
import io.reactivex.rxjava3.core.Observable;

public interface IEasyHttpImplementation {

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
