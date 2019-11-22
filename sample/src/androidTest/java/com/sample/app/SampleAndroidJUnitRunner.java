package com.sample.app;

import android.os.Bundle;

import androidx.test.espresso.IdlingPolicies;
import androidx.test.runner.AndroidJUnitRunner;

import com.moka.EspressoMokaRunner;
import com.moka.lib.debug.DebugTrace;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

@SuppressWarnings("unused")
public class SampleAndroidJUnitRunner extends AndroidJUnitRunner {

    @Override
    public void onCreate(Bundle arguments) {
        EspressoMokaRunner.onCreate(arguments);
        super.onCreate(arguments);
        IdlingPolicies.setMasterPolicyTimeout(15, TimeUnit.SECONDS);
        IdlingPolicies.setIdlingResourceTimeout(15, TimeUnit.SECONDS);
    }

    @Override
    public void onStart() {
        EspressoMokaRunner.onStart();
        super.onStart();
    }

    @Override
    public void waitForIdleSync() {
        super.waitForIdleSync();
        EspressoMokaRunner.waitForIdleSync();
    }

    @Override
    public boolean onException(Object obj, Throwable e) {
        DebugTrace.beginSection(e.getMessage() + " SampleAndroidJUnitRunner.onException");
        try {
            super.onException(obj, e);
            Timber.e(e, "%s", obj.toString());
            return EspressoMokaRunner.onException(obj, e);
        } finally {
            DebugTrace.endSection();
        }
    }
}
