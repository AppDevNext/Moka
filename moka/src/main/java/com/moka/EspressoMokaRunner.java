package com.moka;

import android.app.Activity;
import android.app.UiAutomation;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;

import androidx.test.runner.intent.IntentCallback;
import androidx.test.runner.intent.IntentMonitorRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleCallback;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.moka.internals.ExceptionFromAnotherPlace;
import com.moka.internals.TestAccessibilityEventListener;
import com.moka.mainthread.MainThread;
import com.moka.waiter.BusyWaiter;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import timber.log.Timber;

import static android.os.Looper.getMainLooper;
import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static androidx.test.InstrumentationRegistry.getTargetContext;
import static com.moka.EspressoInternals.alreadyLooping;
import static com.moka.EspressoInternals.waitForEspressoToIdle;
import static com.moka.EspressoMoka.intentWillBeStubbedOut;
import static com.moka.internals.AnimatorsMoka.disableAnimators;
import static com.moka.internals.DeviceSugar.printAvailableScreenSpace;
import static com.moka.internals.ExceptionSugar.propagate;
import static com.moka.internals.PlayServicesSugar.printPlayServicesVersion;
import static java.lang.String.format;
import static java.util.Locale.US;

public final class EspressoMokaRunner {

    private static final Handler MAIN_THREAD_HANDLER = new Handler(getMainLooper());
    private static final Thread MAIN_THREAD = getMainLooper().getThread();
    private static final BusyWaiter BUSY_WAITER = BusyWaiter.Companion.withOperationsCompletedOn(new BusyWaiterExecutionThread());
    private static final AtomicReference<Object> LAST_ERROR_OBJECT = new AtomicReference<>(null);
    private static final AtomicReference<Throwable> LAST_THROWABLE = new AtomicReference<>(null);
    private static final AtomicBoolean WAIT_FOR_ACTIVITIES_TO_RESUME = new AtomicBoolean(true);
    private static final TestAccessibilityEventListener ACCESSIBILITY_EVENT_LISTENER = new TestAccessibilityEventListener(BUSY_WAITER);
    private static final ActivityLifecycleCallback FINISH_ASYNC_OPERATION_WHEN_ACTIVITY_RESUMES = new ResumedActivityWatcher();

    private static volatile Thread INSTRUMENTATION_THREAD;
    private static boolean IS_STARTED = false;
    private static boolean READY_TO_RUN_TESTS = false;

    private EspressoMokaRunner() {
    }

    public static void onCreate(final Bundle arguments) {
        MainThread.INSTANCE.checkOnMainThread();
        catchExceptionsFromTheApp();
        disableAnimators();

        // turn off espresso usage tracking.
        arguments.putString("disableAnalytics", "true");
    }

    public static void onStart() {
        MainThread.INSTANCE.checkNotOnMainThread();
        IS_STARTED = true;
        INSTRUMENTATION_THREAD = Thread.currentThread();
        makeEspressoWaitUntilActivitiesAreResumed();
        printPlayServicesVersion();
        printAvailableScreenSpace();
        setupAccessibilityEventListener();
        EspressoMoka.onStart();
    }

    public static void onDestroy() {
        /* no op for now */
    }

    public static boolean onException(Object obj, @Nullable Throwable e) {
        LAST_ERROR_OBJECT.set(obj);
        LAST_THROWABLE.set(e);

        if (INSTRUMENTATION_THREAD == null && MainThread.INSTANCE.onMainThread()) {
            // the application had an exception during start-up, so we should just fail
            return false;
        }
        interruptInstrumentationThreadIfWaiting();
        return true;
    }

    public static void runOnMainSync(@Nonnull final Runnable runner) {
        runOnMainSyncDelayed(runner, 0);
    }

    @Nonnull
    static Handler getMainThreadHandler() {
        return MAIN_THREAD_HANDLER;
    }

    @Nonnull
    public static BusyWaiter getBusyWaiter() {
        return BUSY_WAITER;
    }

    @Nonnull
    static TestAccessibilityEventListener getAccessibilityEventListener() {
        return ACCESSIBILITY_EVENT_LISTENER;
    }

    static void failIfInstrumentationCaughtAnyErrors() {
        if (!IS_STARTED || !READY_TO_RUN_TESTS) {
            // Don't fail until we have at least gone thru the start up process and are ready to run tests
            return;
        }
        final Throwable throwable = LAST_THROWABLE.get();
        final Object lastObject = LAST_ERROR_OBJECT.get();
        LAST_THROWABLE.set(null);
        LAST_ERROR_OBJECT.set(null);
        if (throwable != null && !(shouldIgnoreError(throwable, lastObject))) {
            // TODO modify spoon so I can take a screen shot here.
            Timber.e(throwable, "failIfInstrumentationCaughtAnyErrors is going to rethrow");
            throw new ExceptionFromAnotherPlace(throwable, lastObject);
        } else if (throwable != null) {
            Timber.w(throwable, "Ignoring exception (%s) from: %s", throwable.getMessage(), lastObject);
        }
    }

    static void loopMainThreadMillis(final int delayMillis) {
        // not sure if this check is 100% necessary.
        final AtomicBoolean alreadyLooping = new AtomicBoolean(false);
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                alreadyLooping.set(alreadyLooping());
            }
        });
        if (alreadyLooping.get()) {
            Timber.d("Main thread is already looping, NOT looping for %d millis", delayMillis);
            return;
        }

        runOnMainSyncDelayed(new Runnable() {
                                 @Override
                                 public void run() {
                                     Timber.d("Main thread has been looping for %d millis", delayMillis);
                                 }
                             },
                delayMillis);
    }

    public static void runOnMainSyncDelayed(@Nonnull final Runnable runner, final int delayMillis) {
        // the built in "runOnMainSync" is not very flexible ( it is not easily interrupted )
        // if we want the tests to continue after app crash we need this to be interruptable.
        failIfInstrumentationCaughtAnyErrors();
        if (MainThread.INSTANCE.onMainThread()) {
            if (delayMillis > 0) {
                throw new IllegalStateException(format(US, "Can't runOnMainSyncDelayed with delay(%d) > 0 if you are already on the main thread",
                        delayMillis));
            }
            runner.run();
        } else {
            final FutureTask<Throwable> booleanFutureTask = new FutureTask<>(new Callable<Throwable>() {
                @Override
                public Throwable call() throws Exception {
                    try {
                        runner.run();
                    } catch (Throwable e) {
                        return e;
                    }
                    return null;
                }
            });
            // post something AFTER the runnable that we can drain the queue.
            MAIN_THREAD_HANDLER.postDelayed(booleanFutureTask, delayMillis);
            try {
                final Throwable throwable = booleanFutureTask.get();
                if (IS_STARTED && READY_TO_RUN_TESTS && throwable != null) {
                    throw new ExceptionFromAnotherPlace(throwable, MAIN_THREAD_HANDLER.getLooper().getThread());
                } else if (throwable != null) {
                    LAST_THROWABLE.set(throwable);
                    LAST_ERROR_OBJECT.set(runner);
                }
            } catch (InterruptedException | ExecutionException e) {
                Timber.w(e, "runOnMainSync was Interrupted, it was probably not synchronous.");
            }
        }
        failIfInstrumentationCaughtAnyErrors();
    }

    public static void waitForIdleSync() {
        failIfInstrumentationCaughtAnyErrors();
        waitForEspressoToIdle();
        failIfInstrumentationCaughtAnyErrors();
        if (IS_STARTED) {
            // WARNING:
            // this assumes the GoogleInstrumentation super class calls this as part of super.onStart,
            // but BEFORE it starts run tests
            READY_TO_RUN_TESTS = true;
        }
    }

    private static boolean shouldIgnoreError(final Throwable throwable, final Object lastObject) {
        // setting up for future if/else
        //noinspection RedundantIfStatement
        if (throwable instanceof NullPointerException
                && lastObject instanceof Thread
                && ((Thread) lastObject).getName().contains("qtp")) {
            // ignore NPE from WireMock/Jetty on background threads
            return true;
        }

        return false;
    }

    private static void catchExceptionsFromTheApp() {
        final Thread.UncaughtExceptionHandler originalUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread thread, final Throwable ex) {
                if (thread.equals(INSTRUMENTATION_THREAD)) {
                    propagate(ex);
                } else {
                    Timber.w(ex, "ExecutionThread with name (%s) died with exception", thread.getName());
                    onException(thread, ex);
                    interruptInstrumentationThreadIfWaiting();
                }
            }
        });
        // we should keep the original on the main thread until later, in case there is a problem during app start up.
        MAIN_THREAD.setUncaughtExceptionHandler(originalUncaughtExceptionHandler);
    }

    private static void interruptInstrumentationThreadIfWaiting() {
        // wake up the test thread as whatever it was waiting for it probably not going to happen.
        // it should just fail and move on to the next test.
        if (INSTRUMENTATION_THREAD != null) {
            Thread.State state = INSTRUMENTATION_THREAD.getState();
            Timber.i("Instrumentation ExecutionThread State: %s", state);
            if (state == Thread.State.BLOCKED || state == Thread.State.WAITING) {
                Timber.i("About to interrupt Instrumentation ExecutionThread:");
                INSTRUMENTATION_THREAD.interrupt();
                state = INSTRUMENTATION_THREAD.getState();
                Timber.i("Instrumentation ExecutionThread State after interrupt: %s", state);
            }
        }
    }

    private static void setupAccessibilityEventListener() {
        // wrap in an anonymous class so we don't get problems on old API levels.
        getInstrumentation().getUiAutomation().setOnAccessibilityEventListener(new UiAutomation.OnAccessibilityEventListener() {
            @Override
            public void onAccessibilityEvent(final AccessibilityEvent accessibilityEvent) {
                ACCESSIBILITY_EVENT_LISTENER.onAccessibilityEvent(accessibilityEvent);
            }
        });
    }

    public static void tellEspressoToNotWaitForActivityToResume() {
        WAIT_FOR_ACTIVITIES_TO_RESUME.set(false);
    }

    public static void tellEspressoToWaitForActivityToResume() {
        WAIT_FOR_ACTIVITIES_TO_RESUME.set(true);
    }

    private static void makeEspressoWaitUntilActivitiesAreResumed() {
        IntentMonitorRegistry.getInstance().addIntentCallback(new IntentCallback() {
            @Override
            public void onIntentSent(final Intent intent) {
                runOnMainSync(new Runnable() {
                    @Override
                    public void run() {
                        if (!intentWillBeStubbedOut(intent)
                                && WAIT_FOR_ACTIVITIES_TO_RESUME.get()
                                && isExplicitIntent(intent)
                                && targetsSomethingInTheAppUnderTest(intent)) {
                            // finished in the activity lifecycle monitor
                            BUSY_WAITER.busyWith(intent.getComponent().getClassName());
                        }
                    }
                });
            }
        });

        ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(FINISH_ASYNC_OPERATION_WHEN_ACTIVITY_RESUMES);
    }

    private static boolean isExplicitIntent(@Nonnull final Intent intent) {
        return intent.getComponent() != null;
    }

    private static boolean targetsSomethingInTheAppUnderTest(final Intent intent) {
        return Objects.equals(intent.getPackage(), getTargetContext().getPackageName());
    }

    private static class ResumedActivityWatcher implements ActivityLifecycleCallback {
        @Override
        public void onActivityLifecycleChanged(final Activity activity, final Stage stage) {
            if (stage == Stage.RESUMED) {
                // started in UiTest.getActivity()
                BUSY_WAITER.completed(activity.getClass().getName());
            }
        }
    }

    private static class BusyWaiterExecutionThread implements BusyWaiter.ExecutionThread {
        @Override
        public void execute(final Runnable runnable) {
            MAIN_THREAD_HANDLER.post(runnable);
        }
    }
}
