package io.github.devlibx.easy.app.dropwizard.proto;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.google.protobuf.util.JsonFormat;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Provider
@Consumes({"application/x-protobuf", "application/x-protobuf-text-format", "application/x-protobuf-json-format"})
@Produces({"application/x-protobuf", "application/x-protobuf-text-format", "application/x-protobuf-json-format"})
public class ProtocolBufferMessageBodyProvider implements MessageBodyReader<Message>, MessageBodyWriter<Message> {
    private final Map<Class<Message>, Method> methodCache = new ConcurrentHashMap();

    public ProtocolBufferMessageBodyProvider() {
    }

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Message.class.isAssignableFrom(type);
    }

    public Message readFrom(Class<Message> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        Method newBuilder = (Method) this.methodCache.computeIfAbsent(type, (t) -> {
            try {
                return t.getMethod("newBuilder");
            } catch (Exception var2) {
                return null;
            }
        });

        Message.Builder builder;
        try {
            builder = (Message.Builder) newBuilder.invoke(type);
        } catch (Exception var10) {
            throw new WebApplicationException(var10);
        }

        if (mediaType.getSubtype().contains("text-format")) {
            TextFormat.merge(new InputStreamReader(entityStream, StandardCharsets.UTF_8), builder);
            return builder.build();
        } else if (mediaType.getSubtype().contains("json-format")) {
            JsonFormat.parser().ignoringUnknownFields().merge(new InputStreamReader(entityStream, StandardCharsets.UTF_8), builder);
            return builder.build();
        } else {
            return builder.mergeFrom(entityStream).build();
        }
    }

    public long getSize(Message m, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        String formatted;
        if (mediaType.getSubtype().contains("text-format")) {
            formatted = m.toString();
            return (long) formatted.getBytes(StandardCharsets.UTF_8).length;
        } else if (mediaType.getSubtype().contains("json-format")) {
            try {
                formatted = JsonFormat.printer().omittingInsignificantWhitespace().print(m);
                return (long) formatted.getBytes(StandardCharsets.UTF_8).length;
            } catch (InvalidProtocolBufferException var7) {
                return -1L;
            }
        } else {
            return (long) m.getSerializedSize();
        }
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Message.class.isAssignableFrom(type);
    }

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
