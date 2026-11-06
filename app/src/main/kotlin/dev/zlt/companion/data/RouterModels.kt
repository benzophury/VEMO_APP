package dev.zlt.companion.data

/**
 * A device connected to the router's Wi-Fi / LAN.
 */
data class ConnectedStation(
    val hostname: String,
    val ip: String,
    val mac: String,
    val devType: String = "wifi",
    val ipType: String = "DHCP",
    val connectTime: String = "",
)

/**
 * An SMS message stored on the SIM card.
 */
data class SmsItem(
    val id: String,
    val number: String,
    val rawContent: String,
    val decodedContent: String,
    val date: String,
    val isUnread: Boolean,
)

/**
 * Wi-Fi AP configuration for dual-band (2.4 GHz and 5 GHz).
 */
data class WifiConfig(
    val isEnabled: Boolean = true,
    val activeBand: String = "5GHz", // "2.4GHz" or "5GHz"
    val ssid24: String = "",
    val password24: String = "",
    val authMode24: String = "WPA2PSK",
    val maxStations24: String = "10",
    val hideSsid24: Boolean = false,
    val ssid5g: String = "",
    val password5g: String = "",
    val authMode5g: String = "WPA3Personal",
    val maxStations5g: String = "10",
    val hideSsid5g: Boolean = false,
    val sleepMode: String = "-1",
    val ssid: String = if (activeBand == "5GHz" && ssid5g.isNotEmpty()) ssid5g else ssid24,
    val password: String = if (activeBand == "5GHz" && password5g.isNotEmpty()) password5g else password24,
    val authMode: String = if (activeBand == "5GHz" && authMode5g.isNotEmpty()) authMode5g else authMode24,
    val maxStations: String = if (activeBand == "5GHz" && maxStations5g.isNotEmpty()) maxStations5g else maxStations24,
    val hideSsid: Boolean = if (activeBand == "5GHz") hideSsid5g else hideSsid24,
)

/**
 * Hardware, SIM and firmware specifications.
 */
data class DeviceInfo(
    val imei: String = "",
    val imsi: String = "",
    val simImsi: String = "",
    val firmwareVersion: String = "",
    val hardwareVersion: String = "",
    val wanIp: String = "",
    val apn: String = "",
    val provider: String = "",
    val batteryPercent: String = "",
    val isCharging: Boolean = false,
)
