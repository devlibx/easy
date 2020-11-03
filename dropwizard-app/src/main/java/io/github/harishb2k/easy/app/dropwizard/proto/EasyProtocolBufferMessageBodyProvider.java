package io.github.harishb2k.easy.app.dropwizard.proto;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.dropwizard.jersey.protobuf.ProtocolBufferMessageBodyProvider;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class EasyProtocolBufferMessageBodyProvider extends ProtocolBufferMessageBodyProvider {

    public void writeTo(Message m, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
        if (mediaType.getSubtype().contains("text-format")) {
            entityStream.write(m.toString().getBytes(StandardCharsets.UTF_8));
        } else if (mediaType.getSubtype().contains("json-format")) {
            String formatted = JsonFormat.printer().preservingProtoFieldNames().omittingInsignificantWhitespace().print(m);
            entityStream.write(formatted.getBytes(StandardCharsets.UTF_8));
        } else {
            m.writeTo(entityStream);
        }
    }
}

