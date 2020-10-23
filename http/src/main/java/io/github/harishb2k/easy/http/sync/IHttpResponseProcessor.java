package io.github.harishb2k.easy.http.sync;

import io.github.harishb2k.easy.http.ResponseObject;
import io.github.harishb2k.easy.http.config.Api;
import io.github.harishb2k.easy.http.config.Server;
import org.apache.http.client.methods.CloseableHttpResponse;

public interface IHttpResponseProcessor {
    ResponseObject process(Server server, Api api, CloseableHttpResponse response);
    ResponseObject processException(Server server, Api api, Throwable e);
}
