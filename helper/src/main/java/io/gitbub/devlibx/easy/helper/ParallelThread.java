package io.gitbub.devlibx.easy.helper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("Convert2MethodRef")
@Slf4j
public class ParallelThread {
    @Getter
    private final int count;
    private final String name;

    public ParallelThread(int count, String name) {
        this.count = count;
        this.name = name;
    }

    public void execute(Runnable runnable) {
        CountDownLatch sync = new CountDownLatch(count);
        CountDownLatch waitForAllToComplete = new CountDownLatch(count);
        AtomicBoolean b = new AtomicBoolean();
        for (int i = 0; i < count; i++) {

            new Thread(() -> {

                // Wait for all threads to start
                sync.countDown();
                try {
                    sync.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                } finally {
                    synchronized (b) {
                        if (!b.get()) {
                            log.info("({}) all {} threads started to make a burst calls", name, count);
                        }
                        b.set(true);
                    }
                }

                // Do the work and also wait for all to complete
                try {
                    runnable.run();
                } finally {
                    waitForAllToComplete.countDown();
                }

            }).start();

        }

        // Wait for all to complete
        try {
            waitForAllToComplete.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }
}
