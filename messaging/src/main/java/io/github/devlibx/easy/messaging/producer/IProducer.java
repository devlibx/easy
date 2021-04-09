package io.github.devlibx.easy.messaging.producer;

public interface IProducer {
    /**
     * Start producer
     */
    void start();

    /**
     * Send message
     *
     * @param key   key for message
     * @param value value to send
     * @return true if message send is successful
     */
    boolean send(String key, Object value);

    /**
     * Shutdown
     */
    void shutdown();
}
