package dev.zlt.companion.net

/**
 * Protocol constants, ported from zlt/client.py and docs/protocol.md.
 *
 * Everything device-specific lives here, matching how the Python project keeps
 * its constants in one block at the top of client.py: a firmware that differs
 * should need changes in this file and nowhere else.
 */
object ZltProtocol {

    const val PROC_GET = "reqproc/proc_get"
    const val PROC_POST = "reqproc/proc_post"

    /** Status keys the device answers without a session. */
    val OPEN_KEYS = listOf(
        "network_type", "rssi", "signalbar", "lte_rsrq", "lte_pci", "ppp_status",
        "network_provider", "wan_ipaddr", "monthly_rx_bytes", "monthly_tx_bytes",
        "realtime_rx_bytes", "realtime_tx_bytes", "sms_unread_num", "wifi_cur_state",
    )

    /** Status keys that come back empty until logged in. */
    val AUTHED_KEYS = listOf("lte_rsrp", "lte_band", "lte_snr")

    /**
     * Keys queried to resolve the configured network mode. `net_select` is the
     * one actually populated on this hardware; the other two are kept as
     * fallbacks for other firmware builds, in the Python's resolution order.
     */
    val NET_KEYS = listOf(
        "current_network_mode", "net_select_mode", "m_netselect_save", "net_select",
    )

    const val GOFORM_LOGIN = "LOGIN"
    const val GOFORM_SET_BEARER = "SET_BEARER_PREFERENCE"
    const val GOFORM_REBOOT = "REBOOT_DEVICE"
    const val GOFORM_CONNECT_NETWORK = "CONNECT_NETWORK"
    const val GOFORM_DISCONNECT_NETWORK = "DISCONNECT_NETWORK"
    const val GOFORM_SET_WIFI_INFO = "SET_WIFI_INFO"
    const val GOFORM_SET_WIFI_SSID1_SETTINGS = "SET_WIFI_SSID1_SETTINGS"
    const val GOFORM_SET_WIFI_SLEEP = "SET_WIFI_SLEEP_INFO"
    const val GOFORM_SEND_SMS = "SEND_SMS"
    const val GOFORM_DELETE_SMS = "DELETE_SMS"
    const val GOFORM_USSD = "USSD_PROCESS"
    const val GOFORM_SET_CONNECTION_MODE = "SET_CONNECTION_MODE"
    const val GOFORM_APN = "APN_PROC_EX"
    const val GOFORM_DHCP = "DHCP_SETTING"
    const val GOFORM_CHANGE_PASSWORD = "CHANGE_PASSWORD"
    const val GOFORM_RESTORE_FACTORY = "RESTORE_FACTORY_SETTINGS"
    const val GOFORM_DATA_LIMIT_SETTING = "DATA_LIMIT_SETTING"
    const val GOFORM_FLOW_CALIBRATION_MANUAL = "FLOW_CALIBRATION_MANUAL"

    const val CMD_STATION_LIST = "station_list"
    const val CMD_SMS_DATA_TOTAL = "sms_data_total"
    const val CMD_USSD_FLAG = "ussd_write_flag"
    const val CMD_USSD_DATA = "ussd_data_info"
    const val CMD_LAN_INFO = "lan_ipaddr,lan_netmask,mac_address,dhcpEnabled,dhcpStart,dhcpEnd,dhcpLease_hour,lan_dns_mode,lan_dns_ip"
    const val CMD_NETWORK_SETTINGS = "dial_mode,roam_setting_option"
    const val CMD_DATA_LIMIT_INFO = "data_volume_limit_switch,data_volume_limit_unit,data_volume_limit_size,data_volume_alert_percent,monthly_tx_bytes,monthly_rx_bytes,monthly_time,traffic_alined_delta"
    const val CMD_WIFI_CONFIG = "wifi_band,wifi_cur_state,SSID1,WPAPSK1,WPAPSK1_encode,AuthMode,MAX_Access_num,HideSSID,SSID1_5g,WPAPSK1_5g,WPAPSK1_encode_5g,AuthMode_5g,MAX_Access_num_5g,HideSSID_5g,WIFI_HAS_5G"

    /** Attempts remaining below this and the client refuses to try at all. */
    const val MIN_ATTEMPTS_TO_TRY = 2
    const val MAX_LOGIN_COUNT = 5
    const val DEFAULT_LOCK_TIME = 300

    /** LOGIN answers "0" on a fresh login and "4" when already logged in. */
    val LOGIN_SUCCESS_RESULTS = setOf("0", "4")

    /** Non-LOGIN writes answer with this instead. */
    const val RESULT_SUCCESS = "success"

    /**
     * Best-effort markers that a write was refused for want of a session. The
     * primary authentication check is always a non-empty `get_token`; this set
     * is only a backstop that triggers one re-login and retry.
     */
    val AUTH_FAIL_MARKERS = setOf("no_session", "session_error", "need_login", "not_login", "-1")
}

/**
 * The five distinct BearerPreference values the device accepts. The Python maps
 * eight friendly aliases onto these; the app only needs the canonical five, each
 * with the label the stock web UI uses.
 *
 * Note that 3G is `TD_W`, not `Only_WCDMA` — this is called out explicitly in
 * the Python's protocol notes as a live-verified correction.
 */
enum class NetworkMode(val value: String, val label: String) {
    AUTO("NETWORK_auto", "Automatic"),
    LTE_ONLY("Only_LTE", "4G only"),
    LTE_3G("TD_W_LTE", "4G and 3G"),
    WCDMA_ONLY("TD_W", "3G only"),
    GSM_ONLY("Only_GSM", "2G only");

    companion object {
        fun fromValue(value: String): NetworkMode? = entries.firstOrNull { it.value == value }
    }
}

/** Credentials for one router. */
data class RouterCredentials(
    val host: String,
    val username: String,
    val password: String,
) {
    /** The origin the Python's Config.host holds, e.g. `http://192.168.0.1`. */
    val baseUrl: String get() = "http://$host"
}

sealed class ZltException(message: String) : Exception(message)

/** Network failure, timeout, or a response that was not JSON at all. */
class RouterUnreachable(message: String) : ZltException(message)

/**
 * The platform blocked the request because the address is not in the app's
 * permitted-cleartext list. Distinct from [RouterUnreachable] because the fix is
 * completely different: nothing is wrong with the network.
 */
class CleartextBlocked(val host: String) : ZltException(
    "Android blocked the connection to $host because it is not in the app's " +
        "list of permitted local addresses.",
)

/** Login was rejected, or could not be attempted for want of a password. */
class LoginFailed(message: String) : ZltException(message)

/** Refusing to log in: too few attempts remain before the device locks out. */
class LockedOut(
    val attemptsRemaining: Int,
    val lockSeconds: Int,
) : ZltException(
    "Only $attemptsRemaining login attempt(s) remain before the router locks " +
        "out for $lockSeconds seconds, so no attempt was made.",
)

/** The router answered a write with something other than success. */
class RouterRejected(message: String) : ZltException(message)
