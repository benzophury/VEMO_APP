package dev.zlt.companion.data

data class SpeedSample(
    val timestamp: Long,
    val rxBytesPerSec: Long,
    val txBytesPerSec: Long,
)

data class NetworkSettings(
    val dialMode: String = "auto_dial",
    val isRoamingAllowed: Boolean = true,
)

data class LanSettings(
    val ipAddress: String = "192.168.0.1",
    val subnetMask: String = "255.255.255.0",
    val isDhcpEnabled: Boolean = true,
    val dhcpStart: String = "192.168.0.100",
    val dhcpEnd: String = "192.168.0.200",
    val leaseHours: String = "1",
    val dnsIp: String = "1.1.1.1",
)

data class UssdResult(
    val success: Boolean,
    val content: String,
    val isSessionOpen: Boolean = false,
)

data class DataQuotaConfig(
    val isEnabled: Boolean = false,
    val limitValue: String = "50",
    val limitUnitMultiplier: String = "1024", // "1024" for GB, "1" for MB
    val limitUnitType: String = "data", // "data" or "time"
    val alertPercent: Int = 90,
    val totalLimitBytes: Long = 50L * 1024L * 1024L * 1024L,
)
