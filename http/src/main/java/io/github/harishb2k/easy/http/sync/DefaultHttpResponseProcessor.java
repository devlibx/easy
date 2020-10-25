package io.github.harishb2k.easy.http.sync;

import io.github.harishb2k.easy.http.ResponseObject;
import io.github.harishb2k.easy.http.config.Api;
import io.github.harishb2k.easy.http.config.Server;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyHttpRequestException;
import io.github.harishb2k.easy.http.exception.EasyHttpExceptions.EasyRequestTimeOutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import static io.github.harishb2k.easy.http.config.Api.DEFAULT_ACCEPTABLE_CODES;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Slf4j
public class DefaultHttpResponseProcessor implements IHttpResponseProcessor {

    @Override
    public ResponseObject process(Server server, Api api, CloseableHttpResponse response) {

        int statusCode = INTERNAL_SERVER_ERROR.getStatusCode();
        if (response == null) {
            return ResponseObject.builder().statusCode(statusCode).build();
        } else {
            statusCode = response.getStatusLine().getStatusCode();
        }

        // Make sure we don't have empty acceptable codes
        if (api.getAcceptableCodes() == null || api.getAcceptableCodes().isEmpty()) {
            api.setAcceptableCodes(DEFAULT_ACCEPTABLE_CODES);
        }

        // See if we are ok with these http codes
        byte[] body = null;
        if (api.getAcceptableCodes().contains(statusCode)) {
            try {
                body = response.getEntity() != null ? EntityUtils.toByteArray(response.getEntity()) : null;
            } catch (Exception ignored) {
            }
            return ResponseObject.builder().success(true).body(body).statusCode(statusCode).build();
        } else {
            try {
                body = response.getEntity() != null ? EntityUtils.toByteArray(response.getEntity()) : null;
                return ResponseObject.builder().body(body).statusCode(statusCode).build();
            } catch (Exception ignored) {
                return ResponseObject.builder().statusCode(statusCode).build();
            }
        }
    }

    @Override
    public ResponseObject processException(Server server, Api api, Throwable e) {
        return ResponseObject.builder().exception(e).statusCode(INTERNAL_SERVER_ERROR.getStatusCode()).build();
    }

    @Override
    public void processResponseForException(ResponseObject response) {
        if (response.isSuccess()) return;

        EasyHttpRequestException exception = EasyHttpExceptions.easyEasyResilienceException(response.getException()).orElse(null);
        if (exception != null) {
            throw exception;
        } else if (response.getException() instanceof SocketTimeoutException) {
            throw new EasyRequestTimeOutException(response);
        } else {
            throw new EasyHttpRequestException(response.getException());
        }
    }
}
