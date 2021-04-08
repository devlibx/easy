package io.github.devlibx.easy.http.sync;

import io.github.devlibx.easy.http.ResponseObject;
import io.github.devlibx.easy.http.config.Api;
import io.github.devlibx.easy.http.config.Server;
import org.apache.http.client.methods.CloseableHttpResponse;

public interface IHttpResponseProcessor {
    ResponseObject process(Server server, Api api, CloseableHttpResponse response);

    ResponseObject processException(Server server, Api api, Throwable e);

    void processResponseForException(ResponseObject response);
}
