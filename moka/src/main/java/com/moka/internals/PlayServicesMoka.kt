@file:JvmName("PlayServicesSugar")

package com.moka.internals

import android.content.pm.PackageManager
import androidx.test.InstrumentationRegistry
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import timber.log.Timber

fun googlePlayServicesNotInstalled(): Boolean {
    return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(InstrumentationRegistry.getInstrumentation().targetContext) != ConnectionResult.SUCCESS
}

fun printPlayServicesVersion() {
    try {
        val packageInfo = InstrumentationRegistry.getTargetContext().packageManager.getPackageInfo("com.google.android.gms", 0)
        Timber.i("Play services APK versionName: %s versionCode: %s", packageInfo.versionName, packageInfo.versionCode)
    } catch (e: PackageManager.NameNotFoundException) {
        Timber.i("No play services found on device")
    }
}
