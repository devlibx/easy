package io.github.devlibx.easy.metrics.statsd;

import com.timgroup.statsd.StatsDClientErrorHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public final class NonBlockingUdpSender {
    private final Charset encoding;
    private final DatagramChannel clientSocket;
    private final StatsDClientErrorHandler handler;
    private final LinkedBlockingQueue<byte[]> buffer;
    private final int bufferSize;
    private final AtomicBoolean keepRunning;
    private final CountDownLatch awaitTermination;
    private final int threadCount;

    public NonBlockingUdpSender(String hostname, int port, Charset encoding, StatsDClientErrorHandler handler, int bufferSize) throws IOException {
        this.encoding = encoding;
        this.handler = handler;
        this.clientSocket = DatagramChannel.open();
        this.clientSocket.connect(new InetSocketAddress(hostname, port));
        this.bufferSize = bufferSize <= 0 ? 1 : bufferSize;
        this.buffer = new LinkedBlockingQueue<>(bufferSize);
        this.keepRunning = new AtomicBoolean(true);
        this.threadCount = 5;
        this.awaitTermination = new CountDownLatch(threadCount);
        setupSendingThread();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void stop() {
        try {
            keepRunning.set(false);
            awaitTermination.await(30, TimeUnit.SECONDS);
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
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                while (keepRunning.get()) {
                    try {
                        final byte[] sendData = buffer.take();
                        clientSocket.write(ByteBuffer.wrap(sendData));
                    } catch (Exception e) {
                        handler.handle(e);
                    }
                }
                awaitTermination.countDown();
            }).start();
        }
    }
}