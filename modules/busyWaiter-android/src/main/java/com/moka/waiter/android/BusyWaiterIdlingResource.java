package com.moka.waiter.android;

import androidx.test.espresso.IdlingResource;

import com.moka.waiter.BusyWaiter;

/**
 * <p>
 * This class is a bridge between espresso's IdlingResource and the app, so the app doesn't have to depend on espresso.
 * <p>
 * See here:
 * https://code.google.com/p/android-test-kit/wiki/EspressoSamples#Using_registerIdlingResource_to_synchronize_with_custom_resource
 * and here:
 * https://code.google.com/p/android-test-kit/source/browse/testapp_test/src/main/java/com/google/android/apps/common/testing/ui/testapp/AdvancedSynchronizationTest.java
 */
public class BusyWaiterIdlingResource implements IdlingResource {

    private final BusyWaiter busyWaiter;

    public BusyWaiterIdlingResource(final BusyWaiter busyWaiter) {
        this.busyWaiter = busyWaiter;
    }

    @Override
    public String getName() {
        return busyWaiter.getName();
    }

    @Override
    public boolean isIdleNow() {
        return busyWaiter.isNotBusy();
    }

    @Override
    public void registerIdleTransitionCallback(final ResourceCallback resourceCallback) {
        busyWaiter.registerNoLongerBusyCallback(new BusyWaiter.NoLongerBusyCallback() {
            @Override
            public void noLongerBusy() {
                resourceCallback.onTransitionToIdle();
            }
        });
    }
}