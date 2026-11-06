# VEMO APP

<p align="center">
  <strong>Modern Pure-AMOLED Material 3 Manager for Vemo 4G & ZTE U30S+ MiFi Hotspots</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_8.0+_(API_26+)-3DDC84?logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF?logo=kotlin&logoColor=white" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Theme-Pure_AMOLED_#000000-00E5FF" alt="Pure AMOLED" />
  <img src="https://img.shields.io/badge/Security-Zero_Telemetry_•_Local_Only-00E676" alt="Security" />
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License" />
</p>

---

## ⚡ Why This Exists

Portable 4G MiFi routers like the **ZTE U30S+**, **Vemo 4G**, and **ZLT T10D MAX** are ubiquitous workhorses for cellular connectivity. However, managing them is often painful:

* **Clunky 2000s Web UI**: Desktop-centric web interfaces that are non-responsive, laggy on mobile browsers, and require repetitive manual logins.
* **Hidden SMS & USSD**: Checking data balances or carrier OTPs (such as Grameenphone, Skitto, Robi, Banglalink, Airtel, or MTN) requires navigating nested, slow web menus.
* **No Real-Time Throughput**: Web dashboards show static, cumulative counters rather than live transfer waveforms.
* **Battery Drain & Lack of Dark Mode**: Bright white web pages drain mobile battery on OLED screens.

**ZTE Hotspot Companion** replaces the stock browser interface with a **native, lightning-fast, pure-AMOLED Android companion**. It talks directly to the router's embedded `reqproc` HTTP API over your local Wi-Fi connection with **zero cloud dependencies, zero analytics, and zero external tracking**.

---

## ✨ Core Features

### 🌌 Pure AMOLED (#000000) Interface
Engineered specifically for OLED/AMOLED displays to achieve true pitch-black backgrounds, infinite contrast, and maximum battery savings. Accented with vibrant neon status indicators (Cyan for download, Magenta for upload, Emerald Green for signal strength, Amber for warnings, Coral Red for critical errors).

### 📈 Live Canvas Throughput Waveforms
High-performance Compose `Canvas` sparklines rendering real-time network throughput (download $\downarrow$ and upload $\uparrow$ transfer speeds computed every poll cycle).

### 🍩 Data Quota Donut Gauge with Threshold Alerts
A custom 240-degree sweep circular donut gauge displaying monthly data usage against user-defined bandwidth caps. Features automatic color transitions:
* **Emerald Green**: $< 75\%$ usage
* **Neon Yellow / Amber**: $75\% - 90\%$ warning threshold
* **Coral Red**: $\ge 90\%$ critical quota exhaustion

### 📶 Cellular Data & Radio Management
* **Instant Cellular Data Toggle**: One-tap mobile packet data connect/disconnect.
* **Network Mode Lock**: Switch and lock radio modes between **Automatic**, **4G LTE Only**, **4G & 3G**, **3G Only**, and **2G Only** (utilizing reverse-engineered `BearerPreference` values).
* **RF Diagnostics**: Deep live telemetry including **RSRP** (dBm), **RSRQ** (dB), **RSSI** (dBm), **SNR** (dB), Physical Cell ID (**PCI**), LTE frequency **Band**, and active cellular **Operator**.

### 📡 Dual-Band Wi-Fi & Connected Client Manager
* **Dual-Band Support**: Manage 2.4 GHz ($802.11\text{b/g/n}$) and 5 GHz ($802.11\text{a/ac}$) Wi-Fi settings.
* **Master Wi-Fi Switch**: Instantly toggle the wireless access point radio on or off.
* **Credentials Management**: View, copy, or update SSID name and WPA2-PSK passphrase.
* **Sleep Timeout**: Configure Wi-Fi idle sleep timers to conserve router battery life.
* **Client Station Table**: Inspect connected clients with hostnames, IP addresses, MAC addresses, connection type (DHCP/Static), and connection durations.

### 💬 UCS-2 Bangla SIM SMS Inbox & Composer
* **SIM Card Storage Access**: Read, paginate, and manage incoming and stored SMS messages directly from the SIM card.
* **Full Bengali (Unicode UCS-2 / UTF-16 BE) Codec**: Accurately decodes and encodes native Bengali text (tested against Skitto and GrameenPhone Bengali SIM OTPs and service notifications) while stripping proprietary modem null/delimiter bytes (`0000`, `0009`).
* **Direct SMS Composer**: Send SMS messages directly from your router's SIM card to any mobile number.
* **Single-Tap Deletion**: Clean up SIM storage with one-click message deletion.

### 📟 Interactive USSD Terminal
* **Two-Way USSD Sessions**: Execute carrier USSD strings (e.g., `*121#`, `*5000#`, `*123#`) and reply to interactive multi-level service menus.
* **Asynchronous Polling Engine**: Automatically polls the router's `ussd_write_flag` state machine and decodes responses.
* **Quick Access Chips**: Pre-configured shortcut chips for common carrier balance and package inquiry codes.

### 🛠️ Hardware & Network Administration Tools
* **Device Telemetry**: Inspect device IMEI, SIM IMSI, WAN IP address, APN profile, firmware version (`wa_inner_version`), hardware revision, and live battery percentage with charging status.
* **APN Profile Editor**: Add and activate custom APN profiles for any global mobile network operator.
* **LAN & DHCP Configuration**: Customize router gateway IP, subnet mask, DHCP address pool range, and DNS server address.
* **Security & Power Controls**: Change the web admin password, execute a clean soft reboot, or perform a factory reset.

### 🛡️ Zero-Telemetry Privacy Boundary
* **Local Communication Only**: Communicates exclusively with your router gateway on RFC1918 private IPv4 subnets (e.g., `192.168.0.1`).
* **Strict Android Network Security Config**: Disallows all general cleartext HTTP traffic; explicitly whitelists only designated private gateway addresses.
* **Keystore Encrypted Credentials**: Admin passwords are sealed using hardware-backed AES-256-GCM encryption via the Android Keystore.
* **No Trackers**: Zero third-party SDKs, zero crash reporters, zero analytics. Only `android.permission.INTERNET` is declared.

### 🎮 Built-in In-Process Demo Mode
Allows exploring, testing, and reviewing every screen and feature without owning physical router hardware. The canned transport runs completely in-process via OkHttp interceptors.

---

## 🏛️ Architecture & Tech Stack

The app is built following modern Android Architecture Components and clean unidirectional data flow (UDF):

```
┌────────────────────────────────────────────────────────┐
│                   UI Layer (Jetpack Compose)           │
│  MainActivity ─ AppRoot ─ StatusScreen ─ WifiDevices   │
│  SmsScreen ─ UssdScreen ─ ToolsSettings ─ Widgets     │
└───────────────────────────▲────────────────────────────┘
                            │ StateFlow<StatusUiState>
┌───────────────────────────┴────────────────────────────┐
│                  ViewModel Layer                       │
│                   RouterViewModel                      │
│   (State orchestration, Coroutine polling, Rate math)  │
└───────────────────────────▲────────────────────────────┘
                            │
┌───────────────────────────┴────────────────────────────┐
│                 Repository Layer                       │
│                 RouterRepository                       │
│     (Snapshot caching, Model transformation, Auth)     │
└───────────────────────────▲────────────────────────────┘
                            │
┌───────────────────────────┴────────────────────────────┐
│              Network & Protocol Layer                  │
│   ZltClient ── ZltAuth ── ZltProtocol ── SmsCodec     │
│   HostValidation ── SessionCookieJar ── DemoTransport  │
└───────────────────────────▲────────────────────────────┘
                            │ HTTP (reqproc/proc_get & proc_post)
                 [ ZTE / Vemo 4G Router ]
```

* **UI Toolkit**: Jetpack Compose with Material 3 components.
* **Reactive State Management**: Kotlin Coroutines, `StateFlow`, `SharedFlow`, and Lifecycle-aware collectors (`collectAsStateWithLifecycle`).
* **Networking**: OkHttp 4 with custom `SessionCookieJar`, mutex-guarded sequential execution, and `DemoTransport` mock engine.
* **Serialization**: `kotlinx.serialization` (JSON).
* **Storage**: Android Jetpack DataStore Preferences with Android Keystore AES-GCM cipher integration (`SecretCipher`).

---

## 🚀 Building & Testing

### Prerequisites
* **Java Development Kit (JDK)**: JDK 17 or higher.
* **Android SDK**: Build tools `36.0.0`, compile SDK `36`, minimum SDK `26` (Android 8.0 Oreo).

### Build Commands

```bash
# Clone the repository
git clone https://github.com/your-username/zte-hotspot-companion.git
cd zte-hotspot-companion

# Run JVM Unit Tests (70+ test cases covering auth, protocol, SMS codec, and models)
./gradlew testDebugUnitTest

# Build Debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

### Building Release APK / App Bundle

To produce signed production builds, create `keystore.properties` from the example template:

```bash
cp keystore.properties.example keystore.properties
# Edit keystore.properties with your keystore path, alias, and credentials
```

Then run:

```bash
# Build signed release APK
./gradlew assembleRelease

# Build Google Play App Bundle (AAB)
./gradlew bundleRelease
```

---

## 📲 Wireless ADB Installation

You can install the app directly to your Android device over Wi-Fi without needing a USB cable:

1. **Enable Developer Options & Wireless Debugging**:
   * Navigate to **Settings** $\to$ **About Phone** and tap **Build Number** 7 times.
   * Go to **Settings** $\to$ **System** $\to$ **Developer Options**.
   * Toggle on **Wireless Debugging** and allow your Wi-Fi network.

2. **Pair with Wireless ADB** (Android 11+):
   * In Wireless Debugging, tap **Pair device with pairing code**.
   * Note the IP address, Port, and 6-digit Wi-Fi pairing code.
   * On your computer terminal, run:
     ```bash
     adb pair <DEVICE_IP>:<PAIRING_PORT>
     # Enter pairing code when prompted
     ```

3. **Connect to Wireless ADB**:
   ```bash
   adb connect <DEVICE_IP>:<CONNECT_PORT>
   adb devices
   ```

4. **Install the Debug APK**:
   ```bash
   ./gradlew installDebug
   # Or install the generated APK file:
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📖 Protocol Documentation

The reverse-engineered protocol documentation, authentication handshake, parameter mappings, SMS UCS-2 encoding, and state machines are documented in depth in:

👉 **[`PROTOCOL.md`](PROTOCOL.md)**

For future AI agents, code maintainers, and LLM coding assistants, architectural deep-dives and firmware idiosyncrasies are documented in:

👉 **[`AI_CONTEXT.md`](AI_CONTEXT.md)**

---

## 🔒 Security & Privacy

* **Zero Cloud Services**: This application does not connect to any servers, cloud proxies, or third-party backends.
* **Zero Telemetry**: No crash reporting (e.g. Firebase Crashlytics), analytics (e.g. Google Analytics), or behavioral tracking are bundled or used.
* **Encrypted Secrets**: Your router credentials never leave your device unencrypted. Passwords are saved encrypted with Android Keystore AES-GCM keys.
* **Strict Cleartext Whitelisting**: Android cleartext HTTP permissions are restricted strictly to private RFC1918 gateway addresses.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

> **Disclaimer**: This is an independent, community-driven reverse-engineered project. It is **not affiliated with, endorsed by, or supported by ZTE Corporation, Vemo, MTN, Grameenphone, or any mobile network operator**. ZTE and other trademarks belong to their respective owners.
