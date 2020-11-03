package io.github.harishb2k.easy.http.util;

import io.github.harishb2k.easy.http.config.Config;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyBadRequestException;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyHttpRequestException;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyInternalServerErrorException;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyRequestTimeOutException;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyResilienceException;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyResilienceRequestTimeoutException;
import io.reactivex.rxjava3.core.Observable;

public class EasyHttp {
    private static IEasyHttpImplementation defaultEasyHttpImplementation = new EasyHttpObject();

    /**
     * This is a method to setup you custom EasyHttpObject. It is mainly helpful in test cases.
     */
    public static void installEasyHttpImplementation(IEasyHttpImplementation easyHttpImplementor) {
        defaultEasyHttpImplementation = easyHttpImplementor;
    }

    /**
     * Setup EasyHttp to make HTTP requests
     */
    public static void setup(Config config) {
        defaultEasyHttpImplementation.setup(config);
    }

    /**
     * Free all resources
     */
    public static void shutdown() {
        defaultEasyHttpImplementation.shutdown();
    }

    /**
     * Make a HTTP call which returns a response.
     * </br>
     * Note - to check for timeout errors you must catch both
     * {@link EasyResilienceRequestTimeoutException} and {@link EasyRequestTimeOutException}
     *
     * @param call request object
     * @param <T>  type of response
     * @return response of http call
     * @throws EasyHttpRequestException if error, it provides {@link EasyHttpRequestException}. You can catch specific
     *                                  type of errors by caching sub-class of EasyHttpRequestException.
     *                                  e.g. {@link EasyInternalServerErrorException}, {@link EasyBadRequestException}
     *                                  <p>
     *                                  It also throws {@link EasyResilienceException} which is also a sub-class of
     *                                  {@link EasyHttpRequestException}. These resilience exception are thrown when
     *                                  circuit is open, too many calls are made, or request timed out.
     *                                  <p>
     */
    public static <T> T callSync(Call<T> call) {
        return defaultEasyHttpImplementation.callSync(call);
    }

    /**
     * Make a HTTP call which returns a observable.
     * </br>
     * Note - to check for timeout errors you must catch both
     * {@link EasyResilienceRequestTimeoutException} and {@link EasyRequestTimeOutException}
     * <p>
     *
     * @param call request object
     * @param <T>  type of response
     * @return observable for response of http call
     * @throws EasyHttpRequestException <b color='red'>(exceptions will be received in the onError callback in subscriber)</b>
     *                                  if error, it provides {@link EasyHttpRequestException}. You can catch specific
     *                                  type of errors by caching sub-class of EasyHttpRequestException.
     *                                  e.g. {@link EasyInternalServerErrorException}, {@link EasyBadRequestException}
     *                                  <p>
     *                                  It also throws {@link EasyResilienceException} which is also a sub-class of
     *                                  {@link EasyHttpRequestException}. These resilience exception are thrown when
     *                                  circuit is open, too many calls are made, or request timed out.
     *                                  <p>
     */
    public static <T> Observable<T> callAsync(Call<T> call) {
        return defaultEasyHttpImplementation.callAsync(call);
    }
}
