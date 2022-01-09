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
You can see the full example code
```io/github/devlibx/easy/messaging/kafka/kafkaResilientExample.java``` and
```io/github/devlibx/easy/messaging/kafka/kafkaExample.java```
---
<br>


### How to test resilient kafka producer
To do this we will set-up a proxy using "toxiproxy". This is to generate latency between out client and 
Kafka

My IPs
```shell
My Host IP							: 192.168.0.126
My VM IP (which is running Kafka)   : 192.168.64.29

```

1. Run a Kafka in other VM -> The Kafka as following setting in server.properties file:
```shell
# Kafka listens for connection in 0.0.0.0:9092
# But client will connect to the address given by "advertised.listeners"
# Here I am telling my Kafka clinet to connect to "My Host IP on port 19092 -> where my proxy is running"
advertised.listeners=PLAINTEXT://192.168.0.126:19092
```


2. Setup proxy
```shell
# Create a Kafka proxyy
# My kafka clients will connect at "My Host IP : 19092" which will forward it to "My VM"
toxiproxy-cli create -l 192.168.0.126:19092 -u 192.168.64.29:9092 ubuntu_kafka
```

3. Run you client and use following to add or remove errors
```shell
# How to generate error
toxiproxy-cli toxic add -t latency -a latency=200  -n bad ubuntu_kafka

# How to remove error
toxiproxy-cli toxic delete  -n bad ubuntu_kafka
```

