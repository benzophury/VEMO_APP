package dev.zlt.companion.net

import dev.zlt.companion.data.ConnectedStation
import dev.zlt.companion.data.DataQuotaConfig
import dev.zlt.companion.data.DeviceInfo
import dev.zlt.companion.data.LanSettings
import dev.zlt.companion.data.NetworkSettings
import dev.zlt.companion.data.SmsItem
import dev.zlt.companion.data.UssdResult
import dev.zlt.companion.data.WifiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.net.UnknownServiceException
import java.util.concurrent.TimeUnit

/**
 * The router's `reqproc` HTTP API, ported from `ZltClient` in zlt/client.py.
 *
 * Deliberately a direct port rather than a reinterpretation: the endpoint
 * shapes, the login handshake, the success codes, the lockout guard and the
 * retry rule all match the Python, which is the live-verified reference. See
 * docs/protocol.md in the zltrouter project for the captured protocol.
 *
 * All IO suspends on [Dispatchers.IO]. Requests are serialised through a mutex
 * for the same reason the Python web layer holds a lock: the device is a single
 * slow embedded box, concurrent calls buy nothing, and the login handshake is a
 * read-then-write sequence that must not interleave with another caller's.
 */
class ZltClient(
    val credentials: RouterCredentials,
    baseClient: OkHttpClient = OkHttpClient(),
    private val timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
) {

    private val cookieJar = SessionCookieJar()

    private val http: OkHttpClient = baseClient.newBuilder()
        .cookieJar(cookieJar)
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        // The router answers every call itself; a redirect would mean something
        // other than the device is on the other end.
        .followRedirects(false)
        .build()

    private val mutex = Mutex()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // --- reads ---------------------------------------------------------------

    /**
     * `GET /reqproc/proc_get?isTest=false[&multi_data=1]&cmd=<comma,joined>`.
     *
     * [multi] defaults to "more than one cmd was asked for", matching the
     * Python. [extra] carries the additional query parameters some aggregate
     * cmds take alongside `cmd`.
     */
    suspend fun get(
        vararg cmds: String,
        multi: Boolean? = null,
        extra: Map<String, String> = emptyMap(),
    ): Map<String, String> = mutex.withLock { getUnlocked(*cmds, multi = multi, extra = extra) }

    suspend fun getJson(
        vararg cmds: String,
        multi: Boolean? = null,
        extra: Map<String, String> = emptyMap(),
    ): JsonObject = mutex.withLock { getJsonUnlocked(*cmds, multi = multi, extra = extra) }

    private suspend fun getJsonUnlocked(
        vararg cmds: String,
        multi: Boolean? = null,
        extra: Map<String, String> = emptyMap(),
    ): JsonObject {
        val query = StringBuilder("isTest=false")
        query.append("&cmd=").append(cmds.joinToString("%2C") { encode(it) })
        if (multi ?: (cmds.size > 1)) query.append("&multi_data=1")
        for ((key, value) in extra) {
            query.append('&').append(encode(key)).append('=').append(encode(value))
        }

        val request = Request.Builder()
            .url("${credentials.baseUrl}/${ZltProtocol.PROC_GET}?$query")
            .headers(deviceHeaders())
            .get()
            .build()
        return parseJsonObject(execute(request))
    }

    private suspend fun getUnlocked(
        vararg cmds: String,
        multi: Boolean? = null,
        extra: Map<String, String> = emptyMap(),
    ): Map<String, String> {
        val query = StringBuilder("isTest=false")
        // The cmd separator goes out percent-encoded as %2C. That is what the
        // Python sends (requests url-encodes commas) and what the device's own
        // web UI sends (encodeURIComponent does too), so it is the form the
        // firmware is known to accept.
        query.append("&cmd=").append(cmds.joinToString("%2C") { encode(it) })
        if (multi ?: (cmds.size > 1)) query.append("&multi_data=1")
        for ((key, value) in extra) {
            query.append('&').append(encode(key)).append('=').append(encode(value))
        }

        val request = Request.Builder()
            .url("${credentials.baseUrl}/${ZltProtocol.PROC_GET}?$query")
            .headers(deviceHeaders())
            .get()
            .build()
        return parseFlatJson(execute(request))
    }

    /**
     * The CSRF token, and the authoritative "am I logged in" signal: empty means
     * no session. Both key spellings occur in the wild.
     */
    suspend fun token(): String = mutex.withLock { tokenUnlocked() }

    private suspend fun tokenUnlocked(): String {
        val data = getUnlocked("get_token")
        return data["token"] ?: data["get_token"] ?: ""
    }

    /**
     * `(attempts remaining, lockout seconds)`.
     *
     * `psw_fail_num_str` counts attempts *remaining*, not failures. An empty
     * value means the device did not say, which the Python treats as a full
     * allowance; an unparseable one is treated as zero so the guard errs toward
     * refusing to try.
     */
    suspend fun attemptsRemaining(): Pair<Int, Int> =
        mutex.withLock { attemptsRemainingUnlocked() }

    private suspend fun attemptsRemainingUnlocked(): Pair<Int, Int> {
        val data = getUnlocked("psw_fail_num_str", "login_lock_time")
        val rawAttempts = data["psw_fail_num_str"]
        val remaining = when {
            rawAttempts.isNullOrEmpty() -> ZltProtocol.MAX_LOGIN_COUNT
            else -> rawAttempts.toIntOrNull() ?: 0
        }
        val rawLock = data["login_lock_time"]
        val lock = when {
            rawLock.isNullOrEmpty() -> ZltProtocol.DEFAULT_LOCK_TIME
            else -> rawLock.toIntOrNull() ?: ZltProtocol.DEFAULT_LOCK_TIME
        }
        return remaining to lock
    }

    // --- auth ----------------------------------------------------------------

    /**
     * The four-step login from docs/protocol.md: read the lockout counters,
     * fetch the nonce, fetch the CSRF token, then POST the encoded credentials.
     *
     * The lockout read comes first and can abort before any POST is sent. That
     * ordering is the whole point of it: the device allows five failures before
     * locking out for five minutes, and there is no way to probe a password
     * safely, so a run of attempts is refused rather than made.
     */
    suspend fun login(): Map<String, String> = mutex.withLock { loginUnlocked() }

    private suspend fun loginUnlocked(): Map<String, String> {
        if (credentials.password.isEmpty()) {
            throw LoginFailed("No router password saved, so there is nothing to log in with.")
        }

        val (remaining, lock) = attemptsRemainingUnlocked()
        if (remaining < ZltProtocol.MIN_ATTEMPTS_TO_TRY) {
            throw LockedOut(remaining, lock)
        }

        val nonceData = runCatching { getUnlocked("get_random_login") }.getOrDefault(emptyMap())
        val hasRandomLoginKey = "random_login" in nonceData
        val nonce = nonceData["random_login"].orEmpty()

        if (hasRandomLoginKey && nonce.isEmpty()) {
            throw LoginFailed("The router did not hand out a login nonce.")
        }

        val fields = linkedMapOf<String, String>(
            "isTest" to "false",
            "goformId" to ZltProtocol.GOFORM_LOGIN,
        )
        if (nonce.isNotEmpty()) {
            fields["username"] = ZltAuth.encodeUsername(credentials.username)
            fields["password"] = ZltAuth.encodePassword(nonce, credentials.password)
            val token = tokenUnlocked()
            if (token.isNotEmpty()) fields["CSRFToken"] = token
        } else {
            // ZTE U30S+ / Vemo WebUI login (Base64 password directly)
            fields["password"] = ZltAuth.encodePlainPassword(credentials.password)
        }

        val data = postRaw(fields)

        val result = data["result"]
        if (result !in ZltProtocol.LOGIN_SUCCESS_RESULTS) {
            throw LoginFailed(
                "The router rejected the password. $remaining attempt(s) were " +
                    "remaining before it locks out.",
            )
        }
        return data
    }

    /** Log in only if there is no session. Checks token and/or loginfo status. */
    suspend fun ensureSession() = mutex.withLock {
        if (!isLoggedInUnlocked()) loginUnlocked()
        Unit
    }

    private suspend fun isLoggedInUnlocked(): Boolean {
        val t = tokenUnlocked()
        if (t.isNotEmpty()) return true
        val info = runCatching { getUnlocked("loginfo")["loginfo"] }.getOrNull()
        return info == "ok"
    }

    /** Drops the cached session. The next call will log in again. */
    fun forgetSession() = cookieJar.clear()

    // --- writes --------------------------------------------------------------

    /**
     * `POST /reqproc/proc_post` with a session, a fresh CSRF token, and one
     * re-login-and-retry if the device answers with an auth-failure marker.
     */
    suspend fun post(goformId: String, fields: Map<String, String> = emptyMap()): Map<String, String> =
        mutex.withLock {
            if (!isLoggedInUnlocked()) loginUnlocked()
            postWithRetry(goformId, fields, retried = false)
        }

    private suspend fun postWithRetry(
        goformId: String,
        fields: Map<String, String>,
        retried: Boolean,
    ): Map<String, String> {
        val body = LinkedHashMap<String, String>()
        body["isTest"] = "false"
        body["goformId"] = goformId
        body.putAll(fields)
        val token = tokenUnlocked()
        if (token.isNotEmpty()) {
            body.putIfAbsent("CSRFToken", token)
        }

        val data = postRaw(body)
        val result = data["result"].orEmpty().lowercase()
        if (!retried && result in ZltProtocol.AUTH_FAIL_MARKERS) {
            loginUnlocked()
            return postWithRetry(goformId, fields, retried = true)
        }
        return data
    }

    private suspend fun postRaw(fields: Map<String, String>): Map<String, String> {
        val encoded = fields.entries.joinToString("&") { (k, v) -> "${encode(k)}=${encode(v)}" }
        val request = Request.Builder()
            .url("${credentials.baseUrl}/${ZltProtocol.PROC_POST}")
            .headers(deviceHeaders())
            .post(encoded.toRequestBody(FORM_MEDIA_TYPE))
            .build()
        return parseFlatJson(execute(request))
    }

    // --- features ------------------------------------------------------------

    suspend fun getStations(): List<ConnectedStation> {
        val json = getJson(ZltProtocol.CMD_STATION_LIST, multi = false)
        val arr = json["station_list"]?.jsonArray ?: return emptyList()
        return arr.mapNotNull { elem ->
            val obj = elem.jsonObject
            ConnectedStation(
                hostname = obj["hostname"]?.jsonPrimitive?.content ?: "",
                ip = obj["ip_addr"]?.jsonPrimitive?.content ?: "",
                mac = obj["mac_addr"]?.jsonPrimitive?.content ?: "",
                devType = obj["dev_type"]?.jsonPrimitive?.content ?: "wifi",
                ipType = obj["ip_type"]?.jsonPrimitive?.content ?: "DHCP",
                connectTime = obj["connect_time"]?.jsonPrimitive?.content ?: "",
            )
        }
    }

    suspend fun getSmsMessages(page: Int = 0, perPage: Int = 50): List<SmsItem> {
        val extra = mapOf(
            "page" to page.toString(),
            "data_per_page" to perPage.toString(),
            "mem_store" to "1",
            "tags" to "10",
            "order_by" to "order by id desc",
        )
        val json = getJson(ZltProtocol.CMD_SMS_DATA_TOTAL, extra = extra)
        val arr = json["messages"]?.jsonArray ?: return emptyList()
        return arr.mapNotNull { elem ->
            val obj = elem.jsonObject
            val id = obj["id"]?.jsonPrimitive?.content ?: ""
            val number = obj["number"]?.jsonPrimitive?.content ?: ""
            val raw = obj["content"]?.jsonPrimitive?.content ?: ""
            val dateRaw = obj["date"]?.jsonPrimitive?.content ?: ""
            val tag = obj["tag"]?.jsonPrimitive?.content ?: "0"
            SmsItem(
                id = id,
                number = number,
                rawContent = raw,
                decodedContent = SmsCodec.decodeHex(raw),
                date = SmsCodec.formatRouterDate(dateRaw),
                isUnread = tag == "1",
            )
        }
    }

    suspend fun sendSms(number: String, message: String): Boolean {
        val params = mapOf(
            "Number" to number,
            "sms_time" to SmsCodec.formatCurrentTimeForSend(),
            "MessageBody" to SmsCodec.encodeToHex(message),
            "ID" to "-1",
            "encode_type" to "UNICODE",
            "notCallback" to "true",
        )
        val res = post(ZltProtocol.GOFORM_SEND_SMS, params)
        return res["result"] == ZltProtocol.RESULT_SUCCESS || res["result"] == "0"
    }

    suspend fun deleteSms(id: String): Boolean {
        val res = post(ZltProtocol.GOFORM_DELETE_SMS, mapOf("msg_id" to "$id;", "notCallback" to "true"))
        return res["result"] == ZltProtocol.RESULT_SUCCESS || res["result"] == "0"
    }

    suspend fun markSmsAsRead(ids: List<String>): Boolean {
        if (ids.isEmpty()) return true
        val msgIds = ids.joinToString(";") + ";"
        val res = post(
            ZltProtocol.GOFORM_SET_MSG_READ,
            mapOf("msg_id" to msgIds, "tag" to "0", "notCallback" to "true"),
        )
        return res["result"] == ZltProtocol.RESULT_SUCCESS || res["result"] == "0"
    }

    private fun decodePassword(raw: String?, b64: String?): String {
        if (!raw.isNullOrEmpty()) return raw
        if (!b64.isNullOrEmpty()) {
            try {
                return String(java.util.Base64.getDecoder().decode(b64), Charsets.UTF_8)
            } catch (_: Exception) {}
        }
        return ""
    }

    suspend fun getWifiConfig(): WifiConfig {
        val data = get(
            "wifi_band", "wifi_cur_state", "Sleep_interval",
            "SSID1", "WPAPSK1", "WPAPSK1_encode", "AuthMode", "MAX_Access_num", "HideSSID",
            "SSID1_5g", "WPAPSK1_5g", "WPAPSK1_encode_5g", "AuthMode_5g", "MAX_Access_num_5g", "HideSSID_5g",
        )
        val activeBand = if (data["wifi_band"] == "a") "5GHz" else "2.4GHz"
        val pwd24 = decodePassword(data["WPAPSK1"], data["WPAPSK1_encode"])
        val pwd5g = decodePassword(data["WPAPSK1_5g"], data["WPAPSK1_encode_5g"])
        val sleepMode = data["Sleep_interval"].orEmpty().ifEmpty { "-1" }

        return WifiConfig(
            isEnabled = data["wifi_cur_state"] != "0",
            activeBand = activeBand,
            ssid24 = data["SSID1"].orEmpty(),
            password24 = pwd24,
            authMode24 = data["AuthMode"].orEmpty().ifEmpty { "WPA2PSK" },
            maxStations24 = data["MAX_Access_num"].orEmpty().ifEmpty { "10" },
            hideSsid24 = data["HideSSID"] == "1",
            ssid5g = data["SSID1_5g"].orEmpty(),
            password5g = pwd5g,
            authMode5g = data["AuthMode_5g"].orEmpty().ifEmpty { "WPA3Personal" },
            maxStations5g = data["MAX_Access_num_5g"].orEmpty().ifEmpty { "10" },
            hideSsid5g = data["HideSSID_5g"] == "1",
            sleepMode = sleepMode,
        )
    }

    suspend fun setWifiEnabled(enabled: Boolean): Boolean {
        val res = post(ZltProtocol.GOFORM_SET_WIFI_INFO, mapOf("wifiEnabled" to if (enabled) "1" else "0"))
        return res["result"] == ZltProtocol.RESULT_SUCCESS || res["result"] == "0"
    }

    suspend fun setWifiBand(band: String): Boolean {
        val bandCode = if (band.contains("5")) "a" else "b"
        val res = post(ZltProtocol.GOFORM_SET_WIFI_INFO, mapOf("wifi_band" to bandCode))
        return res["result"] == ZltProtocol.RESULT_SUCCESS || res["result"] == "0"
    }

    suspend fun setWifiSsidPassword(ssid: String, password: String, authMode: String = "WPA2PSK"): Boolean {
        val res = post(
            ZltProtocol.GOFORM_SET_WIFI_SSID1_SETTINGS,
            mapOf(
                "ssid" to ssid,
                "passphrase" to ZltAuth.encodePlainPassword(password),
                "security_mode" to authMode,
            ),
        )
        return res["result"] == ZltProtocol.RESULT_SUCCESS || res["result"] == "0"
    }

    suspend fun getDataQuotaConfig(): DataQuotaConfig {
        val data = get(
            "data_volume_limit_switch",
            "data_volume_limit_unit",
            "data_volume_limit_size",
            "data_volume_alert_percent",
        )
        val isEnabled = data["data_volume_limit_switch"] == "1"
        val unitType = data["data_volume_limit_unit"].orEmpty().ifEmpty { "data" }
        val rawSize = data["data_volume_limit_size"].orEmpty()
        val parts = rawSize.split("_")
        val sizeValue = if (parts.isNotEmpty() && parts[0].isNotEmpty()) parts[0] else "50"
        val unitMultiplier = if (parts.size > 1 && parts[1].isNotEmpty()) parts[1] else "1024"
        val alertPercent = data["data_volume_alert_percent"]?.toIntOrNull() ?: 90

        val multiplier = unitMultiplier.toLongOrNull() ?: 1024L
        val valueNum = sizeValue.toLongOrNull() ?: 50L
        val totalBytes = valueNum * multiplier * 1024L * 1024L

        return DataQuotaConfig(
            isEnabled = isEnabled,
            limitValue = sizeValue,
            limitUnitMultiplier = unitMultiplier,
            limitUnitType = unitType,
            alertPercent = alertPercent,
            totalLimitBytes = totalBytes,
        )
    }

    suspend fun setDataQuotaConfig(
        enabled: Boolean,
        limitValue: String,
        unitMultiplier: String = "1024",
        alertPercent: Int = 90,
    ): Boolean {
        val limitSize = "${limitValue}_${unitMultiplier}"
        val params = mutableMapOf(
            "data_volume_limit_switch" to if (enabled) "1" else "0",
        )
        if (enabled) {
            params["data_volume_limit_unit"] = "data"
            params["data_volume_limit_size"] = limitSize
            params["data_volume_alert_percent"] = alertPercent.toString()
        }
        val res = post(ZltProtocol.GOFORM_DATA_LIMIT_SETTING, params)
        return res["result"] == ZltProtocol.RESULT_SUCCESS || res["result"] == "0"
    }

    suspend fun resetTrafficCalibration(): Boolean {
        val params = mapOf(
            "calibration_way" to "data",
            "data" to "0",
            "time" to "0",
        )
        val res = post(ZltProtocol.GOFORM_FLOW_CALIBRATION_MANUAL, params)
        return res["result"] == ZltProtocol.RESULT_SUCCESS || res["result"] == "0"
    }

    suspend fun setMobileDataConnected(connected: Boolean): Boolean {
        val goformId = if (connected) ZltProtocol.GOFORM_CONNECT_NETWORK else ZltProtocol.GOFORM_DISCONNECT_NETWORK
        val res = post(goformId, mapOf("notCallback" to "true"))
        return res["result"] == ZltProtocol.RESULT_SUCCESS || res["result"] == "0"
    }

    suspend fun getDeviceInfo(): DeviceInfo {
        val data = get(
            "imei", "imsi", "sim_imsi", "wa_inner_version", "hardware_version",
            "apn_name", "m_profile_name", "wan_ipaddr", "network_provider",
            "battery_vol_percent", "battery_charging",
        )
        return DeviceInfo(
            imei = data["imei"].orEmpty(),
            imsi = data["imsi"].orEmpty(),
            simImsi = data["sim_imsi"].orEmpty(),
            firmwareVersion = data["wa_inner_version"].orEmpty(),
            hardwareVersion = data["hardware_version"].orEmpty(),
            wanIp = data["wan_ipaddr"].orEmpty(),
            apn = data["m_profile_name"]?.ifEmpty { null } ?: data["apn_name"].orEmpty(),
            provider = data["network_provider"].orEmpty(),
            batteryPercent = data["battery_vol_percent"].orEmpty(),
            isCharging = data["battery_charging"] == "1",
        )
    }

    suspend fun sendUssd(code: String, isReply: Boolean = false): UssdResult {
        val op = if (isReply) "ussd_reply" else "ussd_send"
        val params = mutableMapOf(
            "goformId" to ZltProtocol.GOFORM_USSD,
            "USSD_operator" to op,
            "notCallback" to "true",
        )
        if (isReply) {
            params["USSD_reply_number"] = code
        } else {
            params["USSD_send_number"] = code
        }
        val postRes = post(ZltProtocol.GOFORM_USSD, params)
        if (postRes["result"] != ZltProtocol.RESULT_SUCCESS && postRes["result"] != "0") {
            return UssdResult(success = false, content = "Router rejected USSD command.")
        }

        // Poll for USSD response
        for (i in 0 until 18) {
            delay(1000)
            val flagRes = get(ZltProtocol.CMD_USSD_FLAG)
            val flag = flagRes["ussd_write_flag"].orEmpty()
            if (flag == "16") {
                val dataRes = get(ZltProtocol.CMD_USSD_DATA)
                val raw = dataRes["ussd_data"].orEmpty()
                val action = dataRes["ussd_action"].orEmpty()
                val decoded = SmsCodec.decodeHex(raw)
                return UssdResult(
                    success = true,
                    content = decoded.ifBlank { raw },
                    isSessionOpen = (action == "1" || action == "2"),
                )
            } else if (flag == "1" || flag == "2" || flag == "3" || flag == "4" || flag == "99") {
                return UssdResult(success = false, content = "USSD terminated (code $flag).")
            }
        }
        return UssdResult(success = false, content = "USSD request timed out.")
    }

    suspend fun getNetworkSettings(): NetworkSettings {
        val data = get("dial_mode", "roam_setting_option")
        return NetworkSettings(
            dialMode = data["dial_mode"].orEmpty().ifEmpty { "auto_dial" },
            isRoamingAllowed = data["roam_setting_option"] == "on",
        )
    }

    suspend fun setNetworkSettings(dialMode: String, isRoamingAllowed: Boolean): Boolean {
        val res = post(
            ZltProtocol.GOFORM_SET_CONNECTION_MODE,
            mapOf(
                "ConnectionMode" to dialMode,
                "roam_setting_option" to if (isRoamingAllowed) "on" else "off",
            ),
        )
        return res["result"] == ZltProtocol.RESULT_SUCCESS || res["result"] == "0"
    }

    suspend fun getLanSettings(): LanSettings {
        val data = get(
            "lan_ipaddr", "lan_netmask", "mac_address", "dhcpEnabled",
            "dhcpStart", "dhcpEnd", "dhcpLease_hour", "lan_dns_ip",
        )
        return LanSettings(
            ipAddress = data["lan_ipaddr"].orEmpty().ifEmpty { "192.168.0.1" },
            subnetMask = data["lan_netmask"].orEmpty().ifEmpty { "255.255.255.0" },
            isDhcpEnabled = data["dhcpEnabled"] != "0",
            dhcpStart = data["dhcpStart"].orEmpty().ifEmpty { "192.168.0.100" },
            dhcpEnd = data["dhcpEnd"].orEmpty().ifEmpty { "192.168.0.200" },
            leaseHours = data["dhcpLease_hour"].orEmpty().ifEmpty { "1" },
            dnsIp = data["lan_dns_ip"].orEmpty().ifEmpty { "1.1.1.1" },
        )
    }

    suspend fun setLanSettings(settings: LanSettings): Boolean {
        val params = mapOf(
            "lanIp" to settings.ipAddress,
            "lanNetmask" to settings.subnetMask,
            "lanDhcpType" to if (settings.isDhcpEnabled) "SERVER" else "DISABLE",
            "dhcpStart" to settings.dhcpStart,
            "dhcpEnd" to settings.dhcpEnd,
            "dhcpLease" to settings.leaseHours,
            "dhcp_reboot_flag" to "1",
        )
        val res = post(ZltProtocol.GOFORM_DHCP, params)
        return res["result"] == ZltProtocol.RESULT_SUCCESS || res["result"] == "0"
    }

    suspend fun setWifiSleepTime(minutes: String): Boolean {
        val res = post(
            ZltProtocol.GOFORM_SET_WIFI_SLEEP,
            mapOf(
                "sysIdleTimeToSleep" to minutes,
                "Sleep_interval" to minutes,
            ),
        )
        return res["result"] == ZltProtocol.RESULT_SUCCESS || res["result"] == "0"
    }

    suspend fun changeAdminPassword(oldPass: String, newPass: String): Boolean {
        val res = post(
            ZltProtocol.GOFORM_CHANGE_PASSWORD,
            mapOf(
                "oldPassword" to ZltAuth.encodePlainPassword(oldPass),
                "newPassword" to ZltAuth.encodePlainPassword(newPass),
            ),
        )
        return res["result"] == ZltProtocol.RESULT_SUCCESS || res["result"] == "0"
    }

    suspend fun factoryReset(): Boolean {
        val res = post(ZltProtocol.GOFORM_RESTORE_FACTORY, emptyMap())
        return res["result"] == ZltProtocol.RESULT_SUCCESS || res["result"] == "0"
    }

    suspend fun saveApnProfile(name: String, apn: String, authMode: String): Boolean {
        val params = mapOf(
            "apn_action" to "save",
            "apn_mode" to "manual",
            "profile_name" to name,
            "wan_apn" to apn,
            "ppp_auth_mode" to authMode,
            "pdp_type" to "IP",
            "wan_dial" to "*99#",
            "index" to "1",
        )
        val res = post(ZltProtocol.GOFORM_APN, params)
        post(
            ZltProtocol.GOFORM_APN,
            mapOf("apn_action" to "set_default", "apn_mode" to "manual", "set_default_flag" to "1", "index" to "1"),
        )
        return res["result"] == ZltProtocol.RESULT_SUCCESS || res["result"] == "0"
    }

    // --- plumbing ------------------------------------------------------------

    /** Headers copied from the device's own web UI, as the Python does. */
    private fun deviceHeaders() = okhttp3.Headers.Builder()
        .add("Referer", "${credentials.baseUrl}/index.html")
        .add("X-Requested-With", "XMLHttpRequest")
        .build()

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        try {
            http.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
        } catch (e: UnknownServiceException) {
            // Raised when the platform's network security config refuses the
            // cleartext connection. Nothing is wrong with the network, so this
            // must not be reported as an unreachable router.
            throw CleartextBlocked(credentials.host)
        } catch (e: IOException) {
            throw RouterUnreachable("Could not reach the router at ${credentials.host}.")
        }
    }

    private fun parseJsonObject(body: String): JsonObject {
        val element = try {
            json.parseToJsonElement(body)
        } catch (e: Exception) {
            throw RouterUnreachable(
                "The router sent back something that was not a valid response. " +
                    "Check that ${credentials.host} is really the router's address.",
            )
        }
        return element as? JsonObject ?: throw RouterUnreachable(
            "The router sent back an unexpected response.",
        )
    }

    /**
     * The device answers with a flat JSON object of string values. Nested values
     * (the SMS inbox is the one case) are not used here and flatten to empty
     * rather than failing the whole read.
     */
    private fun parseFlatJson(body: String): Map<String, String> {
        val obj = parseJsonObject(body)
        return obj.mapValues { (_, value) -> (value as? JsonPrimitive)?.content ?: "" }
    }

    /** quote_plus semantics, matching what Python's requests sends. */
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 8L
        private val FORM_MEDIA_TYPE =
            "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()
    }
}
