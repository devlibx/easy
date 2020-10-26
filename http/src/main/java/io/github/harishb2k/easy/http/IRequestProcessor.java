package io.github.harishb2k.easy.http;

import io.reactivex.rxjava3.core.Observable;

public interface IRequestProcessor {

    /**
     * Handle a request
     *
     * @param requestObject request information
     * @return response of http call
     */
    Observable<ResponseObject> process(RequestObject requestObject);

    /**
     * Cleanup
     */
    void shutdown();
}
