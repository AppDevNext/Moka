package com.moka;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Instrumentation.ActivityResult;
import android.app.UiAutomation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.NoActivityResumedException;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.core.internal.deps.guava.base.Stopwatch;
import androidx.test.runner.intent.IntentStubber;
import androidx.test.runner.intent.IntentStubberRegistry;

import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.hamcrest.core.AllOf;

import java.lang.annotation.Retention;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import timber.log.Timber;

import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK;
import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME;
import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS;
import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS;
import static android.app.Activity.RESULT_OK;
import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static androidx.test.InstrumentationRegistry.getTargetContext;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.moka.EspressoInternals.waitForEspressoToIdle;
import static com.moka.EspressoInternals.waitUntilNoneOfOurActivitiesAreResumedOrPaused;
import static com.moka.EspressoMokaRunner.getAccessibilityEventListener;
import static com.moka.EspressoMokaRunner.getBusyWaiter;
import static com.moka.EspressoMokaRunner.getMainThreadHandler;
import static com.moka.EspressoMokaRunner.loopMainThreadMillis;
import static com.moka.EspressoMokaRunner.runOnMainSync;
import static com.moka.lib.actions.ViewActions.clickSpannableWithText;
import static com.moka.lib.actions.ViewActions.clickWithRetry;
import static com.moka.lib.actions.ViewActions.requestFocus;
import static com.moka.lib.actions.ViewActions.viewActionWithRetry;
import static com.moka.lib.assertions.ViewAssertions.exists;
import static com.moka.lib.assertions.ViewAssertions.hasChildWithSubstring;
import static com.moka.lib.internals.ExceptionSugar.propagate;
import static com.moka.lib.internals.Reflection.getFieldValue;
import static com.moka.lib.matchers.ViewMatchers.canBeScrolledSoIt;
import static com.moka.lib.matchers.ViewMatchers.contains;
import static com.moka.lib.matchers.ViewMatchers.containsText;
import static com.moka.lib.matchers.ViewMatchers.hasData;
import static com.moka.lib.matchers.ViewMatchers.isGone;
import static com.moka.lib.matchers.ViewMatchers.withClickableText;
import static com.moka.lib.matchers.ViewMatchers.withDrawable;
import static java.lang.String.format;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import static java.util.Locale.US;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Entry point to the Espresso Moka.
 */
public final class EspressoMoka {
    public static final ActivityResult ACTIVITY_RESULT_OK = new ActivityResult(RESULT_OK, null);

    private static final int MAX_WAIT = 30000;
    private static final String ATTEMPTING_TO_RESUME_ACTIVITY = "ATTEMPTING TO RESUME ACTIVITY";
    private static final int AREA_PERCENTAGE = 94;
    private static final ScheduledExecutorService ESPRESSO_SUGAR_THREAD = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(@Nonnull final Runnable r) {
            return new Thread(r, "ESPRESSO_SUGAR_THREAD");
        }
    });
    private static boolean sOnStartCalled = false;
    private static AtomicBoolean sCurrentlyTryingToResume = new AtomicBoolean(false);
    private static AtomicReference<Stopwatch> sStopwatch = new AtomicReference<>(Stopwatch.createUnstarted());

    private EspressoMoka() {
        sStopwatch.get().start();
    }

    static void onStart() {
        sOnStartCalled = true;
    }

    private static void assertOnStartWasCalled() {
        // You have to start onStart() ! otherwise it will fail
        assertTrue(sOnStartCalled);
    }

    /**
     * Causes the Espresso thread to suspend execution.
     * Make sure to only use when debugging.
     */
    public static void pause() {
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            //noinspection ThrowableNotThrown
            propagate(e);
        }
    }

    /**
     * Performs a click action on the given {@link MokaViewInteraction}.
     */
    public static void click(@Nonnull final MokaViewInteraction viewInteraction) {
        viewInteraction.click();
    }

    /**
     * Checks that a {@link View} exists in the {@link View} hierarchy based on its resource id.
     */
    public static void checkThatViewWithIdExists(final @IdRes int viewId) {
        loopMainThreadUntilViewExists(withId(viewId));
    }

    /**
     * Performs an action that clicks on a {@link View} based on its text value.
     */
    public static void clickOnViewWithText(@Nonnull final String string) {
        onViewWithText(is(string), canBeScrolledSoIt(isCompletelyDisplayed())).perform(clickWithRetry());
    }

    /**
     * Performs an action that clicks on a {@link View} based on its resource id.
     */
    public static void clickOnViewWithId(final @IdRes int id) {
        onViewWithId(id, canBeScrolledSoIt(isDisplayingAtLeast(AREA_PERCENTAGE))).perform(requestFocus(), clickWithRetry());
    }

    /**
     * Performs an action that clicks on a {@link View} based on its resource id and on its text value.
     */
    public static void clickOnViewWithIdAndText(final @IdRes int id, final String string) {
        onViewWithIdAndText(id, string).perform(clickWithRetry());
    }

    /**
     * Performs an action that clicks on a clickable substring inside of a {@link TextView}.
     */
    public static void clickOnViewWithClickableText(@Nonnull final String text) {
        getSugarViewInteraction(withClickableText(text)).perform(clickSpannableWithText(text));
    }

    /**
     * Performs an action that clicks the back button.
     */
    public static void pressBack() {
        waitForEspressoToIdle();
        onView(isRoot()).perform(viewActionWithRetry(androidx.test.espresso.action.ViewActions.pressBack()));
        waitForEspressoToIdle();
        waitForAccessibilityStreamToIdle();
    }

    /**
     * Performs an action that clicks the Up button.
     */
    public static void clickOnApplicationUpIcon() {
        onView(withContentDescription("Navigate up")).perform(clickWithRetry());
    }

    /**
     * Checks that a {@link View} is gone based on its resource id.
     */
    public static void checkThatViewWithIdIsGone(final @IdRes int viewId) {
        loopMainThreadUntilViewExists(allOf(withId(viewId), isGone()));
    }

    /**
     * Checks that a {@link View} is not displayed based on its resource id.
     */
    public static void checkThatViewWithIdIsNotDisplayed(final @IdRes int viewId) {
        loopMainThreadUntilViewDoesNotExists(allOf(withId(viewId), isDisplayed()));
    }

    /**
     * Checks that a {@link View} is not being displayed and is not part of the {@link View} hierarchy.
     */
    public static void checkThatViewDoesNotExist(@Nonnull final Matcher<View> viewMatcher) {
        loopMainThreadUntilViewDoesNotExists(viewMatcher, MAX_WAIT);
    }

    /**
     * Checks that a {@link View} with the given resource id is displaying the string.
     */
    public static void checkThatViewWithIdHasStringDisplayed(@IdRes int id, final String substring) {
        onViewWithIdAndText(id, substring).checkMatches(isDisplayed());
    }

    /**
     * Checks that a {@link View} with the given resource id is clickable.
     */
    public static void checkThatViewWithIdWithStringIsClickable(@IdRes int id, final String substring) {
        onViewWithIdAndText(id, substring).checkMatches(isClickable());
    }

    /**
     * Checks that the {@link Toolbar} title is exactly the given text.
     */
    public static void checkToolBarTitleIs(@Nonnull final String title) {
        onView(allOf(instanceOf(Toolbar.class), hasDescendant(withText(title)))).check(matches(isDisplayed()));
    }

    /**
     * Checks that the {@link Toolbar} does not have a TextView, meaning a null or empty title was set.
     */
    public static void checkToolBarTitleIsEmpty() {
        onView(allOf(instanceOf(Toolbar.class), not(hasDescendant(CoreMatchers.<View>instanceOf(TextView.class))))).check(matches(isDisplayed()));
    }

    /**
     * Checks that the {@link Toolbar} title contains the given text.
     */
    public static void checkToolBarContains(@Nonnull final String text) {
        onView(allOf(instanceOf(Toolbar.class), hasDescendant(containsText(text)))).check(matches(isDisplayed()));
    }

    /**
     * Checks that the {@link Toolbar} Up button is not displayed.
     */
    public static void checkThatToolbarUpButtonIsNotVisible() {
        checkThatViewWithContentDescriptionIsNotDisplayedAndItDoesNotExists("Navigate up");
    }

    public static void checkThatToolbarUpButtonIsVisible() {
        onView(withContentDescription("Navigate up")).check(matches(isDisplayed()));
    }

    /**
     * Waits for the accessibility event stream to become idle.
     */
    @SuppressLint("OverlyBroadExceptionCaught")
    public static void waitForAccessibilityStreamToIdle() {
        final UiAutomation uiAutomation = getInstrumentation().getUiAutomation();
        try {
            boolean timedOut = false;
            boolean waitedSuccessfully = false;
            while (!(timedOut) && !(waitedSuccessfully)) {
                try {
                    uiAutomation.waitForIdle(20, 100);
                    waitedSuccessfully = true;
                } catch (TimeoutException e) {
                    Timber.w(e, "Timed out while waiting for Accessibility Stream to be idle.");
                    timedOut = true;
                }
            }
        } catch (Exception e) {
            Timber.w(e, "Ignoring Exception while waitForAccessibilityStreamToIdle");
        }
    }

    /**
     * Performs an action to go to the home screen and then attempts to resume the previous {@link Activity}.
     */
    public static void goToHomeScreenThenAttemptToResumeActivity() {
        goToHomeScreenThenAttemptToResumeActivity(6000, null);
    }

    /**
     * Checks that a text is not being displayed and at the same time is not part of the {@link View} hierarchy.
     */
    public static void checkThatStringIsNotDisplayedAndItDoesNotExists(@Nonnull final String substring) {
        checkThatStringIsNotDisplayedAndItDoesNotExists(substring, MAX_WAIT);
    }

    /**
     * Checks that a text is not being displayed is not part of the {@link TextView} hierarchy.
     * It waits the given amount of time before failing.
     *
     * @param maxWaitInMilliseconds in milliseconds
     */
    public static void checkThatStringIsNotDisplayedAndItDoesNotExists(@Nonnull final String substring, final int maxWaitInMilliseconds) {
        loopMainThreadUntilViewDoesNotExists(allOf(withText(contains(substring)), isDisplayed()), maxWaitInMilliseconds);
    }

    /**
     * Checks that a {@link View} with the given resource id is displaying the drawable resource.
     */
    public static void checkThatImageWithIdWithDrawableIdIsDisplayed(@IdRes int id, @DrawableRes int drawableId) {
        onViewWithId(id).checkMatches(withDrawable(drawableId));
    }

    /**
     * Checks that a {@link View} with the given text is visible (Visibility = VISIBLE), but not necessarily displayed.
     */
    public static void checkThatStringIsVisible(@Nonnull final String substring) {
        onViewWithText(contains(substring), withEffectiveVisibility(androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE));
    }

    /**
     * Checks that a {@link View} containing the substring is displayed.
     */
    public static void checkThatStringIsDisplayed(@Nonnull final String substring) {
        checkThatStringIsDisplayed(substring, MAX_WAIT);
    }

    /**
     * Checks that a {@link View} containing the substring is displayed.
     * It waits the given amount of time before failing.
     */
    public static void checkThatStringIsDisplayed(@Nonnull final String substring, final int maxWaitInMilliseconds) {
        onViewWithText(contains(substring), canBeScrolledSoIt(isDisplayed()), maxWaitInMilliseconds).check(exists());
    }

    /**
     * Checks that a {@link View} matched on its resource id, contains the a child displaying the given substring.
     */
    public static void checkThatViewWithIdHasASubStringWithText(@IdRes final int parentId, @Nonnull final String substring) {
        onViewWithId(parentId).check(hasChildWithSubstring(substring)).checkMatches(isDisplayed());
    }

    /**
     * Checks that a {@link View} containing the substring is displayed and that is satisfies the secondary matcher.
     */
    public static void checkThatStringIsDisplayed(@Nonnull final String substring, @Nonnull final Matcher<View> matcher) {
        onViewWithText(contains(substring), allOf(isDisplayed(), matcher)).check(exists());
    }

    /**
     * Checks that a {@link View} with the text is not being displayed, but it is part of the {@link View} hierarchy
     */
    public static void checkThatStringIsNotDisplayed(@Nonnull final String substring) {
        onViewWithText(contains(substring)).checkMatches(not(isDisplayed()));
    }

    /**
     * Checks that a {@link View} with the given resource id does not exists in the View.
     */
    public static void checkThatViewWithIdDoesNotExist(@IdRes final int viewId) {
        loopMainThreadUntilViewDoesNotExists(withId(viewId));
    }

    @SuppressWarnings("unchecked")
    public static <T> void clickOnAdapterItemAtPosition(@Nonnull final Class<T> type, @IdRes final int adapterViewId, final int position) {
        onAdapterItem(type, (Matcher<T>) instanceOf(type))
                .inAdapterView(withId(adapterViewId))
                .atPosition(position)
                .perform(clickWithRetry());
    }

    /**
     * Creates a {@link MokaViewInteraction} for a {@link View} based on its resource id.
     */
    @Nonnull
    public static MokaViewInteraction onViewWithId(@IdRes final int id) {
        return getSugarViewInteraction(withId(id));
    }

    /**
     * Creates a {@link MokaViewInteraction} for a {@link View} based on its resource id and its content description.
     */
    @Nonnull
    public static MokaViewInteraction onViewWithIdAndContentDescription(@IdRes final int id, @Nonnull final String desc) {
        return getSugarViewInteraction(allOf(withId(id), withContentDescription(desc)));
    }

    /**
     * Creates a {@link MokaViewInteraction} for a {@link View} based on its resource id.
     */
    @Nonnull
    public static MokaViewInteraction onFocusedViewWithId(@IdRes final int id) {
        return getSugarViewInteraction(allOf(withId(id), hasFocus()));
    }

    /**
     * Creates a {@link MokaViewInteraction} for a {@link View} based on its resource id and a matcher.
     */
    @Nonnull
    public static MokaViewInteraction onViewWithId(@IdRes final int id, @Nonnull final Matcher<View> viewMatcher) {
        return getSugarViewInteraction(allOf(withId(id), viewMatcher));
    }

    /**
     * Creates a {@link MokaViewInteraction} for a {@link View} based on its resource id and on its text value.
     */
    @Nonnull
    public static MokaViewInteraction onViewWithIdAndText(@IdRes final int id, String text) {
        return getSugarViewInteraction(allOf(withId(id), withText(text)));
    }

    /**
     * Creates a {@link MokaViewInteraction} for a {@link View} based on its resource id, its text value and a matcher.
     */
    @Nonnull
    public static MokaViewInteraction onViewWithText(@Nonnull final Matcher<String> matcher, @Nonnull final Matcher<View> viewMatcher) {
        return onViewWithText(matcher, viewMatcher, MAX_WAIT);
    }

    /**
     * Creates a {@link MokaViewInteraction} for a {@link View} based on its resource id, its text value and a matcher.
     * It waits the given amount of time before failing.
     *
     * @param maxWaitInMilliseconds in milliseconds
     */
    @Nonnull
    public static MokaViewInteraction onViewWithText(@Nonnull final Matcher<String> matcher,
                                                     @Nonnull final Matcher<View> viewMatcher,
                                                     final int maxWaitInMilliseconds) {
        return getSugarViewInteraction(allOf(withText(matcher), viewMatcher), maxWaitInMilliseconds);
    }

    /**
     * Creates a {@link MokaViewInteraction} for a {@link View} based on its text value.
     */
    @Nonnull
    public static MokaViewInteraction onViewWithText(@Nonnull final String text) {
        return getSugarViewInteraction(withText(text));
    }

    /**
     * Creates a {@link MokaViewInteraction} for a {@link View} based on a String matcher.
     */
    @Nonnull
    public static MokaViewInteraction onViewWithText(@Nonnull final Matcher<String> matcher) {
        return getSugarViewInteraction(withText(matcher));
    }

    /**
     * Creates a {@link MokaViewInteraction} for a {@link View} based on it's hint property value.
     */
    @Nonnull
    public static MokaViewInteraction onViewWithHint(@Nonnull final String hint) {
        return getSugarViewInteraction(withHint(is(hint)));
    }

    public static Matcher<View> withHint(final Matcher<String> stringMatcher) {
        return new BaseMatcher<View>() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public boolean matches(Object item) {
                try {
                    Method method = item.getClass().getMethod("getHint");
                    return stringMatcher.matches(method.invoke(item));
                } catch (NoSuchMethodException e) {
                } catch (InvocationTargetException e) {
                } catch (IllegalAccessException e) {
                }
                return false;
            }
        };
    }

    /**
     * Creates a {@link MokaViewInteraction} from a {@link View} matcher.
     */
    @Nonnull
    public static MokaViewInteraction onView(@Nonnull final Matcher<View> viewMatcher) {
        return getSugarViewInteraction(viewMatcher);
    }

    /**
     * Creates a {@link MokaDataInteraction} that matches by data type and value on an Adapter {@link View} matched by resource id
     */
    @Nonnull
    public static <T> MokaDataInteraction onDataIn(@IdRes final int list, @Nonnull final Class<T> type, @Nonnull final Matcher<T> valueMatcher) {
        onViewWithId(list).loopMainThreadUntil(hasData());
        return checkItemIsPresentIn(list, type, valueMatcher);
    }

    /**
     * Creates a {@link MokaDataInteraction} that matches by data type and value on an Adapter {@link View} matched by resource
     * id and checks that the element is displayed.
     */
    @Nonnull
    public static <T> MokaDataInteraction checkItemIsPresentIn(@IdRes final int list,
                                                               @Nonnull final Class<T> type,
                                                               @Nonnull final Matcher<T> valueMatcher) {
        onViewWithId(list).loopMainThreadUntil(hasData());
        MokaDataInteraction mokaDataInteraction = onAdapterItem(type, valueMatcher).inAdapterView(withId(list));
        mokaDataInteraction.check(matches(isDisplayed()));
        return mokaDataInteraction;
    }

    /**
     * Creates a {@link MokaDataInteraction} that matches by data type and value on any Adapter View.
     */
    @Nonnull
    public static <T> MokaDataInteraction onAdapterItem(@Nonnull final Class<T> type, @Nonnull final Matcher<T> valueMatcher) {
        return getSugarDataInteraction(AllOf.allOf(is(instanceOf(type)), valueMatcher));
    }

    /**
     * Creates a {@link MokaDataInteraction} from a data matcher.
     */
    @Nonnull
    private static MokaDataInteraction getSugarDataInteraction(@Nonnull final Matcher<?> dataMatcher) {
        return new MokaDataInteraction(onData(dataMatcher));
    }

    /**
     * Loops on the main thread until the {@link View} we are trying to match exists.
     */
    @SuppressWarnings("WeakerAccess")
    public static void loopMainThreadUntilViewExists(@Nonnull final Matcher<View> viewMatcher) {
        loopMainThreadUntilViewExists(viewMatcher, MAX_WAIT);
    }

    /**
     * Loops on the main thread until the {@link View} we are trying to match exists.
     * It waits the given amount of time before failing.
     *
     * @param maxWaitInMilliseconds in milliseconds
     */
    @SuppressWarnings("WeakerAccess")
    public static void loopMainThreadUntilViewExists(@Nonnull final Matcher<View> viewMatcher, final int maxWaitInMilliseconds) {
        loopMainThreadUntilView(viewMatcher, true, maxWaitInMilliseconds);
    }

    public static boolean intentWillBeStubbedOut(@Nonnull final Intent intent) {
        return intentStubber().getActivityResultForIntent(intent) != null;
    }

    /**
     * Performs a global action to go to the home screen, attempt to resume the previous activity and execute the given task.
     *
     * @param delayInMilliSeconds amount of time to wait (in milliseconds) before attempting to resume.
     * @param afterHomeScreenTask task to execute after resuming the previous activity.
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    public static void goToHomeScreenThenAttemptToResumeActivity(final int delayInMilliSeconds, @Nullable final Runnable afterHomeScreenTask) {
        waitForEspressoToIdle();
        // Finished in TestRunner when some activity get resumed
        if (sCurrentlyTryingToResume.compareAndSet(false, true)) {
            launchHomeScreen();
            getBusyWaiter().busyWith(ATTEMPTING_TO_RESUME_ACTIVITY);
            getMainThreadHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (afterHomeScreenTask != null) {
                        afterHomeScreenTask.run();
                    }
                    bringOurTaskToFront();
                    ESPRESSO_SUGAR_THREAD.schedule(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                loopMainThreadMillis(delayInMilliSeconds / 12);
                                final long millisSinceLastResumed = elapsedTimeSinceProcessStartIn(MILLISECONDS);
                                if (millisSinceLastResumed > (delayInMilliSeconds * 1.2)) {
                                    Timber.w("No recently resumed activity, trying again... last resumed was over %d millis ago",
                                            millisSinceLastResumed);
                                    goToHomeScreenThenAttemptToResumeActivity();
                                } else {
                                    Timber.d("Activity resumed successfully after going to the Home screen, nothing further to do.");
                                    getBusyWaiter().completed(ATTEMPTING_TO_RESUME_ACTIVITY);
                                }
                            } finally {
                                sCurrentlyTryingToResume.set(false);
                            }
                        }
                    }, (long) (delayInMilliSeconds * .3f), MILLISECONDS);
                }
            }, delayInMilliSeconds);
        } else {
            getBusyWaiter().completed(ATTEMPTING_TO_RESUME_ACTIVITY);
            Timber.d("Skipped trying to go home because we were already trying...  ");
        }
    }

    /**
     * Performs a global action that attempts to launch the home screen.
     */
    @SuppressWarnings("WeakerAccess")
    @SuppressLint("OverlyBroadExceptionCaught")
    public static void launchHomeScreen() {
        tryToGetToHomeScreen();
        try {
            for (int i = 0; i < 3; i++) {
                // don't want to accidentally send back to our app, so make sure it is not in the foreground first.
                if (!waitUntilNoneOfOurActivitiesAreResumedOrPaused()) {
                    tryToGetToHomeScreen();
                }
            }
        } catch (Exception e) {
            // UiAutomation is flaky and occasionally we can't connect.
            // we will just ignore
            Timber.i("Couldn't connect to UiAutomation to send back key");
        }
        waitForAccessibilityStreamToIdle();
        waitUntilNoneOfOurActivitiesAreResumedOrPaused();
        Timber.d("Finished Launching home screen.");
    }

    /**
     * Performs a global action that attempts to open the Notifications Drawer.
     */
    @SuppressLint("OverlyBroadExceptionCaught")
    public static void tryToOpenNotifications() {
        getAccessibilityEventListener().waitUntil(not(withPackageName(getInstrumentation().getTargetContext().getPackageName())));
        Timber.i("Trying to open notifications via performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)");
        try {
            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
        } catch (Exception e) {
            // UiAutomation is flaky and occasionally we can't connect.
            sendHomeIntent();
        }
        waitForEspressoToIdle();
    }

    /**
     * Performs a global action. Such an action can be performed at any moment
     * regardless of the current application or user location in that application.
     *
     * @see android.accessibilityservice.AccessibilityService#GLOBAL_ACTION_BACK
     * @see android.accessibilityservice.AccessibilityService#GLOBAL_ACTION_HOME
     * @see android.accessibilityservice.AccessibilityService#GLOBAL_ACTION_NOTIFICATIONS
     * @see android.accessibilityservice.AccessibilityService#GLOBAL_ACTION_RECENTS
     */
    @SuppressWarnings("WeakerAccess")
    public static void performGlobalAction(@GlobalAction final int globalAction) {
        // looked at uiautomator code to figure out how to do this.
        final UiAutomation automation = getInstrumentation().getUiAutomation();
        waitForAccessibilityStreamToIdle();
        assertNotNull("We can open the notification drawer without an instance of uiAutomation", automation);
        assertTrue("Couldn't open the notification drawer.", automation.performGlobalAction(globalAction));
        waitForAccessibilityStreamToIdle();
    }

    /**
     * Returns a {@link ComponentName} for an Activity class.
     * Useful for Intent matching with {@link androidx.test.espresso.intent.matcher.IntentMatchers#hasComponent(ComponentName)}
     */
    @Nonnull
    public static ComponentName withClass(@Nonnull final Class<? extends Activity> activityClass) {
        return new ComponentName(getTargetContext(), activityClass);
    }

    /**
     * Returns a localized string from the application's package's default string table.
     */
    @Nonnull
    public static String getString(@StringRes int id) {
        return getTargetContext().getString(id);
    }

    @SuppressWarnings("SameParameterValue")
    private static void checkThatViewWithContentDescriptionIsNotDisplayedAndItDoesNotExists(@Nonnull final String contentDescription) {
        loopMainThreadUntilViewDoesNotExists(allOf(withContentDescription(contentDescription), isDisplayed()));
    }

    @SuppressLint("OverlyBroadExceptionCaught")
    private static void tryToGetToHomeScreen() {
        //todo - get reference to main package
        getAccessibilityEventListener().waitUntil(not(withPackageName(getInstrumentation().getTargetContext().getPackageName())));
        Timber.i("Sending Home button via performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)");
        try {
            performGlobalAction(GLOBAL_ACTION_HOME);
        } catch (Exception e) {
            // UiAutomation is flaky and occasionally we can't connect.
            sendHomeIntent();
        }
        waitForEspressoToIdle();
    }

    @SuppressWarnings("deprecation")
    // we are only using getRunningTasks for testing purposes, but this may not work on 21+
    private static void bringOurTaskToFront() {
        Timber.d("After sitting on the home screen for a while, going to move our task to front.");
        final ActivityManager activityManager = (ActivityManager) getTargetContext().getSystemService(Context.ACTIVITY_SERVICE);
        final List<RunningTaskInfo> runningTasks = activityManager.getRunningTasks(10);
        RunningTaskInfo ourTask = null;
        for (RunningTaskInfo runningTask : runningTasks) {
            final String lastActiveTime = Build.VERSION.SDK_INT < 28 ? format(US, "lastActiveTime: %d,", getTaskLastActiveTime(runningTask)) : "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Timber.d("Task: %s, Description: %s, %s Top Activity: %s, BaseActivity: %s",
                        runningTask,
                        runningTask.description,
                        lastActiveTime,
                        runningTask.topActivity,
                        runningTask.baseActivity);
            }
        }
    }

    private static Long getTaskLastActiveTime(RunningTaskInfo runningTask) {
        return getFieldValue(runningTask, RunningTaskInfo.class, "lastActiveTime");
    }

    private static void sendHomeIntent() {
        Timber.i("Sending Home button intent");
        // From: http://stackoverflow.com/questions/4756835/how-to-launch-home-screen-programmatically-in-android
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getTargetContext().startActivity(startMain);
    }

    private static IntentStubber intentStubber() {
        final AtomicReference<IntentStubber> intentStubber = new AtomicReference<>(null);
        // IntentStubberRegistry.getInstance() must be invoked from the main thread.
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                intentStubber.compareAndSet(null, IntentStubberRegistry.getInstance());
            }
        });
        return intentStubber.get();
    }

    @SuppressWarnings("ThrowableNotThrown")
    private static void loopMainThreadUntilView(@Nonnull final Matcher<View> viewMatcher,
                                                final boolean shouldExist,
                                                final int maxWaitInMilliseconds) {
        final AtomicBoolean viewExists = new AtomicBoolean(!shouldExist);
        final AtomicReference<Throwable> exception = new AtomicReference<>(null);

        long startTime = System.currentTimeMillis();
        boolean timedOut = false;
        long count = 1;
        do {
            try {
                final long currentTimeMillis = System.currentTimeMillis();
                timedOut = currentTimeMillis - startTime > maxWaitInMilliseconds;
                if (timedOut) {
                    Timber.d("Timing out with start time = %s and currentTimeMillis = %s", startTime, currentTimeMillis);
                    break;
                }
                count *= 2;
                if (count > 32) { // back off after a few tries
                    Timber.d("Going to idle this many times before checking again: %d", count);
                    for (int i = 0; i < count; i++) {
                        waitForEspressoToIdle();
                    }
                }
                Espresso.onView(viewMatcher).check(new ViewAssertion() {
                    @Override
                    public void check(final View view, final NoMatchingViewException noViewFoundException) {
                        final StringDescription description = new StringDescription();
                        viewMatcher.describeTo(description);
                        Timber.d("Checking for: %s", description.toString());
                        if (noViewFoundException != null) {
                            exception.set(noViewFoundException);
                        }
                        if (view != null && !(view.hasWindowFocus())) {
                            Timber.w("Found view %s matching: %s, but its Window did not have focus.", view, viewMatcher);
                            view.getRootView().requestFocus();
                        }
                        viewExists.set(view != null && view.hasWindowFocus());
                    }
                });
            } catch (RuntimeException e) {
                if (e instanceof IllegalStateException
                        || e.getCause() instanceof IllegalStateException) {
                    Timber.w(e, "Swallowing an IllegalStateException from recursively looping espresso.");
                } else if (e instanceof NoActivityResumedException
                        || e.getMessage().contains("Waited for the root")
                        || (e.getCause() != null
                        && e.getCause().getMessage() != null
                        && e.getCause().getMessage().contains("Waited for the root"))) {
                    Timber.e(e, "Going to try and resume activity...");
                    startTime = System.currentTimeMillis();
                    count = 0;
                    Timber.d("Reset starttime to %s", startTime);
                } else {
                    propagate(e);
                }
            }
        } while (viewExists.get() != shouldExist);

        if (timedOut) {
            final StringDescription stringDescription = new StringDescription();
            viewMatcher.describeTo(stringDescription);
            final Throwable noViewFoundException = exception.get();
            fail(format(US, "Timed out after waiting %d seconds for view %s that meets this criteria %s\n%s",
                    maxWaitInMilliseconds / 1000,
                    shouldExist ? "to exist" : "to NOT exist",
                    stringDescription.toString(),
                    noViewFoundException != null ? noViewFoundException.getMessage() : ""
            ));
        }
    }

    @Nonnull
    private static Long elapsedTimeSinceProcessStartIn(@Nonnull final TimeUnit timeUnit) {
        return sStopwatch.get().elapsed(timeUnit);
    }

    private static BaseMatcher<AccessibilityEvent> withPackageName(@Nonnull final String packageName) {
        return new BaseMatcher<AccessibilityEvent>() {
            @Override
            public boolean matches(final Object o) {
                if (o instanceof AccessibilityEvent) {
                    final AccessibilityEvent accessibilityEvent = (AccessibilityEvent) o;
                    final CharSequence charSequence = accessibilityEvent.getPackageName();
                    return charSequence.toString().contains(packageName);
                }
                return false;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText(" contains packageName = " + packageName);
            }
        };
    }

    private static void loopMainThreadUntilViewDoesNotExists(@Nonnull final Matcher<View> viewMatcher) {
        loopMainThreadUntilViewDoesNotExists(viewMatcher, MAX_WAIT);
    }

    private static void loopMainThreadUntilViewDoesNotExists(@Nonnull final Matcher<View> viewMatcher, final int maxWaitInMilliseconds) {
        loopMainThreadUntilView(viewMatcher, false, maxWaitInMilliseconds);
    }

    @Nonnull
    private static MokaViewInteraction getSugarViewInteraction(@Nonnull final Matcher<View> viewMatcher) {
        return getSugarViewInteraction(viewMatcher, MAX_WAIT);
    }

    @Nonnull
    private static MokaViewInteraction getSugarViewInteraction(@Nonnull final Matcher<View> viewMatcher, final int maxWaitInMilliseconds) {
        assertOnStartWasCalled();
        loopMainThreadUntilViewExists(viewMatcher, maxWaitInMilliseconds);
        return new MokaViewInteraction(Espresso.onView(viewMatcher));
    }

    @SuppressWarnings("WeakerAccess")
    @Retention(SOURCE)
    @IntDef({GLOBAL_ACTION_BACK, GLOBAL_ACTION_HOME, GLOBAL_ACTION_NOTIFICATIONS, GLOBAL_ACTION_RECENTS})
    public @interface GlobalAction {
    }
}
