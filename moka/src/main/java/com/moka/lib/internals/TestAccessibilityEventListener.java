package com.moka.lib.internals;

import android.annotation.SuppressLint;
import android.view.accessibility.AccessibilityEvent;

import com.moka.waiter.android.BusyWaiter;

import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import timber.log.Timber;

import static com.moka.EspressoMoka.waitForAccessibilityStreamToIdle;
import static com.moka.lib.internals.ExceptionSugar.propagate;

public class TestAccessibilityEventListener {

    private final BusyWaiter busyWaiter;
    private final Executor accessibilityEventListenerExecutor = Executors.newSingleThreadExecutor();
    private List<Matcher<AccessibilityEvent>> matchers = new CopyOnWriteArrayList<>();

    public TestAccessibilityEventListener(final BusyWaiter busyWaiter) {
        this.busyWaiter = busyWaiter;
    }

    // It's okay to catch a Throwable here since the goal is to log unexpected errors within an matcher that we do not own.
    @SuppressLint("OverlyBroadExceptionCaught")
    public void onAccessibilityEvent(final AccessibilityEvent accessibilityEvent) {
        for (Iterator<Matcher<AccessibilityEvent>> iterator = matchers.iterator(); iterator.hasNext(); ) {
            final Matcher<AccessibilityEvent> matcher = iterator.next();
            final boolean matches;
            try {
                matches = matcher.matches(accessibilityEvent);
                if (matches) {
                    Timber.d("Found a match for matcher (%s) : %s", toPrintString(matcher), accessibilityEvent.toString());
                    accessibilityEventListenerExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            waitForAccessibilityStreamToIdle();
                            busyWaiter.postCompleted(matcher);
                            waitForAccessibilityStreamToIdle();
                        }
                    });
                    matchers.remove(matcher);
                }
            } catch (Throwable e) {
                Timber.e(e, "Error matching accessibilityEvent");
                propagate(e);
            }
        }

        Timber.d(accessibilityEvent.toString());
    }

    public void waitUntil(Matcher<AccessibilityEvent> accessibilityEventMatcher) {
        matchers.add(accessibilityEventMatcher);
        busyWaiter.busyWith(accessibilityEventMatcher);
    }

    public static <T> String toPrintString(final Matcher<T> matcher) {
        final StringDescription stringDescription = new StringDescription();
        matcher.describeTo(stringDescription);
        return stringDescription.toString();
    }
}
