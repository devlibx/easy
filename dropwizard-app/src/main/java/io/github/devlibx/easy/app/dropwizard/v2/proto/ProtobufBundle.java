package io.github.devlibx.easy.app.dropwizard.v2.proto;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.jersey.protobuf.InvalidProtocolBufferExceptionMapper;
import io.dropwizard.jersey.protobuf.ProtocolBufferMessageBodyProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ProtobufBundle<C extends Configuration> implements ConfiguredBundle<C> {
    static boolean preservingProtoFieldNames;

    private Class<? extends ProtocolBufferMessageBodyProvider> protocolBufferMessageBodyProviderClass;

    public ProtobufBundle() {
        preservingProtoFieldNames = true;
        protocolBufferMessageBodyProviderClass = EasyProtocolBufferMessageBodyProvider.class;
    }

    public ProtobufBundle(boolean preservingProtoFieldNames) {
        ProtobufBundle.preservingProtoFieldNames = preservingProtoFieldNames;
        protocolBufferMessageBodyProviderClass = EasyProtocolBufferMessageBodyProvider.class;
    }

    public ProtobufBundle(boolean preservingProtoFieldNames, Class<? extends ProtocolBufferMessageBodyProvider> protocolBufferMessageBodyProviderClass) {
        ProtobufBundle.preservingProtoFieldNames = preservingProtoFieldNames;
        this.protocolBufferMessageBodyProviderClass = protocolBufferMessageBodyProviderClass;
    }

    public void initialize(Bootstrap<?> bootstrap) {
    }

    public void run(C configuration, Environment environment) {
        environment.jersey().register(protocolBufferMessageBodyProviderClass);
        environment.jersey().register(InvalidProtocolBufferExceptionMapper.class);
    }
}

