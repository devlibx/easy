package io.github.devlibx.easy.http.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Strings;
import io.gitbub.devlibx.easy.helper.json.JsonUtils;
import io.vavr.Function0;
import lombok.Data;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Call<R> {
    private String server;
    private String api;
    private Map<String, Object> headers = new HashMap<>();
    private Map<String, Object> pathParams;
    private MultivaluedMap<String, Object> queryParam;
    private Object body;
    private Class<R> responseClass;
    private IResponseBuilderFunc<R> responseBuilder;
    private Function0<byte[]> requestBodyFunc;

    private Call() {
    }

    public byte[] getBodyAsByteArray() {
        return requestBodyFunc.apply();
    }

    /**
     * A call object builder
     */
    public static <R> Builder<R> builder(Class<R> responseClass) {
        return new Builder<>(responseClass);
    }

    /**
     * Builder class
     */
    public static class Builder<R> {
        private String server;
        private String api;
        private Map<String, Object> headers;
        private Map<String, Object> pathParams;
        private MultivaluedMap<String, Object> queryParam;
        private Object body;
        private final Class<R> responseClass;
        private IResponseBuilderFunc<R> responseBuilder;
        private Function0<byte[]> requestBodyFunc;

        public Builder(Class<R> responseClass) {
            this.responseClass = responseClass;

            // Default response builder
            this.responseBuilder = bytes -> {
                if (bytes != null) {
                    String str = new String(bytes);
                    return JsonUtils.readObject(str, responseClass);
                }
                return null;
            };

            // Default method to convert body to byte array
            this.requestBodyFunc = () -> {
                if (body == null) {
                    return null;
                } else if (body instanceof byte[]) {
                    return (byte[]) body;
                } else {
                    String _body = JsonUtils.asJson(body);
                    if (_body != null) {
                        return _body.getBytes();
                    }
                }
                return null;
            };
        }

        /**
         * @param responseBuilder a build from byte array to desired type
         */
        public Builder<R> withResponseBuilder(IResponseBuilderFunc<R> responseBuilder) {
            this.responseBuilder = responseBuilder;
            return this;
        }

        /**
         * Ensure that all the required params are provided.
         */
        private void validate() {
            if (Strings.isNullOrEmpty(server) || Strings.isNullOrEmpty(api)) {
                throw new IllegalArgumentException("server and api must be set to create a valid call objetc");
            }
        }

        /**
         * @return a call object
         */
        public Call<R> build() {

            // Make sure we have all required fields set
            validate();

            Call<R> call = new Call<>();
            call.server = server;
            call.api = api;
            call.headers = headers;
            call.pathParams = pathParams;
            call.queryParam = queryParam;
            call.body = body;
            call.responseClass = responseClass;
            call.responseBuilder = responseBuilder;
            call.requestBodyFunc = requestBodyFunc;
            return call;
        }

        /**
         * @param api name of the API
         * @return builder object
         */
        public Builder<R> withServerAndApi(String server, String api) {
            this.server = server;
            this.api = api;
            return this;
        }

        /**
         * @param requestBodyFunc a function to return byte array - used when user wants to write custom object to
         *                        byte array implementation
         */
        public Builder<R> withRequestBodyFunc(Function0<byte[]> requestBodyFunc) {
            this.requestBodyFunc = requestBodyFunc;
            return this;
        }

        /**
         * @param body body to be passed in the request
         * @return builder object
         */
        public Builder<R> withBody(Object body) {
            this.body = body;
            return this;
        }

        /**
         * Sets request content-type header as application/json
         *
         * @return builder object
         */
        public Builder<R> asContentTypeJson() {
            getHeaders().put("Content-Type", "application/json");
            return this;
        }

        /**
         * Sets request content-type header as application/x-protobuf
         *
         * @return builder object
         */
        public Builder<R> asContentTypeProtoBuffer() {
            getHeaders().put("Content-Type", "application/x-protobuf");
            return this;
        }

        /**
         * Sets request content-type header as application/x-protobuf
         *
         * @return builder object
         */
        public Builder<R> asContentTypeProtoBufferJson() {
            getHeaders().put("Content-Type", "application/x-protobuf-json-format");
            return this;
        }

        /**
         * Add a key-value for path param
         *
         * @return builder object
         */
        public Builder<R> addPathParam(String key, Object value) {
            getPathParams().put(key, String.format("%s", value));
            return this;
        }

        /**
         * Add all key-value for path param. This list size must be even.
         *
         * @return builder object
         * @throws RuntimeException if params size is not even
         */
        public Builder<R> addPathParams(Object... params) {
            if (params.length % 2 != 0) {
                throw new RuntimeException("params count must be even");
            }
            for (int i = 0; i < params.length; i = i + 2) {
                getPathParams().put(String.format("%s", params[i]), String.format("%s", params[i + 1]));
            }
            return this;
        }

        /**
         * Add all key-value for path param.
         *
         * @return builder object
         */
        public Builder<R> addPathParams(Map<String, Object> pathParams) {
            getPathParams().putAll(pathParams);
            return this;
        }

        /**
         * Add a key-value for header
         *
         * @return builder object
         */
        public Builder<R> addHeader(String key, Object value) {
            getHeaders().put(key, String.format("%s", value));
            return this;
        }

        /**
         * Add all key-value for headers. This list size must be even.
         *
         * @return builder object
         * @throws RuntimeException if headers size is not even
         */
        public Builder<R> addHeaders(Object... headers) {
            if (headers.length % 2 != 0) {
                throw new RuntimeException("headers count must be even");
            }
            for (int i = 0; i < headers.length; i = i + 2) {
                getHeaders().put(String.format("%s", headers[i]), String.format("%s", headers[i + 1]));
            }
            return this;
        }

        /**
         * Add all key-value for header
         *
         * @return builder object
         */
        public Builder<R> addHeaders(Map<String, Object> headers) {
            getHeaders().putAll(headers);
            return this;
        }


        /**
         * Add a key-value for query params
         *
         * @return builder object
         */
        public Builder<R> addQueryParam(String key, Object value) {
            getQueryParam().add(key, String.format("%s", value));
            return this;
        }

        /**
         * Add all key-value for query params. This list size must be even.
         *
         * @return builder object
         * @throws RuntimeException if queryParams size is not even
         */
        public Builder<R> addQueryParams(Object... queryParams) {
            if (queryParams.length % 2 != 0) {
                throw new RuntimeException("params count must be even");
            }
            for (int i = 0; i < queryParams.length; i = i + 2) {
                getQueryParam().add(String.format("%s", queryParams[i]), String.format("%s", queryParams[i + 1]));
            }
            return this;
        }

        /**
         * Add all key-value for query params
         *
         * @return builder object
         */
        public Builder<R> addQueryParams(MultivaluedHashMap<String, Object> queryParams) {
            getQueryParam().putAll(queryParams);
            return this;
        }

        /**
         * Add all key-value for query params
         *
         * @return builder object
         */
        public Builder<R> addQueryParams(Map<String, Object> queryParams) {
            queryParams.forEach((key, value) -> {
                getQueryParam().add(key, String.format("%s", value));
            });
            return this;
        }

        private Map<String, Object> getHeaders() {
            if (headers == null) {
                headers = new HashMap<>();
            }
            return headers;
        }

        private Map<String, Object> getPathParams() {
            if (pathParams == null) {
                pathParams = new HashMap<>();
            }
            return pathParams;
        }

        private MultivaluedMap<String, Object> getQueryParam() {
            if (queryParam == null) {
                queryParam = new MultivaluedHashMap<>();
            }
            return queryParam;
        }
    }

    public interface IResponseBuilderFunc<R> {
        R apply(byte[] bytes) throws Exception;
    }
}
