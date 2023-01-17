package io.github.devlibx.easy.app.dropwizard.proto;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jersey.protobuf.InvalidProtocolBufferExceptionMapper;
import io.dropwizard.jersey.protobuf.ProtocolBufferMessageBodyProvider;


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

