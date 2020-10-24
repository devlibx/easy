package io.github.harishb2k.easy.resilience;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.github.harishb2k.easy.resilience.ResilienceManagerTest.CustomException;
import io.github.harishb2k.easy.resilience.exception.CircuitOpenException;
import io.github.harishb2k.easy.resilience.exception.OverflowException;
import io.github.harishb2k.easy.resilience.module.ResilienceModule;
import io.reactivex.Observable;
import junit.framework.TestCase;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ResilienceManagerObservableTest extends TestCase {
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
     * Test we get a proper type of exception is thrown when circuit is open
     */
    public void testCircuitOpenExceptionIsThrown() throws Exception {
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
        assertTrue("We must have received a CircuitOpenException", gotException.get());


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
    public void testApplicationExceptionIsThrown() {
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
        assertTrue("We must have received a CustomException", gotException.get());


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
        assertTrue("We must have received a NullPointerException", gotException.get());
    }

    /**
     * Test we get a proper type of exception when we make too many calls.
     */
    public void testOverflowExceptionIsThrown() throws InterruptedException {
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
        CountDownLatch waitForAllThreads = new CountDownLatch(total);
        for (int i = 0; i < total; i++) {
            new Thread(() -> {
                try {
                    waitForAllThreads.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
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
            }).start();
            waitForAllThreads.countDown();
        }

        waitForAllRequestToComplete.await(5, TimeUnit.SECONDS);
        assertEquals("Expected these many success calls", expectedCountOfSuccessfulRequest, successCalls.get());
        assertEquals("Expected these many error calls", expectedCountOfErrorRequest, errorCalls.get());
        assertEquals("Expected these many error calls - must be of OverflowException", expectedCountOfErrorRequest, overflowExceptionCalls.get());
    }
}