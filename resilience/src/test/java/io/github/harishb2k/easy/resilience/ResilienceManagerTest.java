package io.github.harishb2k.easy.resilience;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.github.harishb2k.easy.resilience.IResilienceManager.CircuitOpenException;
import io.github.harishb2k.easy.resilience.IResilienceManager.IResilienceProcessor;
import io.github.harishb2k.easy.resilience.IResilienceManager.OverflowException;
import io.github.harishb2k.easy.resilience.IResilienceManager.RequestTimeoutException;
import io.github.harishb2k.easy.resilience.IResilienceManager.ResilienceCallConfig;
import io.github.harishb2k.easy.resilience.IResilienceManager.ResilienceException;
import io.github.harishb2k.easy.resilience.IResilienceManager.UnknownException;
import io.github.harishb2k.easy.resilience.module.ResilienceModule;
import junit.framework.TestCase;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ResilienceManagerTest extends TestCase {
    private Injector injector;
    private IResilienceManager resilienceManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        injector = Guice.createInjector(new ResilienceModule());
        ApplicationContext.setInjector(injector);
        resilienceManager = injector.getInstance(IResilienceManager.class);
    }

    public void testResilienceManager() throws Exception {
        int concurrency = 3;
        String uuid = UUID.randomUUID().toString();
        IResilienceProcessor processor = resilienceManager.getOrCreate(
                ResilienceCallConfig.withDefaults()
                        .concurrency(concurrency)
                        .id(uuid)
                        .timeout(100)
                        .build()
        );

        AtomicInteger results = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(concurrency);
        for (int i = 0; i < concurrency; i++) {
            try {
                processor.execute(uuid, System::currentTimeMillis, Long.class);
                results.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                latch.countDown();
            }
        }
        latch.await(1, TimeUnit.SECONDS);
        assertEquals(concurrency, results.get());
    }

    public void testResilienceManager_WithErrors_10_times() throws Exception {
        for (int i = 0; i < 10; i++) {
            System.out.println("Iteration - " + i);
            testResilienceManager_WithErrors();
        }
    }

    public void testResilienceManager_WithErrors() throws Exception {
        int concurrency = 10;
        int queueSize = 1;
        int extraCalls = 4;
        int totalSuccessExpected = concurrency + queueSize;
        int totalCallsToMake = totalSuccessExpected + extraCalls;

        String uuid = UUID.randomUUID().toString();
        IResilienceProcessor processor = resilienceManager.getOrCreate(
                ResilienceCallConfig.withDefaults()
                        .id(uuid)
                        .concurrency(concurrency)
                        .queueSize(queueSize)
                        .build()
        );

        AtomicInteger success = new AtomicInteger();
        AtomicInteger error = new AtomicInteger();
        AtomicInteger bulkheadFullError = new AtomicInteger();
        CountDownLatch totalSuccessLatch = new CountDownLatch(totalSuccessExpected);
        CountDownLatch waitForAllThreadsToStartLatch = new CountDownLatch(totalCallsToMake);
        CountDownLatch allCallLatch = new CountDownLatch(totalCallsToMake);
        for (int i = 0; i < totalCallsToMake; i++) {
            new Thread(() -> {
                waitForAllThreadsToStartLatch.countDown();

                Callable<String> callable = () -> {
                    Thread.sleep(100);
                    return UUID.randomUUID().toString();
                };

                try {
                    processor.execute(uuid, callable, String.class);
                    totalSuccessLatch.countDown();
                    success.incrementAndGet();
                } catch (OverflowException e) {
                    error.incrementAndGet();
                    bulkheadFullError.incrementAndGet();
                } finally {
                    allCallLatch.countDown();
                }

            }).start();
        }
        waitForAllThreadsToStartLatch.await(10, TimeUnit.SECONDS);
        totalSuccessLatch.await(15, TimeUnit.SECONDS);
        allCallLatch.await(15, TimeUnit.SECONDS);

        assertEquals("We expect success for these requests", totalSuccessExpected, success.get());
        assertEquals("We expect error for these requests", extraCalls, bulkheadFullError.get());
    }

    public void testResilienceManager_Timeout_Error() throws Exception {
        int concurrency = 3;
        String uuid = UUID.randomUUID().toString();
        IResilienceProcessor processor = resilienceManager.getOrCreate(
                ResilienceCallConfig.withDefaults()
                        .concurrency(concurrency)
                        .id(uuid)
                        .timeout(100)
                        .build()
        );

        Callable<String> callable = () -> {
            Thread.sleep(1000);
            return UUID.randomUUID().toString();
        };

        boolean gotException = false;
        try {
            processor.execute(uuid, callable, String.class);
        } catch (RequestTimeoutException e) {
            gotException = true;
        }
        assertTrue(gotException);
    }

    public void testResilienceManager_CircuitOpen_Error() throws Exception {
        int concurrency = 3;
        String uuid = UUID.randomUUID().toString();
        IResilienceProcessor processor = resilienceManager.getOrCreate(
                ResilienceCallConfig.withDefaults()
                        .concurrency(concurrency)
                        .id(uuid)
                        .timeout(100)
                        .queueSize(1000)
                        .build()
        );

        Callable<String> callable = () -> {
            throw new CustomException();
        };

        boolean gotCircuitOpenException = false;
        for (int i = 0; i < 1000; i++) {
            try {
                processor.execute(uuid, callable, String.class);
            } catch (CircuitOpenException e) {
                gotCircuitOpenException = true;
            } catch (UnknownException e) {
                if (e.getCause() instanceof CustomException) {
                    // This is expected - we threw this error to open circuit
                } else {
                    System.out.println(e);
                    fail("we did not expected this error");
                }

            }
        }
        assertTrue(gotCircuitOpenException);
    }

    public void testResilienceManager_Observable() throws Exception {
        int concurrency = 3;
        String uuid = UUID.randomUUID().toString();
        IResilienceProcessor processor = resilienceManager.getOrCreate(
                ResilienceCallConfig.withDefaults()
                        .concurrency(concurrency)
                        .id(uuid)
                        .timeout(100)
                        .build()
        );

        AtomicInteger results = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(concurrency);
        for (int i = 0; i < concurrency; i++) {
            processor.executeAsObservable(uuid, System::currentTimeMillis, Long.class)
                    .subscribe(aLong -> {
                        long temp = aLong;
                        results.incrementAndGet();
                    }, throwable -> {
                        fail("Did not expected any error");
                    });
        }
        latch.await(1, TimeUnit.SECONDS);
        assertEquals(concurrency, results.get());
    }

    public void testResilienceManager_Observable_Timeout_Error() throws Exception {
        int concurrency = 3;
        String uuid = UUID.randomUUID().toString();
        IResilienceProcessor processor = resilienceManager.getOrCreate(
                ResilienceCallConfig.withDefaults()
                        .concurrency(concurrency)
                        .id(uuid)
                        .timeout(100)
                        .build()
        );

        Callable<String> callable = () -> {
            Thread.sleep(1000);
            return UUID.randomUUID().toString();
        };

        final AtomicBoolean gotException = new AtomicBoolean(false);
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        processor.executeAsObservable(uuid, callable, String.class)
                .subscribe(s -> {
                        },
                        throwable -> {
                            gotException.set(true);
                            exception.set(throwable);
                        });
        assertTrue(gotException.get());
        assertEquals(RequestTimeoutException.class, exception.get().getClass());
        assertTrue(ResilienceException.class.isAssignableFrom(exception.get().getClass()));
    }

    private static class CustomException extends RuntimeException {
    }
}