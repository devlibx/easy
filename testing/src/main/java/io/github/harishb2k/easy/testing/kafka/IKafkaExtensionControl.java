package io.github.harishb2k.easy.testing.kafka;

public interface IKafkaExtensionControl {
    void stopIfRunning();
    boolean isKafkaRunning();
}
