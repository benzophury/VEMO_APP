package dev.zlt.companion.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouterSettingsTest {

    @Test
    fun `not configured without a password`() {
        assertFalse(RouterSettings().isConfigured)
    }

    @Test
    fun `configured with a valid host and a password`() {
        assertTrue(RouterSettings(host = "192.168.0.1", password = "secret").isConfigured)
    }

    @Test
    fun `demo mode counts as configured without any password`() {
        assertTrue(RouterSettings(password = "", demoMode = true).isConfigured)
    }

    @Test
    fun `demo mode is off by default`() {
        assertFalse(RouterSettings().demoMode)
    }

    @Test
    fun `carrier app defaults to Skitto`() {
        val settings = RouterSettings()
        org.junit.Assert.assertEquals("Skitto", settings.carrierAppName)
        org.junit.Assert.assertEquals("com.skitto", settings.carrierPackageName)
    }

    @Test
    fun `custom carrier app is configurable`() {
        val settings = RouterSettings(
            carrierAppName = "MyGP",
            carrierPackageName = "com.portonics.mygp",
        )
        org.junit.Assert.assertEquals("MyGP", settings.carrierAppName)
        org.junit.Assert.assertEquals("com.portonics.mygp", settings.carrierPackageName)
    }

    @Test
    fun `carrier auto-match and fallback router app have sensible defaults`() {
        val settings = RouterSettings()
        assertTrue(settings.autoMatchCarrierApp)
        org.junit.Assert.assertEquals("TP-Link Tether", settings.fallbackRouterAppName)
        org.junit.Assert.assertEquals("com.tplink.tether", settings.fallbackRouterPackageName)
    }

    @Test
    fun `matchCarrierForOperator resolves Bangladesh operators`() {
        org.junit.Assert.assertEquals("com.portonics.mygp", dev.zlt.companion.util.AppLauncher.matchCarrierForOperator("GrameenPhone")?.packageName)
        org.junit.Assert.assertEquals("com.skitto", dev.zlt.companion.util.AppLauncher.matchCarrierForOperator("skitto")?.packageName)
        org.junit.Assert.assertEquals("net.banglalink.mybl", dev.zlt.companion.util.AppLauncher.matchCarrierForOperator("Banglalink")?.packageName)
        org.junit.Assert.assertEquals("net.omobio.robisc", dev.zlt.companion.util.AppLauncher.matchCarrierForOperator("Robi")?.packageName)
        org.junit.Assert.assertEquals("net.omobio.airtelsc", dev.zlt.companion.util.AppLauncher.matchCarrierForOperator("Airtel")?.packageName)
        org.junit.Assert.assertEquals("teletalk.teletalkcustomerapp", dev.zlt.companion.util.AppLauncher.matchCarrierForOperator("Teletalk")?.packageName)
        org.junit.Assert.assertNull(dev.zlt.companion.util.AppLauncher.matchCarrierForOperator("UnknownTelco"))
    }
}
