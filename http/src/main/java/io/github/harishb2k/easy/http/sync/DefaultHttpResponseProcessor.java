package io.github.harishb2k.easy.http.sync;

import io.github.harishb2k.easy.http.ResponseObject;
import io.github.harishb2k.easy.http.config.Api;
import io.github.harishb2k.easy.http.config.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

@Slf4j
public class DefaultHttpResponseProcessor implements IHttpResponseProcessor {
    @Override
    public ResponseObject process(Server server, Api api, CloseableHttpResponse response) {
        byte[] body = null;
        try {
            body = response.getEntity() != null ? EntityUtils.toByteArray(response.getEntity()) : null;
        } catch (Exception ignored) {
        }
        return ResponseObject.builder().body(body).build();
    }
}
