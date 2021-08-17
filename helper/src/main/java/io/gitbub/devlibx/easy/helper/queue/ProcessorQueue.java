package io.gitbub.devlibx.easy.helper.queue;


import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("ALL")
@Slf4j
public class ProcessorQueue<T> {
    private final int threadCount;
    private final BlockingQueue<T> queue;
    private final ExecutorService executorService;
    private final IProcessor<T> processor;
    private final AtomicBoolean STOP_PROCESSOR;
    private final int maxTimeToWaitForAItemToProcessInSec;
    private final CountDownLatch waitLatch;
    private final int maxRetryPerItem;
    private final int waitTimeToRetry;
    private final IRateLimiter rateLimiter;
    private final AtomicBoolean clientInitiatedAllEeventsArePosted = new AtomicBoolean(false);

    public ProcessorQueue(int threadCount,
                          int queueBufferSize,
                          int maxTimeToWaitForAItemToProcessInSec,
                          int maxRetryPerItem,
                          IRateLimiter.Config rateLimiterConfig,
                          IProcessor<T> processor
    ) {
        this.maxRetryPerItem = maxRetryPerItem <= 0 ? Integer.MAX_VALUE : maxRetryPerItem;
        this.waitTimeToRetry = maxTimeToWaitForAItemToProcessInSec <= 0 ? 1 : maxTimeToWaitForAItemToProcessInSec;
        this.STOP_PROCESSOR = new AtomicBoolean(false);
        this.waitLatch = new CountDownLatch(threadCount);
        this.processor = processor;
        this.threadCount = threadCount;
        this.queue = new ArrayBlockingQueue<>(queueBufferSize);
        this.maxTimeToWaitForAItemToProcessInSec = maxTimeToWaitForAItemToProcessInSec <= 0 ? 10 : maxTimeToWaitForAItemToProcessInSec;
        this.executorService = Executors.newFixedThreadPool(threadCount);
        if (rateLimiterConfig.limit > 0) {
            this.rateLimiter = new DefaultRateLimiter(rateLimiterConfig);
        } else {
            this.rateLimiter = new NoOpRateLimiter();
        }
    }

    public ProcessorQueue(int threadCount,
                          int queueBufferSize,
                          int maxTimeToWaitForAItemToProcessInSec,
                          int maxRetryPerItem,
                          int rateLimit,
                          IProcessor<T> processor
    ) {
        this(threadCount, queueBufferSize, maxTimeToWaitForAItemToProcessInSec, maxRetryPerItem, IRateLimiter.Config.builder()
                .limit(rateLimit)
                .build(), processor);
    }

    public void noMoreItemsToProcess() {
        clientInitiatedAllEeventsArePosted.set(true);
    }

    public CountDownLatch start() {
        for (int i = 0; i < threadCount; i++) {
            this.executorService.submit(runnable(i));
        }
        new Thread(() -> {
            try {
                waitLatch.await();
            } catch (InterruptedException e) {
            }
            executorService.shutdown();
        }).start();
        return waitLatch;
    }

    public void processItem(T item) {
        while (true) {
            try {
                rateLimiter.execute(() -> {
                    _processItem(item);
                });
                break;
            } catch (RequestNotPermitted e) {
                try {
                    System.out.println("rate limit error - will repost it");
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void _processItem(T item) {
        try {
            queue.put(item);
        } catch (InterruptedException e) {
            log.error("Failed to put items to queue for processing", e);
        }
    }

    private Runnable runnable(int threadId) {
        return () -> {
            while (!STOP_PROCESSOR.get()) {
                try {
                    // Get items to process from thread
                    T item = queue.poll(maxTimeToWaitForAItemToProcessInSec, TimeUnit.SECONDS);
                    if (item == null && clientInitiatedAllEeventsArePosted.get()) {
                        log.info("Worker Id=" + threadId + " - Stop worker thread - did not get items for " + maxTimeToWaitForAItemToProcessInSec + " sec");
                        break;
                    } else if (item == null) {
                        Thread.sleep(10);
                        log.debug("Worker Id=" + threadId + " did not find any work... Continue in loop and waiting for work...");
                        continue;
                    }

                    // Process the items
                    int temp = maxRetryPerItem;
                    while (temp > 0) {
                        try {
                            processor.process(item);
                            break;
                        } catch (Throwable e) {
                            Thread.sleep(waitTimeToRetry);
                            log.info("Worker Id=" + threadId + " - Got error in items: " + item + " Retry=" + temp + " Wait for " + waitTimeToRetry + "ms");
                        }
                        temp--;
                    }

                } catch (Throwable e) {
                    log.error("Worker Id=" + threadId + " - Got error: " + e.getMessage());
                }
            }

            // Worker is done - reduce latch
            log.info("Worker Id=" + threadId + " - Stopping the worker thread...");
            waitLatch.countDown();
        };
    }
}
