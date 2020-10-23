package io.github.harishb2k.easy.http;

public interface IRequestProcessor {

    /**
     * Handle a request
     *
     * @param requestObject request information
     * @return response of http call
     */
    ResponseObject process(RequestObject requestObject);

}
