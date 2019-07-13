package com.moka;

import android.app.Activity;
import android.os.Looper;

import androidx.annotation.UiThread;
import androidx.test.espresso.BaseLayerComponent;
import androidx.test.espresso.GraphHolder;
import androidx.test.espresso.UiController;
import androidx.test.espresso.base.ActiveRootLister;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitor;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

import static com.moka.EspressoMokaRunner.failIfInstrumentationCaughtAnyErrors;
import static com.moka.EspressoMokaRunner.loopMainThreadMillis;
import static com.moka.EspressoMokaRunner.runOnMainSync;
import static com.moka.internals.ExceptionSugar.propagate;
import static com.moka.internals.Reflection.getStaticFieldValue;
import static com.moka.internals.Reflection.invokeStatic;
import static java.lang.System.currentTimeMillis;

public final class EspressoInternals {

    // These only need to be init'd once.
    private static final BaseLayerComponent BASE_LAYER_COMPONENT = espressoBaseLayerComponent();

    private EspressoInternals() {
    }

    @SuppressWarnings("ThrowableNotThrown")
    public static void waitForEspressoToIdle() {
        failIfInstrumentationCaughtAnyErrors();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Boolean alreadyLooping = alreadyLooping();

                if (!alreadyLooping) { // avoid recursive looping
                    try {
                        uiController().loopMainThreadUntilIdle();
                    } catch (RuntimeException e) {
                        if (e instanceof IllegalStateException
                                || e.getCause() instanceof IllegalStateException) {
                            Timber.w(e, "Swallowing an IllegalStateException from recursively looping espresso.");
                        } else {
                            Timber.w(e.getCause(),
                                    "Propagating cause %s from loopMainThreadUntilIdle in waitForEspressoToIdle",
                                    e.getMessage());
                            propagate(e);
                        }
                    }
                }
            }
        });
    }

    @UiThread
    public static Boolean alreadyLooping() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            // must be on MainThread, so that we get the correct ThreadLocal value
            ThreadLocal<Boolean> alreadyLoopingThreadLocal = getStaticFieldValue("androidx.test.espresso.base.Interrogator",
                    "interrogating");
            return alreadyLoopingThreadLocal.get();
        } else {
            throw new IllegalThreadStateException("Should only call alreadyLooping() on the ui thread.");
        }
    }

    public static boolean waitForAnyOfOurActivitiesToBeInResumedState() {
        return waitForAnyOfOurActivitiesToBeInResumedState(10000);
    }

    public static boolean waitForAllOfOurActivitiesToBeDestroyed(final int maxWaitTime) {
        boolean notIn = false;
        for (Stage stage : new Stage[]{
                Stage.PRE_ON_CREATE,
                Stage.CREATED,
                Stage.STARTED,
                Stage.RESUMED,
                Stage.RESTARTED,
                Stage.PAUSED,
                Stage.STOPPED
        }) {
            if (!waitForAllActivitiesToBe(notIn, stage, maxWaitTime)) {
                return false;
            }
        }
        return true;
    }

    public static boolean waitForAnyOfOurActivitiesToBeInResumedState(final int maxWaitTime) {
        boolean in = true;
        return waitForAllActivitiesToBe(in, Stage.RESUMED, maxWaitTime);
    }

    public static boolean waitUntilNoneOfOurActivitiesAreResumed() {
        boolean notIn = false;
        return waitForAllActivitiesToBe(notIn, Stage.RESUMED);
    }

    public static boolean waitUntilNoneOfOurActivitiesAreResumedOrPaused() {
        boolean notIn = false;
        waitForAllActivitiesToBe(notIn, Stage.RESUMED);
        return waitForAllActivitiesToBe(notIn, Stage.PAUSED);
    }

    private static boolean waitForAllActivitiesToBe(final boolean shouldActivitiesBeResumed, final Stage stage) {
        return waitForAllActivitiesToBe(shouldActivitiesBeResumed, stage, 10000);
    }

    private static boolean waitForAllActivitiesToBe(final boolean shouldActivitiesBeResumed,
                                                    final Stage stage,
                                                    final int maxWaitTime) {
        long startTime = currentTimeMillis();
        boolean timedOut = currentTimeMillis() - startTime > maxWaitTime;
        boolean someActivitiesAreResumed = !noActivitiesAreInStage(stage);
        Timber.d("some Activities are in Stage %s? %s", stage, someActivitiesAreResumed);
        Timber.d("timedOut? %s", timedOut);
        while (someActivitiesAreResumed != shouldActivitiesBeResumed && !timedOut) {
            failIfInstrumentationCaughtAnyErrors();
            loopMainThreadMillis(300);
            timedOut = currentTimeMillis() - startTime > maxWaitTime;
            someActivitiesAreResumed = !noActivitiesAreInStage(stage);
            Timber.d("timedOut? %s", timedOut);
        }
        return someActivitiesAreResumed == shouldActivitiesBeResumed;
    }

    public static boolean noActivitiesAreInStage(final Stage stage) {
        final AtomicBoolean noActivityWasInStage = new AtomicBoolean(false);
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                final Collection<Activity> activitiesInStage = activityLifecycleMonitor().getActivitiesInStage(stage);
                boolean empty = activitiesInStage.isEmpty();
                noActivityWasInStage.set(empty);
                if (!empty) {
                    Timber.d("Found %s activities: %s", stage, activitiesInStage);
                }
            }
        });
        return noActivityWasInStage.get();
    }

    public static UiController uiController() {
        return BASE_LAYER_COMPONENT.uiController();
    }

    public static ActiveRootLister activeRootLister() {
        return BASE_LAYER_COMPONENT.activeRootLister();
    }

    public static ActivityLifecycleMonitor activityLifecycleMonitor() {
        return ActivityLifecycleMonitorRegistry.getInstance();
    }

    private static BaseLayerComponent espressoBaseLayerComponent() {
        return invokeStatic(GraphHolder.class, "baseLayer");
    }
}
