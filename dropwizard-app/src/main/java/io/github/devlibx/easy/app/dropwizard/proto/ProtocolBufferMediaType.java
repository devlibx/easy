package io.github.devlibx.easy.app.dropwizard.proto;


import jakarta.ws.rs.core.MediaType;

public class ProtocolBufferMediaType {
    /**
     * "application/x-protobuf"
     */
    public static final String APPLICATION_PROTOBUF = "application/x-protobuf";
    /**
     * "application/x-protobuf"
     */
    public static final MediaType APPLICATION_PROTOBUF_TYPE =
            new MediaType("application", "x-protobuf");

    /**
     * "application/x-protobuf-text-format"
     */
    public static final String APPLICATION_PROTOBUF_TEXT = "application/x-protobuf-text-format";
    /**
     * "application/x-protobuf-text-format"
     */
    public static final MediaType APPLICATION_PROTOBUF_TEXT_TYPE =
            new MediaType("application", "x-protobuf-text-format");

    /**
     * "application/x-protobuf-json-format"
     */
    public static final String APPLICATION_PROTOBUF_JSON = "application/x-protobuf-json-format";
    /**
     * "application/x-protobuf-json-format"
     */
    public static final MediaType APPLICATION_PROTOBUF_JSON_TYPE =
            new MediaType("application", "x-protobuf-json-format");
}
