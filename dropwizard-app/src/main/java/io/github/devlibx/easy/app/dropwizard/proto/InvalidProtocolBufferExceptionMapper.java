package io.github.devlibx.easy.app.dropwizard.proto;

import com.google.protobuf.InvalidProtocolBufferException;
import io.dropwizard.jersey.protobuf.protos.DropwizardProtos;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvalidProtocolBufferExceptionMapper implements ExceptionMapper<InvalidProtocolBufferException> {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(io.dropwizard.jersey.protobuf.InvalidProtocolBufferExceptionMapper.class);

    @Override
    public Response toResponse(InvalidProtocolBufferException exception) {
        final DropwizardProtos.ErrorMessage message =
                DropwizardProtos.ErrorMessage.newBuilder()
                        .setMessage("Unable to process protocol buffer")
                        .setCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .build();

        LOGGER.debug("Unable to process protocol buffer message", exception);
        return Response.status(Response.Status.BAD_REQUEST)
                .type(ProtocolBufferMediaType.APPLICATION_PROTOBUF_TYPE)
                .entity(message)
                .build();
    }
}
