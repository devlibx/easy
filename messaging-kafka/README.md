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
We have defined few message producer and consumer:
customer - normal message producer
customerNoErrorIfMessageSendFail - if message send fails then we open circuit, and we do not block once circuit is open.
<br>
    use ```enableCircuitBreakerOnError=true``` to enable this

```yaml
messaging:
  producers:    
    customer:
      topic: customer
      brokers: localhost:9092
      sync: false
      retries: 0
      acks: 0
      value.serializer: org.apache.kafka.common.serialization.BytesSerializer
    customerNoErrorIfMessageSendFail:
      topic: customer
      brokers: localhost:9092
      sync: false
      retries: 0
      acks: 0
      value.serializer: org.apache.kafka.common.serialization.BytesSerializer
      enableCircuitBreakerOnError: true
  consumers:   
    customer:
      topic: customer
      broker: localhost:9092
      sync: true
      group.id: 1234
```
---
<br>

### Example Code
You can see the full example code
```io/github/devlibx/easy/messaging/kafka/kafkaResilientExample.java``` and
```io/github/devlibx/easy/messaging/kafka/kafkaExample.java```
