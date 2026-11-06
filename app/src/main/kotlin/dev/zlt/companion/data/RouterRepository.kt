package dev.zlt.companion.data

import dev.zlt.companion.net.DemoTransport
import dev.zlt.companion.net.LockedOut
import dev.zlt.companion.net.LoginFailed
import dev.zlt.companion.net.NetworkMode
import dev.zlt.companion.net.RouterCredentials
import dev.zlt.companion.net.RouterRejected
import dev.zlt.companion.net.ZltClient
import dev.zlt.companion.net.ZltProtocol
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One poll's worth of everything the screen shows. */
data class RouterSnapshot(
    val status: RouterStatus,
    /** Null when there was no session to read it with. */
    val mode: NetModeState?,
)

/**
 * Owns the [ZltClient] and turns raw key/value reads into the app's models.
 *
 * The client is rebuilt whenever the saved credentials change, which also drops
 * the session cookie — the old one belongs to a different router.
 *
 * Takes a credentials provider rather than a [SettingsStore] so the whole class
 * is exercisable on the JVM against a mock router. Use [from] to build one
 * backed by the real settings.
 */
class RouterRepository(
    private val credentialsProvider: suspend () -> RouterCredentials,
    private val clientFactory: (RouterCredentials) -> ZltClient = { ZltClient(it) },
) {

    private val mutex = Mutex()
    private var cachedClient: ZltClient? = null
    private var cachedCredentials: RouterCredentials? = null

    // The credentials read happens inside the lock so that it and the factory
    // call are atomic with respect to each other. [from] relies on that: it
    // passes demo-ness from one to the other, and an interleaved caller would
    // otherwise be able to pair one poll's flag with another poll's credentials.
    private suspend fun client(): ZltClient = mutex.withLock {
        val credentials = credentialsProvider()
        val existing = cachedClient
        if (existing != null && cachedCredentials == credentials) {
            existing
        } else {
            clientFactory(credentials).also {
                cachedClient = it
                cachedCredentials = credentials
            }
        }
    }

    /**
     * One poll: signal, connection state and configured mode together.
     *
     * The status keys and the network-mode keys go out as a single `proc_get`.
     * They are all plain scalar keys and `multi_data` already joins as many as
     * are asked for, so there is no protocol reason to make two round trips —
     * and making two was a real bug. Reading the mode only on first load meant
     * a mode changed from the router's own web UI never appeared here, which
     * looks exactly like the app misreporting the signal.
     *
     * Falling back to the open keys when login fails is ported from cli.py:
     * losing RSRP, band and SNR still leaves a usable RSSI reading, which beats
     * an error screen. The mode is unreadable without a session, so it comes
     * back null there rather than stale.
     */
    suspend fun snapshot(): RouterSnapshot {
        val client = client()
        var note: String? = null
        val full = try {
            client.ensureSession()
            client.get(
                *(ZltProtocol.OPEN_KEYS + ZltProtocol.AUTHED_KEYS + ZltProtocol.NET_KEYS)
                    .toTypedArray(),
            )
        } catch (e: LoginFailed) {
            note = e.message
            null
        } catch (e: LockedOut) {
            note = e.message
            null
        }

        if (full == null) {
            val open = client.get(*ZltProtocol.OPEN_KEYS.toTypedArray())
            return RouterSnapshot(
                status = RouterStatus.from(open, authed = false, note = note),
                mode = null,
            )
        }

        // If the device answered the status keys but dropped the mode keys
        // entirely, it will not serve this many cmds in one read. Ask for them
        // separately rather than silently reporting no mode. Note the test for
        // *absence*: a key present but empty is a real answer ("nothing is
        // configured") and must not trigger a second round trip every poll.
        val mode = if (ZltProtocol.NET_KEYS.none { it in full }) {
            runCatching { client.get(*ZltProtocol.NET_KEYS.toTypedArray()) }
                .map { NetModeState.from(it) }
                .getOrNull()
        } else {
            NetModeState.from(full)
        }

        return RouterSnapshot(
            status = RouterStatus.from(full, authed = true, note = null),
            mode = mode,
        )
    }

    /**
     * Sets the network mode and reads back what actually stuck.
     *
     * Ported from `net_set` in zlt/cli.py, including the read-back: the device
     * accepting the write is not the same as the device having applied it, and
     * the returned state is what the router now reports rather than what we
     * asked for. Note this write answers `"success"`, not the `"0"` that LOGIN
     * uses.
     */
    suspend fun setMode(mode: NetworkMode): NetModeState {
        val client = client()
        val result = client.post(
            ZltProtocol.GOFORM_SET_BEARER,
            mapOf("BearerPreference" to mode.value),
        )
        if (result["result"] != ZltProtocol.RESULT_SUCCESS && result["result"] != "0") {
            throw RouterRejected(
                "The router would not change the network mode " +
                    "(it answered \"${result["result"].orEmpty()}\").",
            )
        }
        return NetModeState.from(client.get(*ZltProtocol.NET_KEYS.toTypedArray()))
    }

    suspend fun reboot() {
        val client = client()
        client.post(ZltProtocol.GOFORM_REBOOT, emptyMap())
    }

    suspend fun getStations(): List<ConnectedStation> = client().getStations()

    suspend fun getSmsMessages(): List<SmsItem> = client().getSmsMessages()

    suspend fun sendSms(number: String, message: String): Boolean = client().sendSms(number, message)

    suspend fun deleteSms(id: String): Boolean = client().deleteSms(id)

    suspend fun markSmsAsRead(ids: List<String>): Boolean = client().markSmsAsRead(ids)

    suspend fun getWifiConfig(): WifiConfig = client().getWifiConfig()

    suspend fun setWifiEnabled(enabled: Boolean): Boolean = client().setWifiEnabled(enabled)

    suspend fun setWifiBand(band: String): Boolean = client().setWifiBand(band)

    suspend fun setWifiSsidPassword(ssid: String, password: String, authMode: String = "WPA2PSK"): Boolean =
        client().setWifiSsidPassword(ssid, password, authMode)

    suspend fun getDataQuotaConfig(): DataQuotaConfig = client().getDataQuotaConfig()

    suspend fun setDataQuotaConfig(
        enabled: Boolean,
        limitValue: String,
        unitMultiplier: String = "1024",
        alertPercent: Int = 90,
    ): Boolean = client().setDataQuotaConfig(enabled, limitValue, unitMultiplier, alertPercent)

    suspend fun resetTrafficCalibration(): Boolean = client().resetTrafficCalibration()

    suspend fun setMobileDataConnected(connect: Boolean): Boolean = client().setMobileDataConnected(connect)

    suspend fun getDeviceInfo(): DeviceInfo = client().getDeviceInfo()

    suspend fun sendUssd(code: String, isReply: Boolean = false): UssdResult =
        client().sendUssd(code, isReply)

    suspend fun getNetworkSettings(): NetworkSettings = client().getNetworkSettings()

    suspend fun setNetworkSettings(dialMode: String, isRoamingAllowed: Boolean): Boolean =
        client().setNetworkSettings(dialMode, isRoamingAllowed)

    suspend fun getLanSettings(): LanSettings = client().getLanSettings()

    suspend fun setLanSettings(settings: LanSettings): Boolean =
        client().setLanSettings(settings)

    suspend fun setWifiSleepTime(minutes: String): Boolean =
        client().setWifiSleepTime(minutes)

    suspend fun changeAdminPassword(oldPass: String, newPass: String): Boolean =
        client().changeAdminPassword(oldPass, newPass)

    suspend fun factoryReset(): Boolean =
        client().factoryReset()

    suspend fun saveApnProfile(name: String, apn: String, authMode: String): Boolean =
        client().saveApnProfile(name, apn, authMode)

    companion object {
        /**
         * In demo mode every request is answered in-process by [DemoTransport],
         * so no traffic leaves the app and no router is needed. The seam is the
         * client's `baseClient`, which means everything above it — session,
         * parsing, this repository — behaves identically either way.
         */
        fun from(settingsStore: SettingsStore): RouterRepository {
            // Carried from the credentials read to the factory call, which
            // client() performs atomically under its lock.
            //
            // Deliberately not inferred by comparing against
            // DemoTransport.CREDENTIALS: someone whose router really is at
            // 192.168.0.1 with the password "demo" would then be shown sample
            // readings, with no banner to say so, as though they were real.
            var demo = false
            return RouterRepository(
                credentialsProvider = {
                    val settings = settingsStore.current()
                    demo = settings.demoMode
                    if (demo) DemoTransport.CREDENTIALS else settings.credentials()
                },
                clientFactory = { credentials ->
                    if (demo) {
                        ZltClient(credentials, baseClient = DemoTransport.okHttpClient())
                    } else {
                        ZltClient(credentials)
                    }
                },
            )
        }
    }
}
