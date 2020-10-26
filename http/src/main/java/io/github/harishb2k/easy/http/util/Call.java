package io.github.harishb2k.easy.http.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Strings;
import io.vavr.Function0;
import lombok.Data;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Call {
    private String server;
    private String api;
    private Map<String, Object> headers = new HashMap<>();
    private Map<String, Object> pathParams;
    private MultivaluedMap<String, Object> queryParam;
    private Object body;

    private Call() {
    }

    /**
     * A call object builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class
     */
    public static class Builder {
        private String server;
        private String api;
        private Map<String, Object> headers;
        private Map<String, Object> pathParams;
        private MultivaluedMap<String, Object> queryParam;
        private Object body;

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
        public Call build() {

            // Make sure we have all required fields set
            validate();

            Call call = new Call();
            call.server = server;
            call.api = api;
            call.headers = headers;
            call.pathParams = pathParams;
            call.queryParam = queryParam;
            call.body = body;
            return call;
        }

        /**
         * @param api name of the API
         * @return builder object
         */
        public Builder withServerAndApi(String server, String api) {
            this.server = server;
            this.api = api;
            return this;
        }

        /**
         * @param body body to be passed in the request
         * @return builder object
         */
        public Builder withBody(Object body) {
            this.body = body;
            return this;
        }

        /**
         * @param bodyFunc a function which returns the body object to be passed in request
         * @return builder object
         */
        public Builder withBodyFunc(Function0<Object> bodyFunc) {
            this.body = bodyFunc.apply();
            return this;
        }

        /**
         * Sets request content-type header as application/json
         *
         * @return builder object
         */
        public Builder asContentTypeJson() {
            getHeaders().put("Content-Type", "application/json");
            return this;
        }

        /**
         * Add a key-value for path param
         *
         * @return builder object
         */
        public Builder addPathParam(String key, Object value) {
            getPathParams().put(key, String.format("%s", value));
            return this;
        }

        /**
         * Add all key-value for path param. This list size must be even.
         *
         * @return builder object
         * @throws RuntimeException if params size is not even
         */
        public Builder addPathParams(Object... params) {
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
        public Builder addPathParams(Map<String, Object> pathParams) {
            getPathParams().putAll(pathParams);
            return this;
        }

        /**
         * Add a key-value for header
         *
         * @return builder object
         */
        public Builder addHeader(String key, Object value) {
            getHeaders().put(key, String.format("%s", value));
            return this;
        }

        /**
         * Add all key-value for headers. This list size must be even.
         *
         * @return builder object
         * @throws RuntimeException if headers size is not even
         */
        public Builder addHeaders(Object... headers) {
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
        public Builder addHeaders(Map<String, Object> headers) {
            getHeaders().putAll(headers);
            return this;
        }


        /**
         * Add a key-value for query params
         *
         * @return builder object
         */
        public Builder addQueryParam(String key, Object value) {
            getQueryParam().add(key, String.format("%s", value));
            return this;
        }

        /**
         * Add all key-value for query params. This list size must be even.
         *
         * @return builder object
         * @throws RuntimeException if queryParams size is not even
         */
        public Builder addQueryParams(Object... queryParams) {
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
        public Builder addQueryParams(MultivaluedHashMap<String, Object> queryParams) {
            getQueryParam().putAll(queryParams);
            return this;
        }

        /**
         * Add all key-value for query params
         *
         * @return builder object
         */
        public Builder addQueryParams(Map<String, Object> queryParams) {
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
}