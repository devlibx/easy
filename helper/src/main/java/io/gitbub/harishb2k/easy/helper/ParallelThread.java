package io.gitbub.harishb2k.easy.helper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("Convert2MethodRef")
public class ParallelThread {
    private final int count;

    public ParallelThread(int count) {
        this.count = count;
    }

    public void execute(Runnable runnable) {
        CountDownLatch sync = new CountDownLatch(count);
        CountDownLatch waitForAllToComplete = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {

            new Thread(() -> {

                // Wait for all threads to start
                sync.countDown();
                try {
                    sync.await(10, TimeUnit.SECONDS);
                    System.out.println("All threads started...");
                } catch (InterruptedException ignored) {
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
