package io.gitbub.devlibx.easy.helper.queue;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ProcessorQueueTest {
    @Test
    public void testRateLimit() throws InterruptedException {
        ProcessorQueue<String> queue = new ProcessorQueue<String>(
                1,
                100,
                2,
                0,
                10,
                in -> {
                    try {
                        Thread.sleep(100);
                        System.out.println(in);
                    } catch (InterruptedException ignored) {
                    }
                }
        );
        CountDownLatch latch = queue.start();
        for (int i = 0; i < 15; i++) {
            queue.processItem("item_" + i);
        }
        queue.noMoreItemsToProcess();
        boolean result = latch.await(10, TimeUnit.SECONDS);
        Assertions.assertTrue(result);
    }


    @Test
    public void testRateLimit_WeShouldSeeRateLimitError() throws InterruptedException {
        ProcessorQueue<String> queue = new ProcessorQueue<String>(
                10, // 10 threads as workers
                100, // Queue buffer
                2, // Wait for 2 sec - if we don't have enough items in 2 sec then exit
                0, // No limit to retries

                // Setup rate limit - 100 requests to process per second
                IRateLimiter.Config.builder().limit(100).build(),
                // NOTE - pass limit=0 if you don't want any rate-limiting

                // This is client function
                in -> {
                    try {
                        System.out.println(in);
                    } catch (Exception ignored) {
                    }
                }
        );

        // Mandatory - Start the work
        CountDownLatch latch = queue.start();

        // Client code which will generate work to process
        for (int i = 0; i < 20; i++) {
            queue.processItem("item_" + i);
        }

        // Mandatory - Client must specify that he is done with sending items
        queue.noMoreItemsToProcess();

        // Mandatory - You must wait for latch
        // NOTE - this is test so we are waiting for 10 sec, for your case you can wait without any timeout
        boolean result = latch.await(10, TimeUnit.SECONDS);
        Assertions.assertTrue(result);
    }
}