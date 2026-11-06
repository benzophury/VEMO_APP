package dev.zlt.companion.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsCodecTest {

    @Test
    fun `decodeHex decodes Bangla UTF-16 BE characters`() {
        // Sample from Skitto OTP live capture
        val rawHex = "003C0023003E0020098609AA09A809BE09B0"
        val decoded = SmsCodec.decodeHex(rawHex)
        assertEquals("<#> আপনার", decoded)
    }

    @Test
    fun `decodeHex filters special ignore characters 0000 and 0009`() {
        val withSpecial = "00310000003200090033"
        val decoded = SmsCodec.decodeHex(withSpecial)
        assertEquals("123", decoded)
    }

    @Test
    fun `decodeHex falls back to plain text when not hex aligned`() {
        val plain = "Normal unencoded text"
        val decoded = SmsCodec.decodeHex(plain)
        assertEquals(plain, decoded)
    }

    @Test
    fun `encodeToHex encodes text into 4-char uppercase hex`() {
        val input = "Hi!"
        val encoded = SmsCodec.encodeToHex(input)
        assertEquals("004800690021", encoded)
    }

    @Test
    fun `formatRouterDate formats router timestamp correctly`() {
        val raw = "26,11,05,03,41,56,+24"
        val formatted = SmsCodec.formatRouterDate(raw)
        assertEquals("2026-11-05 03:41:56", formatted)
    }

    @Test
    fun `formatCurrentTimeForSend produces valid router time string`() {
        val timeStr = SmsCodec.formatCurrentTimeForSend()
        // Format: yy;MM;dd;HH;mm;ss;+tz
        val parts = timeStr.split(';')
        assertTrue(parts.size >= 7)
    }
}
