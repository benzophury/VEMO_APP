package dev.zlt.companion.util

import android.content.Context
import android.content.Intent
import android.net.Uri

data class CarrierPreset(
    val name: String,
    val packageName: String,
)

data class ApnPreset(
    val label: String,
    val profileName: String,
    val apn: String,
    val authMode: String = "none",
)

object AppLauncher {
    const val DEFAULT_CARRIER_PACKAGE = "com.skitto"
    const val DEFAULT_CARRIER_NAME = "Skitto"

    const val DEFAULT_ROUTER_APP_PACKAGE = "com.tplink.tether"
    const val DEFAULT_ROUTER_APP_NAME = "TP-Link Tether"

    val PRESETS = listOf(
        CarrierPreset("Skitto", "com.skitto"),
        CarrierPreset("MyGP (Grameenphone)", "com.portonics.mygp"),
        CarrierPreset("Mukto / MyBL (Banglalink)", "net.banglalink.mybl"),
        CarrierPreset("My Robi", "net.omobio.robisc"),
        CarrierPreset("My Airtel", "net.omobio.airtelsc"),
        CarrierPreset("My Teletalk", "teletalk.teletalkcustomerapp"),
    )

    val ROUTER_PRESETS = listOf(
        CarrierPreset("TP-Link Tether", "com.tplink.tether"),
        CarrierPreset("Tenda WiFi", "com.tenda.router.app"),
        CarrierPreset("ASUS Router", "com.asus.aihome"),
    )

    val APN_PRESETS = listOf(
        ApnPreset("Skitto", "skittonet", "skittonet"),
        ApnPreset("Grameenphone", "GP-INTERNET", "gpinternet"),
        ApnPreset("Banglalink", "Banglalink-WEB", "blweb"),
        ApnPreset("Robi", "Robi-INTERNET", "internet"),
        ApnPreset("Airtel", "Airtel-INTERNET", "internet"),
        ApnPreset("Teletalk", "Teletalk Internet", "wap"),
    )

    /**
     * Best-effort match from the router's reported cellular operator string
     * (e.g. "GrameenPhone", "skitto", "Banglalink", "Robi", "Airtel", "Teletalk")
     * to a known self-care app preset.
     */
    fun matchCarrierForOperator(operator: String?): CarrierPreset? {
        val op = operator?.trim()?.lowercase().orEmpty()
        if (op.isEmpty()) return null
        return when {
            "skitto" in op -> CarrierPreset("Skitto", "com.skitto")
            "grameen" in op || op == "gp" -> CarrierPreset("MyGP", "com.portonics.mygp")
            "banglalink" in op || "bl" == op -> CarrierPreset("Mukto", "net.banglalink.mybl")
            "robi" in op -> CarrierPreset("My Robi", "net.omobio.robisc")
            "airtel" in op -> CarrierPreset("My Airtel", "net.omobio.airtelsc")
            "teletalk" in op -> CarrierPreset("My Teletalk", "teletalk.teletalkcustomerapp")
            else -> null
        }
    }

    fun isAppInstalled(context: Context, packageName: String = DEFAULT_CARRIER_PACKAGE): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun launchCarrierApp(context: Context, packageName: String = DEFAULT_CARRIER_PACKAGE) {
        launchApp(context, packageName)
    }

    fun launchApp(context: Context, packageName: String) {
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
