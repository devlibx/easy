package io.github.devlibx.easy.testing.kafka;

public interface IKafkaExtensionControl  {
    void stopIfRunning();
    boolean isKafkaRunning();
}
