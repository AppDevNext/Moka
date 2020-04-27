package com.moka.utils

import android.Manifest
import android.app.Activity
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule

abstract class BaseScreenshotTest<T : Activity>(activityClass: Class<T>) {

    @get:Rule
    var ruleScreenShot = ScreenshotActivityRule(activityClass)

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

}