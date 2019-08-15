package com.sample.app;

import android.os.Bundle;

import androidx.test.espresso.IdlingPolicies;
import androidx.test.runner.AndroidJUnitRunner;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class SampleAndroidJUnitRunner extends AndroidJUnitRunner {

    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        IdlingPolicies.setMasterPolicyTimeout(15, TimeUnit.SECONDS);
        IdlingPolicies.setIdlingResourceTimeout(15, TimeUnit.SECONDS);
    }

}
