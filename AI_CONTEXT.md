# AI Coding Agent Guide & Architecture Context

> **Audience**: Autonomous AI coding agents, pair-programming assistants (e.g., Claude, GPT-4, Gemini), and human software engineers maintaining or extending this codebase.
> **Repository**: ZTE Hotspot Companion (Android / Kotlin / Jetpack Compose).

---

## 1. Executive Summary & Purpose

This Android application interfaces with **ZTE / Vemo 4G MiFi Hotspot routers** (specifically ZTE U30S+, Vemo 4G, ZLT T10D MAX, and related ZXIC-based modems). It completely replaces the stock embedded WebUI with a native pure-AMOLED Material 3 client.

The modem runs a proprietary embedded HTTP server (`/reqproc/proc_get` and `/reqproc/proc_post`) over local Wi-Fi / LAN with **no public documentation**. This codebase is the production-tested reverse-engineered client.

---

## 2. Codebase Map & Directory Topology

```
app/src/main/kotlin/dev/zlt/companion/
├── data/
│   ├── ExtraModels.kt          # Secondary data models (SpeedSample, LanSettings, NetworkSettings, UssdResult)
│   ├── RouterModels.kt         # Core domain models (ConnectedStation, SmsItem, WifiConfig, DeviceInfo)
│   ├── RouterRepository.kt     # High-level business logic, poll snapshots, caching, and model translation
│   ├── RouterStatus.kt         # Live router RF readings, byte formatters, connection state machine
│   ├── SecretCipher.kt         # Android Keystore AES-256-GCM cipher for router passwords
│   ├── SettingsStore.kt        # Jetpack DataStore Preferences persistence
│   ├── SignalQuality.kt        # RSRP/RSSI RF threshold evaluators & grade levels (Excellent/Good/Fair/Poor)
│   └── StatusCache.kt          # SharedPreferences cache feeding the Quick Settings tile
├── net/
│   ├── DemoTransport.kt        # In-process OkHttp mock router engine (for testing & Play review)
│   ├── HostValidation.kt       # Strict RFC1918 / CGNAT private gateway IP validation
│   ├── SessionCookieJar.kt     # In-memory cookie store maintaining session cookies
│   ├── SmsCodec.kt             # UCS-2 / UTF-16 BE hex encoder/decoder & Bengali character codec
│   ├── ZltAuth.kt              # Nonce SHA256 hashing and Base64 password encoding
│   ├── ZltClient.kt            # Low-level OkHttp reqproc client with Mutex serialization & retry logic
│   └── ZltProtocol.kt          # Hardware command strings, keys, goformIds, and bearer preference enums
├── tile/
│   └── SignalTileService.kt    # Android Quick Settings Tile for status bar signal strength
└── ui/
    ├── DeviceInfoScreen.kt     # Hardware specs, IMEI/IMSI, WAN IP, battery level, reboot confirmation
    ├── MainActivity.kt         # Activity root, bottom navigation bar, lifecycle-aware polling launcher
    ├── ModeControl.kt          # Network generation radio mode selector UI (4G / 3G / 2G)
    ├── RouterViewModel.kt      # Central StateFlow viewmodel, rate calculation, action dispatchers
    ├── SettingsScreen.kt       # Host, username, password configuration and demo mode toggle
    ├── SignalBars.kt           # Canvas bar visualization for signal levels
    ├── SmsScreen.kt            # SMS inbox list, message compose dialog, delete confirmations
    ├── StatusScreen.kt         # Primary dashboard: live throughput, quota gauge, cellular toggle, RF cards
    ├── ToolsSettingsScreen.kt  # Network dial mode, LAN/DHCP editor, APN profiles, admin password, factory reset
    ├── WifiDevicesScreen.kt    # Wi-Fi master switch, SSID/password editor, sleep timeout, client station table
    ├── UssdScreen.kt           # Interactive USSD terminal with shortcut chips and reply input
    ├── theme/
    │   ├── AmoledColors.kt     # Pure black palette (#000000) and high-contrast neon accents
    │   └── Theme.kt            # Material 3 dark color scheme and typography definitions
    └── widgets/
        ├── DataQuotaGauge.kt   # Compose Canvas 240° sweep donut arc gauge with dynamic threshold colors
        └── LiveSpeedChart.kt   # Compose Canvas sparkline waveform plotting download/upload byte rates
```

---

## 3. Core Architectural Patterns

### 3.1 Unidirectional Data Flow (UDF)
1. **Single Source of Truth**: All screen states reside within `StatusUiState` inside [`RouterViewModel`](file:///home/sh/APK_FOR_ZTEU30SPLUS/app/src/main/kotlin/dev/zlt/companion/ui/RouterViewModel.kt).
2. **Lifecycle Awareness**: [`MainActivity`](file:///home/sh/APK_FOR_ZTEU30SPLUS/app/src/main/kotlin/dev/zlt/companion/ui/MainActivity.kt) uses `lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED)` to run the polling loop *only* when the activity is actively in the foreground. When backgrounded, polling stops instantly.
3. **Immutability**: All models in `data/` are immutable Kotlin `data class` structures. State transitions are dispatched via `_state.update { it.copy(...) }`.

### 3.2 Threading & Concurrency Model
* **Dispatcher Separation**: All network operations run strictly on `Dispatchers.IO`. UI interactions run on `Dispatchers.Main`.
* **Mutex Serialization**: [`ZltClient`](file:///home/sh/APK_FOR_ZTEU30SPLUS/app/src/main/kotlin/dev/zlt/companion/net/ZltClient.kt) uses `kotlinx.coroutines.sync.Mutex` to wrap all requests (`mutex.withLock { ... }`).
* **Why the Mutex is Mandatory**: The router's embedded web server is single-threaded. Interleaving concurrent requests crashes the router daemon, causes TCP resets, or breaks the multi-step login sequence.

### 3.3 Zero-Telemetry Security Sandbox
* **Cleartext Protection**: Cleartext HTTP is globally blocked by Android. Only gateway addresses in `app/src/main/res/xml/network_security_config.xml` are allowed.
* **IP Range Enforcement**: [`HostValidation.kt`](file:///home/sh/APK_FOR_ZTEU30SPLUS/app/src/main/kotlin/dev/zlt/companion/net/HostValidation.kt) refuses to connect to any non-RFC1918 / non-CGNAT IPv4 address.
* **Hardware KeyStore**: Stored passwords in DataStore are sealed with an AES-256-GCM cipher via [`SecretCipher.kt`](file:///home/sh/APK_FOR_ZTEU30SPLUS/app/src/main/kotlin/dev/zlt/companion/data/SecretCipher.kt).

---

## 4. Hardware & Firmware Idiosyncrasies (Gotchas)

When modifying or adding features, be aware of these firmware behaviors:

### ⚠️ 1. `psw_fail_num_str` Counts Attempts *Remaining*, NOT Failures
* On normal web services, fail counters increase. On ZTE firmwares, `psw_fail_num_str` starts at `"5"` and counts down.
* When it reaches `"0"`, the hardware enters a hard 300-second lockout.
* **The Guard**: Never bypass `MIN_ATTEMPTS_TO_TRY = 2`. If remaining attempts drop below 2, the app throws `LockedOut` and halts.

### ⚠️ 2. Password Hashing Has Two Distinct Modes
1. **Challenge-Response**: If `get_random_login` returns a nonce, the password format is:
   `Base64(ascii(lowercase_hex(sha256(nonce + password))))`.
   *Do NOT Base64-encode the raw binary digest bytes!*
2. **Direct Base64**: If no nonce is issued (common on ZTE U30S+), the password format is simply `Base64(utf8(password))`.

### ⚠️ 3. 3G Network Mode is `TD_W`, NOT `Only_WCDMA`
* Setting `BearerPreference` to `"Only_WCDMA"` is ignored or rejected by ZTE U30S+ / ZLT modems.
* The canonical parameter for 3G-only is `TD_W`.

### ⚠️ 4. Dual-Band Wi-Fi Parameter Names
* 2.4 GHz parameters use base names: `SSID1`, `WPAPSK1`, `AuthMode`, `MAX_Access_num`, `HideSSID`.
* 5.0 GHz parameters use `_5g` suffixes: `SSID1_5g`, `WPAPSK1_5g`, `AuthMode_5g`, `MAX_Access_num_5g`, `HideSSID_5g`.
* The band identifier key is `wifi_band`: `"b"` means 2.4 GHz, `"a"` means 5 GHz.

### ⚠️ 5. SMS Codec & Bengali UTF-16 BE Hex
* SMS bodies are encoded as 4-character big-endian hex chunks per character.
* **ZTE Noise Characters**: ZTE modems inject spurious `0000` (null) and `0009` (tab) sequences into message streams. The decoder in [`SmsCodec.kt`](file:///home/sh/APK_FOR_ZTEU30SPLUS/app/src/main/kotlin/dev/zlt/companion/net/SmsCodec.kt) explicitly ignores them.
* SMS deletion requires a trailing semicolon: `msg_id=31;` (omitting the semicolon fails).

### ⚠️ 6. USSD Asynchronous Polling
* Dispatched via `goformId=USSD_PROCESS`. The router returns immediately with `result="0"` or `"success"`.
* The client must repeatedly poll `cmd=ussd_write_flag` until it returns `"16"` (success), then fetch `cmd=ussd_data_info`.
* If `ussd_action` is `"1"` or `"2"`, the session remains active and the user can reply with `USSD_operator=ussd_reply`.

### ⚠️ 7. Stale Telemetry on "No Service"
* When cellular reception is lost (`network_type == "No Service"`), the modem's internal DSP continues reporting the last observed `rssi` (often `-91`) and `signalbar` (often `4`).
* Always check `status.hasService` before trusting signal bars or RSSI values.

---

## 5. Development, Testing & Verification

### 5.1 Running JVM Unit Tests
All protocol tests, authentication logic, codec tests, and mock router tests run on the JVM without an emulator:
```bash
./gradlew testDebugUnitTest
```
Key test suites:
* [`ZltAuthTest`](file:///home/sh/APK_FOR_ZTEU30SPLUS/app/src/test/kotlin/dev/zlt/companion/net/ZltAuthTest.kt): Verifies challenge-response digest vectors and Base64 password encodings.
* [`SmsCodecTest`](file:///home/sh/APK_FOR_ZTEU30SPLUS/app/src/test/kotlin/dev/zlt/companion/net/SmsCodecTest.kt): Validates Bengali UTF-16 BE hex decoding, special character filtering, and send time formatting.
* [`RouterRepositoryTest`](file:///home/sh/APK_FOR_ZTEU30SPLUS/app/src/test/kotlin/dev/zlt/companion/data/RouterRepositoryTest.kt): Validates full polling snapshots against [`RouterMock`](file:///home/sh/APK_FOR_ZTEU30SPLUS/app/src/test/kotlin/dev/zlt/companion/net/RouterMock.kt).
* [`DemoTransportTest`](file:///home/sh/APK_FOR_ZTEU30SPLUS/app/src/test/kotlin/dev/zlt/companion/net/DemoTransportTest.kt): Tests the canned in-process transport.

### 5.2 Building Debug APK
```bash
./gradlew assembleDebug
```
The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### 5.3 Testing via In-Process Demo Mode
When running the app without physical router hardware:
1. Open the app.
2. If unconfigured, tap **"Try Demo Mode"** on the initial setup screen.
3. If already configured, open Settings $\to$ Toggle **"Demo Mode"** on.
4. `DemoTransport` intercepts all `reqproc` HTTP requests in-process and returns simulated live data (including live throughput, SIM SMS, and clients).

---

## 6. Guidelines for Making Changes

1. **Keep `ZltProtocol.kt` as the single source for wire constants**:
   Never hardcode `reqproc` URLs or parameter names inside UI or repository classes.
2. **Preserve Mutex Protection**:
   Never remove `mutex.withLock` from `ZltClient.kt`.
3. **Preserve AMOLED Theme Integrity**:
   Always use `AmoledBlack` (`#000000`) for screen backgrounds. Use `DarkSurface` (`#0A0A0A`) and `DarkSurfaceBorder` (`#242424`) for container cards.
4. **Never Add External Analytics**:
   This app maintains a strict zero-telemetry boundary. Do not import Google Play Services, Firebase, or external analytics libraries.
