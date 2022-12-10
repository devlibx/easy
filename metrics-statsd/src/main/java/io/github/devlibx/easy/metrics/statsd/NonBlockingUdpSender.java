package io.github.devlibx.easy.metrics.statsd;

import com.timgroup.statsd.StatsDClientErrorHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class NonBlockingUdpSender {
    private final Charset encoding;
    private final DatagramChannel clientSocket;
    private final ExecutorService executor;
    private StatsDClientErrorHandler handler;
    private final LinkedBlockingQueue<byte[]> buffer;
    private int bufferSize;

    public NonBlockingUdpSender(String hostname, int port, Charset encoding, StatsDClientErrorHandler handler, int bufferSize) throws IOException {
        this.encoding = encoding;
        this.handler = handler;
        this.clientSocket = DatagramChannel.open();
        this.clientSocket.connect(new InetSocketAddress(hostname, port));
        this.executor = Executors.newFixedThreadPool(5);
        this.bufferSize = bufferSize <= 0 ? 1 : bufferSize;
        this.buffer = new LinkedBlockingQueue<>(bufferSize);
        setupSendingThread();
    }

    public void stop() {
        try {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            handler.handle(e);
        } finally {
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (Exception e) {
                    handler.handle(e);
                }
            }
        }
    }

    public void send(final String message) {
        try {
            final byte[] sendData = message.getBytes(encoding);
            if (!buffer.offer(sendData)) {
                log.warn("dropping statsd messages, buffer of size " + bufferSize + " is full");
            }
        } catch (Exception e) {
            handler.handle(e);
        }
    }

    private void setupSendingThread() {
        new Thread(() -> {
            while (true) {
                try {
                    final byte[] sendData = buffer.take();
                    executor.submit(() -> {
                        try {
                            clientSocket.write(ByteBuffer.wrap(sendData));
                        } catch (Exception e) {
                            handler.handle(e);
                        }
                    });
                } catch (Exception e) {
                    handler.handle(e);
                }
            }
        }).start();
    }
}