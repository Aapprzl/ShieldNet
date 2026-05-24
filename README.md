# 🛡️ ShieldNet

**ShieldNet** is a lightweight, modern, and high-performance Local VPN Ad Blocker for Android. It operates completely locally on your device without requiring root access, ensuring your privacy while blocking annoying ads and trackers system-wide.

## ✨ Features

- **No Root Required**: Utilizes Android's native `VpnService` to filter traffic locally.
- **DNS-Based Blocking**: Efficiently intercepts and drops DNS queries to known ad networks and trackers (UDP port 53) while keeping normal traffic untouched.
- **High Performance**: Designed to maintain maximum network speed (4G/5G/Wi-Fi). Non-DNS traffic (TCP/UDP) bypasses the tunnel natively, ensuring zero impact on video streaming or heavy downloads.
- **Real-Time Statistics**: Powered by Room Database and Kotlin Coroutines/Flow, providing a live dashboard of "Blocked Today" and "Total Ads Blocked".
- **Modern Jetpack Compose UI**: Features a beautiful, dark-themed, glassmorphism-inspired user interface built entirely with modern Android UI toolkits.
- **Privacy First**: Your data never leaves your device. Traffic is analyzed and blocked entirely on your phone.

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest version recommended)
- Android SDK 34+
- Kotlin 1.9+

### Installation & Build
1. Clone the repository:
   ```bash
   git clone https://github.com/Aapprzl/ShieldNet.git
   ```
2. Open the project in **Android Studio**.
3. Let Gradle sync and resolve all dependencies.
4. Build and run the app on your emulator or physical Android device:
   - Click the **Run** button or use the terminal:
     ```bash
     ./gradlew assembleDebug
     ```

## 🛠️ Architecture & Tech Stack

- **Kotlin**: 100% written in Kotlin.
- **Jetpack Compose**: For the entire UI layer, providing a reactive and smooth user experience.
- **MVVM Architecture**: Clean separation of concerns using ViewModels.
- **Room Database**: For storing and querying real-time ad-blocking statistics.
- **Kotlin Coroutines & Flow**: For asynchronous operations and reactive data streams.
- **Android VpnService**: The core engine for intercepting and routing network packets.

## 💡 How It Works
ShieldNet sets up a local VPN interface (`10.0.0.2`) and routes all DNS traffic (`10.0.0.1`) through the app. 
1. The `PacketInterceptor` catches DNS requests.
2. It checks the requested domain against a local blocklist.
3. If the domain is an ad/tracker, it responds with `0.0.0.0` (dropping the request).
4. If it's a safe domain, it forwards the query to a fast upstream DNS (e.g., Google DNS `8.8.8.8`) via a concurrent `DatagramSocket`.
5. All other normal internet traffic bypasses the VPN tunnel natively for zero latency overhead.

## 📄 License
This project is open-sourced under the MIT License.

---
*Created with ❤️ by [Aapprzl](https://github.com/Aapprzl)*
