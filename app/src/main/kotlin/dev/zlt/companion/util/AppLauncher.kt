package dev.zlt.companion.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object AppLauncher {
    const val DEFAULT_CARRIER_PACKAGE = "com.skitto"
    const val DEFAULT_CARRIER_NAME = "Skitto"

    fun isAppInstalled(context: Context, packageName: String = DEFAULT_CARRIER_PACKAGE): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun launchCarrierApp(context: Context, packageName: String = DEFAULT_CARRIER_PACKAGE) {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            // Not installed: redirect to Google Play Store
            try {
                val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(storeIntent)
            } catch (_: Exception) {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
        }
    }
}
