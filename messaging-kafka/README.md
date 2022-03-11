## Kafka helper setup
This is the setup code to get ```IMessagingFactory``` object. You can manually set-up if you like.
```java
// Setup messaging factory - recommended using this code, as it will setup defaults.
// You can create objects by yourself if you want
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.gitbub.devlibx.easy.helper.yaml.YamlUtils;
import io.github.devlibx.easy.messaging.config.MessagingConfigs;
import io.github.devlibx.easy.messaging.kafka.module.MessagingKafkaModule;
import io.github.devlibx.easy.messaging.module.MessagingModule;
import io.github.devlibx.easy.messaging.service.IMessagingFactory;

----
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public static class KafkaMessagingTestConfig {
    public MessagingConfigs messaging;
}
----

// Read config from file        
KafkaMessagingTestConfig kafkaConfig = YamlUtils.readYaml("kafka_test_config.yml", KafkaMessagingTestConfig.class);

Injector injector = Guice.createInjector(new AbstractModule() {
    @Override
    protected void configure() {
        bind(MessagingConfigs.class).toInstance(kafkaConfig.messaging);
    }
}, new MessagingKafkaModule(), new MessagingModule());

// Get messaging factory and initialize the factory
IMessagingFactory messagingFactory = injector.getInstance(IMessagingFactory.class);
messagingFactory.initialize();
```

### Producer
```java
// Produce sample data
messagingFactory.getProducer("customer").ifPresent(producer -> {
    for (int i = 0; i < 100; i++) {

        // Produce event to Kafka
        Object toSend = StringObjectMap.of("input", UUID.randomUUID().toString());
        producer.send(UUID.randomUUID().toString(), JsonUtils.asJson(toSend).getBytes());
    }
});
```

### Consumer
```java
// Get and start consuming data
messagingFactory.getConsumer("customer").ifPresent(consumer -> {
    consumer.start((message, metadata) -> {
        // Consumer event
        System.out.println(message);
    });
});
```
---
<br>

### Messaging config 

**Example consumer** -> customer - normal message producer <br>
**Example producer** -> customerNoErrorIfMessageSendFail - if message send fails then we open circuit, and we do not block once circuit is open.
**use ```enableCircuitBreakerOnError=true``` to enable circuit breaker**

**sync** = true | false (send message in sync or async mode) <br>
**request.timeout.ms** = timeout of not able to send message (in case of sync=false you will not block, but you should still define
a timeout for your kafka client) <br>
**ack** = (0 | 1 | all) -> if "ack=1" then circuit-breaker settings will not have any effect <br>
**enableCircuitBreakerOnError** = true | false (default=false) - open circuit to avoid calls to Kafka if there are errors <br>

```yaml
messaging:
  producers:    
    customer:
      topic: customer
      brokers: localhost:9092
      sync: false
      retries: 0
      acks: 1
      request.timeout.ms: 100
      value.serializer: org.apache.kafka.common.serialization.BytesSerializer
    customerNoErrorIfMessageSendFail:
      topic: customer
      brokers: localhost:9092
      sync: false
      retries: 0
      acks: 1
      request.timeout.ms: 100
      value.serializer: org.apache.kafka.common.serialization.BytesSerializer
      enableCircuitBreakerOnError: true
  consumers:   
    customer:
      topic: customer
      brokers: localhost:9092
      sync: true
      group.id: 1234
```
---
<br>

### Example Code
You can see the full example code (under messaging-kafka/src/test)
```io/github/devlibx/easy/messaging/kafka/kafkaResilientExample.java``` and
```io/github/devlibx/easy/messaging/kafka/kafkaExample.java```
---
<br>

### Spring Boot Integration
```java

application.properties:
# Messaging to send events to update user event
messaging.producers.updateUserEvent.enabled: true
messaging.producers.updateUserEvent.topic: topic_123
messaging.producers.updateUserEvent.brokers: localhost:9092
messaging.producers.updateUserEvent.sync: true
messaging.producers.updateUserEvent.enableCircuitBreakerOnError: true
messaging.producers.updateUserEvent.request.timeout.ms: 100
messaging.producers.updateUserEvent.ack: 1
messaging.producers.updateUserEvent.circuit-breaker.stay_in_open_state_on_error.ms: 10000

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Configuration
@ConfigurationProperties(prefix = "messaging")
public class MessagingConfigs extends io.github.devlibx.easy.messaging.config.MessagingConfigs {
}


@Configuration
public static class MessagingConfigurationBeanFactory {

    @Bean
    public StringHelper stringHelper() {
        return new StringHelper();
    }

    @Bean
    public IMetrics metrics() {
        return new IMetrics.NoOpMetrics();
    }

    @Bean
    @Autowired
    public IMessagingFactory messagingFactory(MessagingConfigs messagingConfigs, StringHelper stringHelper, IMetrics metrics) {
        Map<String, IProducerService> producerServiceMap = new HashMap<>();
        producerServiceMap.put("KAFKA", new KafkaBasedProducerService(stringHelper, metrics));
        Map<String, IConsumerService> consumerServiceMap = new HashMap<>();
        consumerServiceMap.put("KAFKA", new KafkaBasedConsumerService(metrics));
        IMessagingFactory messagingFactory = new MessageFactory(producerServiceMap, consumerServiceMap, messagingConfigs);
        messagingFactory.initialize();
        return messagingFactory;
    }
}

Usage:
======
messagingFactory.getProducer("updateUserEvent")
        .ifPresent(producer -> producer.send(key, JsonUtils.asJson(logEvent)));
```

## How to test resilient kafka producer 
This implementation was tested using ```toxiproxy``` to generate errors between client and kafka <br>

To do this we will set-up a proxy using ```toxiproxy```. This is to generate latency between out client and 
Kafka

*My IPs for this example*
```shell
My Host IP							: 192.168.0.126
My VM IP (which is running Kafka)   : 192.168.64.29

```

1. Run a Kafka in other VM <br>You will have to change following setting in server.properties file before 
you run Kafka (to proxy it for this setup).
```shell
advertised.listeners=PLAINTEXT://192.168.0.126:19092
```

2. Setup proxy
```shell
# Create a Kafka proxy
# My kafka clients (example code) will connect at "My Host IP : 19092", and proxy will forward it to "My VM"
toxiproxy-cli create -l 192.168.0.126:19092 -u 192.168.64.29:9092 ubuntu_kafka
```

3. Run you client and use following to add/remove errors
```shell
# When your app is running then you can inject latency of 200 ms by following
toxiproxy-cli toxic add -t latency -a latency=200  -n inject_latency_in_my_kafka_calls ubuntu_kafka

# To remove the latency
toxiproxy-cli toxic delete  -n inject_latency_in_my_kafka_calls ubuntu_kafka
```

