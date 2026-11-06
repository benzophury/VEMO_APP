package dev.zlt.companion.net

import java.util.Calendar
import java.util.Locale

object SmsCodec {

    private val IGNORED_SPECIAL_HEX = setOf("0000", "0009")

    /**
     * Decodes a UCS-2 / UTF-16 BE hex string from ZTE/Vemo modem SMS inbox.
     * E.g. "003C0023003E" -> "<#>"
     * Also tolerates unencoded ASCII fallback.
     */
    fun decodeHex(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""

        // Check if string is 4-character hex aligned
        if (trimmed.length >= 4 && trimmed.length % 4 == 0 && trimmed.all { it.isHexDigit() }) {
            val sb = java.lang.StringBuilder()
            for (i in 0 until trimmed.length step 4) {
                val chunk = trimmed.substring(i, i + 4)
                if (chunk !in IGNORED_SPECIAL_HEX) {
                    try {
                        val code = chunk.toInt(16)
                        sb.append(code.toChar())
                    } catch (_: Exception) {
                        // ignore malformed character
                    }
                }
            }
            return sb.toString()
        }

        // Return plain text if not hex-encoded
        return trimmed
    }

    /**
     * Encodes plaintext into UCS-2 hex string for SEND_SMS.
     */
    fun encodeToHex(text: String): String {
        val sb = java.lang.StringBuilder()
        for (c in text) {
            val hex = c.code.toString(16).padStart(4, '0').uppercase(Locale.US)
            sb.append(hex)
        }
        return sb.toString()
    }

    /**
     * Converts router timestamp (e.g. "26,11,05,03,41,56,+24" or "26;11;05;03;41;56;+24")
     * into readable format: "2026-11-05 03:41:56".
     */
    fun formatRouterDate(rawDate: String): String {
        if (rawDate.isBlank()) return ""
        val cleaned = if (rawDate.contains('+')) {
            rawDate.substring(0, rawDate.lastIndexOf('+'))
        } else {
            rawDate
        }
        val parts = if (cleaned.contains(',')) cleaned.split(',') else cleaned.split(';')
        if (parts.size < 6) return rawDate
        val yy = parts[0].padStart(2, '0')
        val mm = parts[1].padStart(2, '0')
        val dd = parts[2].padStart(2, '0')
        val h = parts[3].padStart(2, '0')
        val m = parts[4].padStart(2, '0')
        val s = parts[5].padStart(2, '0')
        val year = if (yy.toIntOrNull() != null && yy.toInt() in 0..69) "20$yy" else "19$yy"
        return "$year-$mm-$dd $h:$m:$s"
    }

    /**
     * Produces router-format time string required by SEND_SMS:
     * "yy;MM;dd;HH;mm;ss;+tz"
     */
    fun formatCurrentTimeForSend(): String {
        val cal = Calendar.getInstance()
        val yy = String.format(Locale.US, "%02d", cal.get(Calendar.YEAR) % 100)
        val mm = String.format(Locale.US, "%02d", cal.get(Calendar.MONTH) + 1)
        val dd = String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH))
        val hh = String.format(Locale.US, "%02d", cal.get(Calendar.HOUR_OF_DAY))
        val min = String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))
        val ss = String.format(Locale.US, "%02d", cal.get(Calendar.SECOND))
        val offsetHours = -(cal.timeZone.rawOffset / (1000 * 60 * 60))
        val tzStr = if (offsetHours <= 0) "+${-offsetHours}" else "$offsetHours"
        return "$yy;$mm;$dd;$hh;$min;$ss;$tzStr"
    }

    private fun Char.isHexDigit(): Boolean =
        (this in '0'..'9') || (this in 'a'..'f') || (this in 'A'..'F')
}
