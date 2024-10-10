package io.github.devlibx.easy.http.sync;

import io.github.devlibx.easy.http.ResponseObject;
import io.github.devlibx.easy.http.config.Api;
import io.github.devlibx.easy.http.config.Server;
import io.github.devlibx.easy.http.exception.EasyHttpExceptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import static io.github.devlibx.easy.http.config.Api.DEFAULT_ACCEPTABLE_CODES;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import javax.ws.rs.core.Response;

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
            return ResponseObject.builder().success(true).body(body).statusCode(statusCode)
                    .errorWithAcceptableErrorCode(notHttp2xxStatusCode(statusCode))
                    .build();
        } else {
            try {
                body = response.getEntity() != null ? EntityUtils.toByteArray(response.getEntity()) : null;
                return ResponseObject.builder().body(body).statusCode(statusCode).build();
            } catch (Exception ignored) {
                return ResponseObject.builder().statusCode(statusCode).build();
            }
        }
    }

    private boolean notHttp2xxStatusCode(int statusCode) {
        return !isHttp2xxStatusCode(statusCode);
    }

    private boolean isHttp2xxStatusCode(int statusCode) {
        return statusCode >= Response.Status.OK.getStatusCode() && statusCode < Response.Status.MOVED_PERMANENTLY.getStatusCode();
    }

    @Override
    public ResponseObject processException(Server server, Api api, Throwable e) {
        return ResponseObject.builder().exception(e).statusCode(INTERNAL_SERVER_ERROR.getStatusCode()).build();
    }

    @Override
    public void processResponseForException(ResponseObject response) {
        if (response.isSuccess() && !response.isErrorWithAcceptableErrorCode()) return;
        throw EasyHttpExceptions.convert(response.getStatusCode(), response.getException(), response);
    }
}
