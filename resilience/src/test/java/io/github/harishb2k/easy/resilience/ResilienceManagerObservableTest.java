package io.github.harishb2k.easy.resilience;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.gitbub.harishb2k.easy.helper.ApplicationContext;
import io.github.harishb2k.easy.resilience.exception.CircuitOpenException;
import io.github.harishb2k.easy.resilience.module.ResilienceModule;
import io.reactivex.Observable;
import junit.framework.TestCase;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

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
}