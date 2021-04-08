package io.github.devlibx.easy.resilience;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.CommonBaseTestCase;
import io.gitbub.devlibx.easy.helper.ParallelThread;
import io.github.devlibx.easy.resilience.ResilienceManagerTest.CustomException;
import io.github.devlibx.easy.resilience.exception.CircuitOpenException;
import io.github.devlibx.easy.resilience.exception.OverflowException;
import io.github.devlibx.easy.resilience.exception.RequestTimeoutException;
import io.github.devlibx.easy.resilience.module.ResilienceModule;
import io.reactivex.rxjava3.core.Observable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ResilienceManagerObservableTest extends CommonBaseTestCase {
    private IResilienceManager resilienceManager;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Injector injector = Guice.createInjector(new ResilienceModule());
        ApplicationContext.setInjector(injector);
        resilienceManager = injector.getInstance(IResilienceManager.class);
    }

    /**
     * Test we get a proper type of exception is thrown when circuit is open
     */
    @Test
    @DisplayName("CircuitOpenException will be thrown if CircuitBreaker is open")
    public void circuitOpenExceptionIsGeneratedIfCircuitBreakerIsOpen() throws Exception {
        int concurrency = 3;
        String uuid = UUID.randomUUID().toString();
        ResilienceProcessor processor = (ResilienceProcessor) resilienceManager.getOrCreate(
                IResilienceManager.ResilienceCallConfig.withDefaults()
                        .concurrency(concurrency)
                        .id(uuid)
                        .timeout(100)
                        .build()
        );

        Observable<Long> observable = Observable.create(observableEmitter -> {
            observableEmitter.onNext(10L);
            observableEmitter.onComplete();
        });

        // Test 1 - We have a good Observable but Circuit is forced open
        // We must get a CircuitOpenException
        AtomicBoolean gotException = new AtomicBoolean();
        processor.getCircuitBreaker().transitionToOpenState();
        processor.executeObservable(uuid, observable, Long.class)
                .blockingSubscribe(aLong -> {
                            fail("Circuit is open, we should never get there");
                        },
                        throwable -> {
                            assertTrue(throwable.getClass().isAssignableFrom(CircuitOpenException.class));
                            gotException.set(true);
                        });
        assertTrue(gotException.get(), "We must have received a CircuitOpenException");


        // Test 2 - We have a good Observable and Circuit is forced closed
        // We must NOT get a exception
        gotException.set(true);
        processor.getCircuitBreaker().transitionToClosedState();
        processor.executeObservable(uuid, observable, Long.class)
                .blockingSubscribe(ignored -> {
                            assertEquals(10L, ignored.longValue());
                            gotException.set(false);
                        },
                        throwable -> {
                            fail("Circuit is close, we should never get there");
                        });
        assertFalse(gotException.get());
    }


    /**
     * Test we get a proper type of exception is thrown when circuit is open
     */
    @Test
    @DisplayName("If application throws a exception then the same exception reaches to the clients")
    public void exceptionFromApplicationWillReachToClient() {
        int concurrency = 3;
        String uuid = UUID.randomUUID().toString();
        ResilienceProcessor processor = (ResilienceProcessor) resilienceManager.getOrCreate(
                IResilienceManager.ResilienceCallConfig.withDefaults()
                        .concurrency(concurrency)
                        .id(uuid)
                        .timeout(100)
                        .build()
        );

        Observable<Long> observable = Observable.create(observableEmitter -> {
            throw new CustomException();
        });

        // Test 1 - We have Observable which throws a CustomException (we must get the same)
        final AtomicBoolean gotException = new AtomicBoolean();
        processor.executeObservable(uuid, observable, Long.class)
                .blockingSubscribe(aLong -> {
                            fail("Circuit is open, we should never get there");
                        },
                        throwable -> {
                            assertTrue(throwable.getClass().isAssignableFrom(CustomException.class));
                            gotException.set(true);
                        });
        assertTrue(gotException.get(), "We must have received a CustomException");


        observable = Observable.create(observableEmitter -> {
            throw new NullPointerException();
        });

        // Test 2 - We have Observable which throws a NullPointerException (we must get the same)
        // We must get a CircuitOpenException
        gotException.set(false);
        processor.executeObservable(uuid, observable, Long.class)
                .blockingSubscribe(aLong -> {
                            fail("Circuit is open, we should never get there");
                        },
                        throwable -> {
                            assertTrue(throwable.getClass().isAssignableFrom(NullPointerException.class));
                            gotException.set(true);
                        });
        assertTrue(gotException.get(), "We must have received a NullPointerException");
    }

    /**
     * Test we get a proper type of exception when we make too many calls.
     */
    @Test
    @DisplayName("OverflowException exception is thrown if we make too many calls")
    public void overflowExceptionIsThrownIfWeMakeTooManyCalls() throws InterruptedException {
        int concurrency = 10;
        int queueSize = 1;
        int expectedCountOfSuccessfulRequest = concurrency + queueSize;
        int expectedCountOfErrorRequest = 4;
        int total = expectedCountOfSuccessfulRequest + expectedCountOfErrorRequest;

        String uuid = UUID.randomUUID().toString();
        ResilienceProcessor processor = (ResilienceProcessor) resilienceManager.getOrCreate(
                IResilienceManager.ResilienceCallConfig.withDefaults()
                        .id(uuid)
                        .timeout(5000)
                        .concurrency(concurrency)
                        .queueSize(queueSize)
                        .build()
        );

        Observable<Long> observable = Observable.create(observableEmitter -> {
            Thread.sleep(1000);
            observableEmitter.onNext(10L);
            observableEmitter.onComplete();
        });

        AtomicInteger successCalls = new AtomicInteger();
        AtomicInteger errorCalls = new AtomicInteger();
        AtomicInteger overflowExceptionCalls = new AtomicInteger();
        CountDownLatch waitForAllRequestToComplete = new CountDownLatch(total);

        ParallelThread parallelThread = new ParallelThread(total, "testOverflowExceptionIsThrown");
        parallelThread.execute(() -> {
            processor.executeObservable(uuid, observable, Long.class)
                    .subscribe(
                            aLong -> {
                                successCalls.incrementAndGet();
                                waitForAllRequestToComplete.countDown();
                            },
                            throwable -> {
                                errorCalls.incrementAndGet();
                                if (throwable.getClass().isAssignableFrom(OverflowException.class)) {
                                    overflowExceptionCalls.incrementAndGet();
                                }
                                waitForAllRequestToComplete.countDown();
                            }
                    );
        });
        waitForAllRequestToComplete.await(5, TimeUnit.SECONDS);
        assertEquals(expectedCountOfSuccessfulRequest, successCalls.get(), "Expected these many success calls");
        assertEquals(expectedCountOfErrorRequest, errorCalls.get(), "Expected these many error calls");
        assertEquals(expectedCountOfErrorRequest, overflowExceptionCalls.get(), "Expected these many error calls - must be of OverflowException");
    }

    /**
     * Test we get a proper type of exception when we make too many calls.
     */
    @Test
    @DisplayName("RequestTimeoutException exception is thrown if we took lot of time to finish request")
    public void requestTimeoutExceptionIsThrownIfWeTakeMoreTime() {
        String uuid = UUID.randomUUID().toString();
        ResilienceProcessor processor = (ResilienceProcessor) resilienceManager.getOrCreate(
                IResilienceManager.ResilienceCallConfig.withDefaults()
                        .id(uuid)
                        .timeout(100)
                        .concurrency(2)
                        .build()
        );

        Observable<Long> observable = Observable.create(observableEmitter -> {
            Thread.sleep(1000);
            observableEmitter.onNext(10L);
            observableEmitter.onComplete();
        });

        AtomicBoolean gotException = new AtomicBoolean();
        processor.executeObservable(uuid, observable, Long.class)
                .blockingSubscribe(
                        aLong -> {
                            fail("We should never get here");
                        },
                        throwable -> {
                            assertTrue(throwable.getClass().isAssignableFrom(RequestTimeoutException.class));
                            gotException.set(true);
                        }
                );
        assertTrue(gotException.get(), "We must have received an exception");
    }
}