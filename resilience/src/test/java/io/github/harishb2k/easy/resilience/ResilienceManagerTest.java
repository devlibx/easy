package io.github.harishb2k.easy.resilience;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.github.harishb2k.easy.resilience.IResilienceManager.ResilienceCallConfig;
import io.github.harishb2k.easy.resilience.exception.CircuitOpenException;
import io.github.harishb2k.easy.resilience.exception.OverflowException;
import io.github.harishb2k.easy.resilience.exception.RequestTimeoutException;
import io.github.harishb2k.easy.resilience.exception.ResilienceException;
import io.github.harishb2k.easy.resilience.exception.UnknownException;
import io.github.harishb2k.easy.resilience.module.ResilienceModule;
import io.reactivex.Observable;
import junit.framework.TestCase;
import org.junit.Ignore;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("ResultOfMethodCallIgnored")
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

    /**
     * Run processor N times with concurrency=N, we expect success for all calls.
     */
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

        // Execute this N times - we must get all success because we run this for N time only and
        // processor is also configured with N
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
        latch.await(5, TimeUnit.SECONDS);

        // We must have N success
        assertEquals(concurrency, results.get());
    }


    /**
     * Test request overflow
     * <pre>
     * e.g.
     *  we can only handle 10 concurrent requests
     *  we can hold extra 1 request in queue
     *      So we should be able to execute 11 requests without error
     *  we add 4 more requests
     *      So we should get 4 overflow (or reject) requests
     */
    public void testResilienceManager_Overflow_Simulation() throws Exception {
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
                    // When we get overflow or rejected errors
                    error.incrementAndGet();
                    bulkheadFullError.incrementAndGet();
                } finally {
                    allCallLatch.countDown();
                }

            }).start();
        }
        // Make sure all threads are started
        waitForAllThreadsToStartLatch.await(10, TimeUnit.SECONDS);

        // Wait for all success and request to execute + all 15 request to finish
        totalSuccessLatch.await(15, TimeUnit.SECONDS);
        allCallLatch.await(15, TimeUnit.SECONDS);

        assertEquals("We expect success for these requests", totalSuccessExpected, success.get());
        assertEquals("We expect error for these requests", extraCalls, bulkheadFullError.get());
    }

    /**
     * Run overflow test 10 times
     */
    public void testResilienceManager_Overflow_Simulation_10_times() throws Exception {
        for (int i = 0; i < 10; i++) {
            System.out.println("Iteration - " + i);
            testResilienceManager_Overflow_Simulation();
        }
    }

    /**
     * Generate a timeout from execution
     */
    public void testResilienceManager_Timeout_Simulation() throws Exception {
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

    /**
     * Generate a open circuit due to many errors
     */
    public void testResilienceManager_CircuitOpen_Simulation() {
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

    /**
     * Test a observable with success
     */
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

    /**
     * Test a observable with timeout
     */
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

    /**
     * This is when Resilience timed out
     */
    public void testResilienceManager_Observable_With_Observable_Timeout__ResilienceTimedOut() throws InterruptedException {
        int concurrency = 3;
        String uuid = UUID.randomUUID().toString();
        IResilienceProcessor processor = resilienceManager.getOrCreate(
                ResilienceCallConfig.withDefaults()
                        .concurrency(concurrency)
                        .id(uuid)
                        .timeout(100)
                        .build()
        );

        Observable<String> observable = Observable.create(observableEmitter -> {
            Thread.sleep(1000);
            observableEmitter.onNext("Done");
            observableEmitter.onComplete();
        });

        CountDownLatch waitForSuccessOrError = new CountDownLatch(1);
        final AtomicBoolean gotException = new AtomicBoolean(false);
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        processor.executeAsObservable(uuid, observable, String.class)
                .subscribe(s -> {
                            waitForSuccessOrError.countDown();
                        },
                        throwable -> {
                            gotException.set(true);
                            exception.set(throwable);
                            waitForSuccessOrError.countDown();
                        });
        waitForSuccessOrError.await(5, TimeUnit.SECONDS);
        assertTrue(gotException.get());
        assertEquals(RequestTimeoutException.class, exception.get().getClass());
        assertTrue(ResilienceException.class.isAssignableFrom(exception.get().getClass()));
    }

    /**
     * This is when Resilience timed out
     */
    public void testResilienceManager_Observable_With_Observable_Timeout__ObservableTimedOut() throws InterruptedException {
        int concurrency = 3;
        String uuid = UUID.randomUUID().toString();
        IResilienceProcessor processor = resilienceManager.getOrCreate(
                ResilienceCallConfig.withDefaults()
                        .concurrency(concurrency)
                        .id(uuid)
                        .timeout(1000)
                        .build()
        );

        Observable<String> observable = Observable.create(observableEmitter -> {
            Thread.sleep(100);
            throw new TimeoutException();
        });

        CountDownLatch waitForSuccessOrError = new CountDownLatch(1);
        final AtomicBoolean gotException = new AtomicBoolean(false);
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        processor.executeAsObservable(uuid, observable, String.class)
                .subscribe(s -> {
                            waitForSuccessOrError.countDown();
                        },
                        throwable -> {
                            gotException.set(true);
                            exception.set(throwable);
                            waitForSuccessOrError.countDown();
                        });
        waitForSuccessOrError.await(5, TimeUnit.SECONDS);
        assertTrue(gotException.get());
        assertEquals(RequestTimeoutException.class, exception.get().getClass());
        assertTrue(ResilienceException.class.isAssignableFrom(exception.get().getClass()));
    }


    /**
     * This is when Resilience timed out
     */
    @Ignore
    public void testResilienceManager_Observable_With_Observable_CircuitOpen_Simulation() throws InterruptedException {
        int concurrency = 3;
        String uuid = UUID.randomUUID().toString();
        IResilienceProcessor processor = resilienceManager.getOrCreate(
                ResilienceCallConfig.withDefaults()
                        .concurrency(concurrency)
                        .id(uuid)
                        .timeout(1000)
                        .build()
        );

        Observable<String> observable = Observable.create(observableEmitter -> {
            Thread.sleep(1);
            throw new CustomException();
        });

        int count = 1000;
        CountDownLatch waitForSuccessOrError = new CountDownLatch(count);
        final AtomicBoolean gotCircuitOpenException = new AtomicBoolean(false);
        for (int i = 0; i < count; i++) {
            processor.executeAsObservable(uuid, observable, String.class)
                    .subscribe(s -> {
                                waitForSuccessOrError.countDown();
                            },
                            throwable -> {
                                System.out.println(throwable);
                                if (throwable instanceof CircuitOpenException) {
                                    gotCircuitOpenException.set(true);
                                }
                                waitForSuccessOrError.countDown();
                            });
        }
        waitForSuccessOrError.await(5, TimeUnit.SECONDS);
        // assertTrue(gotCircuitOpenException.get());
    }

    private static class CustomException extends RuntimeException {
    }
}