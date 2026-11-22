# NetSleuth — Mobile Network Diagnostics Engine

NetSleuth is a native Android network-diagnostics app written in Kotlin. It inspects the active Android network, performs real DNS resolution, probes multiple HTTPS endpoints concurrently, stores timestamped results locally, and exports a readable text report through Android's share sheet.

## Implemented features

- **Connection snapshot:** active transport (Wi-Fi, mobile data, Ethernet, VPN, other/offline), Android's validated-internet capability, metered status, local interface addresses and configured DNS server addresses.
- **DNS diagnostic:** resolves `example.com` with `InetAddress.getAllByName()` and records elapsed lookup time and returned addresses.
- **Endpoint reachability and latency:** sends real HTTPS GET requests to Google's connectivity-check endpoint, Cloudflare Trace and example.com. Each probe has connection/read timeouts and runs concurrently on an IO dispatcher.
- **Result interpretation:** reports partial failures separately and includes likely causes such as captive portals, VPNs, endpoint restrictions or firewalls. It does not claim that one HTTP request is a universal speed test.
- **Persistent history:** Room stores each scan locally with timestamp, connection state, DNS details, endpoint success count and summary. The app displays the latest 20 scans and offers a clear-history action.
- **Report export:** shares the latest detailed report using Android's Sharesheet; the user chooses where to send/save it.
- **Privacy:** diagnostics are not uploaded by the app. The only network traffic is the DNS lookup and HTTPS requests to the listed test destinations.

## Tech stack

- Kotlin, Android SDK, AndroidX AppCompat / Activity / Lifecycle
- Android `ConnectivityManager`, `NetworkCapabilities`, `LinkProperties`
- Kotlin Coroutines
- Room database

## Requirements

- JDK 17
- Android SDK Platform 35 and Build Tools installed
- Gradle 8.11.1 (the wrapper can be generated if absent)

## Build on Windows (PowerShell)

If this ZIP does not contain Gradle wrapper scripts, run this once from the project root using an installed Gradle 8.11.1:

```powershell
gradle wrapper --gradle-version 8.11.1
```

Set Java 17 for the current terminal (adjust the path if your JDK is installed elsewhere):

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.21.10002-hotspot"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
```

Build the debug APK:

```powershell
.\gradlew.bat assembleDebug
```

APK output:

`app\build\outputs\apk\debug\app-debug.apk`

Install to a connected Android device with USB debugging enabled:

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

## Important interpretation notes

- A successful endpoint request means that particular endpoint responded; it does not prove every internet service is reachable.
- Request duration includes DNS/TLS/server/network effects and is not ICMP ping or a pure radio-latency measurement.
- Android may expose different local addresses and DNS server details depending on the active transport, VPN and OS version.
- Captive portals, firewalls, VPNs, private DNS settings and endpoint-side restrictions can affect results.
- The first scan requires an active network to return useful endpoint results. An offline scan still records the detected offline state and failed probes.
- The app uses the INTERNET and ACCESS_NETWORK_STATE permissions only. It does not request location, contacts, phone state or background access.

## Project structure

```text
NetSleuth/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/rajeshwari/netsleuth/
│       │   ├── MainActivity.kt
│       │   ├── data/                 # Room entity, DAO and database
│       │   └── diagnostics/          # Probe and result models
│       └── res/values/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Validation status

This source package contains the implemented app features, but it has not been compiled or exercised on a physical Android device in this environment. Build it with the requirements above, then test Wi-Fi, mobile data, offline mode, VPN/captive portal behavior, history persistence and report sharing on-device.
