package io.github.harishb2k.easy.resilience;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.exceptions.CompositeException;
import io.reactivex.rxjava3.schedulers.Schedulers;
import junit.framework.TestCase;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class RxJavaTest extends TestCase {

    /**
     * A "subscribe" method runs in same thread.
     */
    public void test_Subscribe_Method_Runs_In_Same_Thread() {

        long currentThreadId = Thread.currentThread().getId();
        Observable<Long> observable = Observable.create(emitter -> {
            long currentThreadIdInsideObservable = Thread.currentThread().getId();
            emitter.onNext(currentThreadIdInsideObservable);
        });

        AtomicBoolean gotInside = new AtomicBoolean(false);
        observable.subscribe(l -> {
            assertEquals(currentThreadId, l.longValue());
            gotInside.set(true);
        });
        assertTrue(gotInside.get());
    }

    /**
     * Here heavy work of thread block is happening on "subscribeOn" thread.
     * <p>
     * blockingSubscribe - this will execute on main thread. E.g. in android you dont use
     * blockingSubscribe but observeOn(Android.MainThread())
     */
    public void test_ObserveOn_SubscribeOn() throws Exception {
        long currentThreadId = Thread.currentThread().getId();
        Observable<Long> observable = Observable.create(emitter -> {
            long currentThreadIdInsideObservable = Thread.currentThread().getId();

            Thread.sleep(100);
            emitter.onNext(currentThreadIdInsideObservable);
            emitter.onComplete();

        });

        AtomicLong threadWhereSubscribeMethodIsExecuted = new AtomicLong();
        AtomicLong threadWhereCreateMethodIsCalled = new AtomicLong();
        observable
                .subscribeOn(Schedulers.newThread())
                .blockingSubscribe(aLong -> {
                    threadWhereCreateMethodIsCalled.set(aLong);
                    threadWhereSubscribeMethodIsExecuted.set(Thread.currentThread().getId());
                });
        assertEquals(currentThreadId, threadWhereSubscribeMethodIsExecuted.get());
    }

    /**
     * Do on next is called on before it is passed to final onNext().
     * <p>
     * Note - you can add 1 or more doOnNext()
     */
    public void test_What_DoOnNext_Does() {
        Observable<Long> observable = Observable.create(emitter -> {
            long currentThreadIdInsideObservable = Thread.currentThread().getId();
            emitter.onNext(currentThreadIdInsideObservable);
        });

        AtomicBoolean gotInside1 = new AtomicBoolean(false);
        AtomicBoolean gotInside2 = new AtomicBoolean(false);
        AtomicBoolean gotInside3 = new AtomicBoolean(false);
        observable.doOnNext(aLong -> {
            gotInside1.set(true);
        }).doOnNext(aLong -> {
            gotInside2.set(true);
        }).subscribe(aLong -> {
            gotInside3.set(true);
        });

        assertTrue(gotInside1.get());
        assertTrue(gotInside2.get());
        assertTrue(gotInside3.get());
    }

    /**
     * Called for each error before goes to onError(). Also onError() gets the exact exception which you had thrown in
     * "create".
     *
     * <p>
     * Note - you can add 1 or more doOnError()
     * <p>
     * IMP - if you had called onNext() in your create() then doNext() & doOnNext()  will also be called and
     */
    public void test_What_DoOnError_Does() {
        String uuid = UUID.randomUUID().toString();
        Observable<Long> observable = Observable.create(emitter -> {
            emitter.onError(new RuntimeException(uuid));
        });

        AtomicBoolean gotInsideDoOnNext = new AtomicBoolean(false);
        AtomicBoolean gotInsideOnNext = new AtomicBoolean(false);
        AtomicBoolean gotInside1 = new AtomicBoolean(false);
        AtomicBoolean gotInside2 = new AtomicBoolean(false);
        AtomicBoolean gotInside3 = new AtomicBoolean(false);
        AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();
        observable
                .doOnNext(aLong -> {
                    gotInsideDoOnNext.set(true);
                })
                .doOnError(throwable -> {
                    gotInside1.set(true);
                })
                .doOnError(throwable -> {
                    gotInside2.set(true);
                })
                .subscribe(
                        aLong -> {
                            gotInsideOnNext.set(true);
                        }, throwable -> {
                            throwableAtomicReference.set(throwable);
                            gotInside3.set(true);
                        }
                );

        assertTrue(gotInside1.get());
        assertTrue(gotInside2.get());
        assertTrue(gotInside3.get());
        assertFalse(gotInsideOnNext.get());
        assertFalse(gotInsideDoOnNext.get());

        assertTrue(throwableAtomicReference.get() instanceof RuntimeException);
        assertEquals(uuid, throwableAtomicReference.get().getMessage());
    }


    /**
     * If you throw a exception on doOnError() then the final Exception will be
     * {@link io.reactivex.rxjava3.exceptions.CompositeException}
     * <p>
     * You can get them back
     * <pre>
     *     CompositeException e = (CompositeException) ex;
     *     e.getExceptions().get(0) -> "XYX-Thrown_From_Create"
     *     e.getExceptions().get(1) -> "XYX-Thrown_From_DoOnError"
     * </pre>
     */
    public void test_What_DoOnError_Does_When() {
        String uuid = UUID.randomUUID().toString() + "-Thrown_From_Create";
        Observable<Long> observable = Observable.create(emitter -> {
            emitter.onError(new RuntimeException(uuid));
        });

        String uuidAfterProcessingDoOnError = UUID.randomUUID().toString() + "Thrown_From_DoOnError";
        AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();
        AtomicBoolean gotInsideOnNext = new AtomicBoolean(false);
        AtomicBoolean gotInside1 = new AtomicBoolean(false);
        AtomicBoolean gotInside3 = new AtomicBoolean(false);
        observable
                .doOnError(throwable -> {
                    gotInside1.set(true);
                    throw new RuntimeException(uuidAfterProcessingDoOnError);
                })
                .subscribe(
                        aLong -> {
                            gotInsideOnNext.set(true);
                        }, throwable -> {
                            gotInside3.set(true);
                            throwableAtomicReference.set(throwable);
                        }
                );

        assertTrue(gotInside1.get());
        assertTrue(gotInside3.get());
        assertFalse(gotInsideOnNext.get());
        assertTrue(throwableAtomicReference.get() instanceof CompositeException);
        CompositeException e = (CompositeException) throwableAtomicReference.get();

        assertTrue(e.getExceptions().get(0) instanceof RuntimeException);
        assertEquals(uuid, e.getExceptions().get(0).getMessage());
        assertTrue(e.getExceptions().get(1) instanceof RuntimeException);
        assertEquals(uuidAfterProcessingDoOnError, e.getExceptions().get(1).getMessage());
    }
}
