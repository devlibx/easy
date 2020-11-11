package io.github.harishb2k.easy.messaging.consumer;

public interface IConsumer {

    /**
     * Start producer
     */
    void start(IMessageConsumer messageConsumer);

    /**
     * Shutdown
     */
    void shutdown();

    interface IMessageConsumer {
        void process(Object message, Object metadata);
    }
}
