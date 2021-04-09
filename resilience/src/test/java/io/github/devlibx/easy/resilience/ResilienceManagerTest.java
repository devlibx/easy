package io.github.devlibx.easy.resilience;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.CommonBaseTestCase;
import io.github.devlibx.easy.resilience.IResilienceManager.ResilienceCallConfig;
import io.github.devlibx.easy.resilience.exception.CircuitOpenException;
import io.github.devlibx.easy.resilience.exception.OverflowException;
import io.github.devlibx.easy.resilience.exception.RequestTimeoutException;
import io.github.devlibx.easy.resilience.exception.ResilienceException;
import io.github.devlibx.easy.resilience.exception.UnknownException;
import io.github.devlibx.easy.resilience.module.ResilienceModule;
import io.reactivex.rxjava3.core.Observable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ResilienceManagerTest extends CommonBaseTestCase {
    private IResilienceManager resilienceManager;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Injector injector = Guice.createInjector(new ResilienceModule());
        ApplicationContext.setInjector(injector);
        resilienceManager = injector.getInstance(IResilienceManager.class);
    }

    /**
     * Run processor N times with concurrency=N, we expect success for all calls.
     */
    @Test
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
    @Test
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

        assertEquals(totalSuccessExpected, success.get(), "We expect success for these requests");
        assertEquals(extraCalls, bulkheadFullError.get(), "We expect error for these requests");
    }

    /**
     * Run overflow test 10 times
     */
    @Test
    public void testResilienceManager_Overflow_Simulation_10_times() throws Exception {
        for (int i = 0; i < 3; i++) {
            System.out.println("Iteration - " + i);
            testResilienceManager_Overflow_Simulation();
        }
    }

    /**
     * Generate a timeout from execution
     */
    @Test
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
    @Test
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
                    fail("we did not expected this error");
                }

            }
        }
        assertTrue(gotCircuitOpenException);
    }

    /**
     * Test a observable with success
     */
    @Test
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
            processor.executeObservable(uuid, System::currentTimeMillis, Long.class)
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
    @Test
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
        processor.executeObservable(uuid, callable, String.class)
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
    @Test
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
        processor.executeObservable(uuid, observable, String.class)
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
    @Test
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
        processor.executeObservable(uuid, observable, String.class)
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
    @Test
    public void testResilienceManager_Observable_With_Observable_CircuitOpen_Simulation() throws InterruptedException {
        int concurrency = 3;
        String uuid = UUID.randomUUID().toString();
        IResilienceProcessor processor = resilienceManager.getOrCreate(
                ResilienceCallConfig.withDefaults()
                        .concurrency(concurrency)
                        .concurrency(100000)
                        .id(uuid)
                        .timeout(1000)
                        .build()
        );

        Observable<String> observable = Observable.create(observableEmitter -> {
            // Thread.sleep(1);
            Thread.sleep(1);
            throw new CustomException();
        });

        int count = 200;
        CountDownLatch waitForSuccessOrError = new CountDownLatch(count);
        final AtomicBoolean gotCircuitOpenException = new AtomicBoolean(false);
        for (int i = 0; i < count; i++) {
            processor.executeObservable(uuid, observable, String.class)
                    .subscribe(s -> {
                                waitForSuccessOrError.countDown();
                            },
                            throwable -> {
                                if (throwable instanceof CircuitOpenException || throwable.getCause() instanceof CircuitOpenException) {
                                    gotCircuitOpenException.set(true);
                                }
                                waitForSuccessOrError.countDown();
                            });
        }
        waitForSuccessOrError.await(20, TimeUnit.SECONDS);
        assertTrue(gotCircuitOpenException.get(), "Expected a CircuitOpenException");

        boolean gotSuccess = false;
        Thread.sleep(2);
        for (int i = 0; i < 200; i++) {
            try {
                Long result = processor.execute(uuid, () -> {
                    Thread.sleep(100);
                    return 111L;
                }, Long.class);
                gotSuccess = true;
                assertEquals(111L, result.longValue());
                break;
            } catch (Exception ignored) {
            }
            Thread.sleep(1);
        }
        assertTrue(gotSuccess);

    }

    public static class CustomException extends RuntimeException {
    }
}