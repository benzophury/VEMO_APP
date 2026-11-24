package dev.zlt.companion.util

import android.content.Context
import android.content.Intent
import android.net.Uri

data class CarrierPreset(
    val name: String,
    val packageName: String,
)

object AppLauncher {
    const val DEFAULT_CARRIER_PACKAGE = "com.skitto"
    const val DEFAULT_CARRIER_NAME = "Skitto"

    val PRESETS = listOf(
        CarrierPreset("Skitto", "com.skitto"),
        CarrierPreset("MyGP (Grameenphone)", "com.portonics.mygp"),
        CarrierPreset("Mukto / MyBL (Banglalink)", "net.banglalink.mybl"),
        CarrierPreset("My Robi", "net.omobio.robisc"),
        CarrierPreset("My Airtel", "net.omobio.airtelsc"),
        CarrierPreset("My Teletalk", "teletalk.teletalkcustomerapp"),
    )

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
            return
        }

        // Direct explicit launcher intent attempt if package visibility masked it
        try {
            val directIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                `package` = packageName
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(directIntent)
            return
        } catch (_: Exception) {
            // Not installed or cannot launch directly: redirect to Google Play Store
        }

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
