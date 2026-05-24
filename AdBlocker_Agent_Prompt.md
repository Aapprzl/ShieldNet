# 🛡️ AGENT BUILD PROMPT — Android Local VPN Ad Blocker
> **Untuk:** AI Coding Agent (Google Antigravity)
> **Dari:** Arsitek Aplikasi
> **Target Device:** Samsung Galaxy A56 (SM-A566B) — Android 14+, RAM 7.4 GB, Exynos 1580
> **Tujuan:** Membangun aplikasi Android native yang memblokir iklan di SEMUA aplikasi menggunakan mekanisme Local VPN (tanpa root, tanpa server eksternal)

---

## 📌 INTRO & KONTEKS

Aplikasi ini bernama **"ShieldNet"** — sebuah ad blocker lokal berbasis `VpnService` Android. Cara kerjanya adalah dengan membuat tunnel VPN palsu di dalam device sendiri (tidak ada data yang keluar ke server manapun), lalu menyaring semua paket jaringan yang masuk/keluar untuk mendeteksi dan memblokir domain/IP yang dikenal sebagai sumber iklan, tracker, dan malware.

### Mengapa Local VPN?
- **Tidak perlu root** — Android secara resmi menyediakan `VpnService` API
- **Efektif untuk semua app** — karena semua traffic melewati tunnel
- **Private** — tidak ada data yang dikirim ke server luar
- **Kompatibel** — bekerja di Android 9+ termasuk Android 14 (target device)

### Stack Teknologi
- **Bahasa:** Kotlin
- **Min SDK:** 26 (Android 8.0) | **Target SDK:** 35 (Android 14+)
- **Build System:** Gradle (Kotlin DSL)
- **UI:** Jetpack Compose + Material Design 3
- **Architecture:** MVVM + Clean Architecture
- **Background Service:** Foreground Service + VpnService
- **Database:** Room (untuk log & statistik)
- **Blocklist Format:** Hosts file format (domain-based)

---

## 🗂️ STRUKTUR PROYEK

```

├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── kotlin/com/shieldnet/
│   │   │   ├── MainActivity.kt
│   │   │   ├── ui/
│   │   │   │   ├── theme/                  # Compose theme & colors
│   │   │   │   ├── screens/
│   │   │   │   │   ├── HomeScreen.kt       # Toggle VPN + statistik
│   │   │   │   │   ├── LogScreen.kt        # Log domain yang diblokir
│   │   │   │   │   └── SettingsScreen.kt   # Konfigurasi & update blocklist
│   │   │   │   └── components/             # Reusable Compose components
│   │   │   ├── vpn/
│   │   │   │   ├── ShieldVpnService.kt     # Core VPN service
│   │   │   │   ├── PacketInterceptor.kt    # Proses & filter paket
│   │   │   │   ├── DnsPacketParser.kt      # Parse DNS query dari paket
│   │   │   │   └── TcpUdpForwarder.kt      # Forward paket yang lolos filter
│   │   │   ├── blocklist/
│   │   │   │   ├── BlocklistManager.kt     # Load & manage daftar domain blokir
│   │   │   │   ├── BlocklistParser.kt      # Parse file hosts format
│   │   │   │   └── BlocklistUpdater.kt     # Update blocklist dari internet
│   │   │   ├── data/
│   │   │   │   ├── db/
│   │   │   │   │   ├── AppDatabase.kt      # Room database
│   │   │   │   │   ├── BlockedLogDao.kt    # DAO untuk log blokir
│   │   │   │   │   └── BlockedLogEntity.kt # Entity tabel log
│   │   │   │   └── repository/
│   │   │   │       └── LogRepository.kt
│   │   │   ├── viewmodel/
│   │   │   │   ├── HomeViewModel.kt
│   │   │   │   ├── LogViewModel.kt
│   │   │   │   └── SettingsViewModel.kt
│   │   │   └── utils/
│   │   │       ├── NetworkUtils.kt
│   │   │       └── Constants.kt
│   │   └── res/
│   │       ├── raw/
│   │       │   └── blocklist_default.txt   # Bundled blocklist (hosts format)
│   │       └── xml/
│   │           └── network_security_config.xml
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 📋 STEP-BY-STEP IMPLEMENTASI

### STEP 1 — Setup Proyek & Konfigurasi Gradle

**1.1 Buat proyek Android baru di Android Studio (atau generate via CLI)**
- Package name: `com.shieldnet`
- Language: Kotlin
- Min SDK: 26

**1.2 `build.gradle.kts` (app level) — tambahkan dependensi:**

```kotlin
dependencies {
    // Jetpack Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // DataStore (untuk preferences/settings)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // OkHttp (untuk update blocklist)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
```

**1.3 `AndroidManifest.xml` — izin yang WAJIB ada:**

```xml
<manifest>
    <!-- Izin wajib untuk VPN -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application ...>

        <!-- VPN Service — WAJIB ada meta-data ini -->
        <service
            android:name=".vpn.ShieldVpnService"
            android:exported="false"
            android:foregroundServiceType="specialUse"
            android:permission="android.permission.BIND_VPN_SERVICE">
            <intent-filter>
                <action android:name="android.net.VpnService" />
            </intent-filter>
            <meta-data
                android:name="android.net.VpnService.SUPPORTS_ALWAYS_ON"
                android:value="true" />
        </service>

        <!-- Boot receiver untuk auto-start -->
        <receiver
            android:name=".BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

    </application>
</manifest>
```

---

### STEP 2 — Core VPN Service (`ShieldVpnService.kt`)

Ini adalah **komponen terpenting** aplikasi. Berikut logika lengkapnya:

```kotlin
class ShieldVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private lateinit var blocklistManager: BlocklistManager
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
        }
        return START_STICKY // Restart otomatis jika di-kill sistem
    }

    private fun startVpn() {
        // 1. Load blocklist ke memory (HashSet untuk lookup O(1))
        blocklistManager = BlocklistManager(this)
        blocklistManager.loadBlocklist()

        // 2. Build VPN interface
        val builder = Builder()
            .setSession("ShieldNet")
            .addAddress("10.0.0.2", 32)          // IP virtual device
            .addRoute("0.0.0.0", 0)               // Route SEMUA traffic IPv4
            .addRoute("::", 0)                    // Route SEMUA traffic IPv6
            .addDnsServer("10.0.0.1")             // DNS virtual (kita yang handle)
            .setMtu(1500)
            .setBlocking(false)                   // Non-blocking I/O

        vpnInterface = builder.establish()
        isRunning = true

        // 3. Tampilkan notifikasi foreground
        showForegroundNotification()

        // 4. Mulai packet processing di coroutine
        CoroutineScope(Dispatchers.IO).launch {
            processPackets()
        }
    }

    private suspend fun processPackets() {
        val fileDescriptor = vpnInterface?.fileDescriptor ?: return
        val inputStream = FileInputStream(fileDescriptor)
        val outputStream = FileOutputStream(fileDescriptor)

        val packetBuffer = ByteArray(32767) // Buffer max MTU size

        while (isRunning) {
            val length = inputStream.read(packetBuffer)
            if (length <= 0) {
                delay(10)
                continue
            }

            val packet = packetBuffer.copyOf(length)

            // Cek apakah ini DNS query (UDP port 53)
            if (isDnsPacket(packet)) {
                handleDnsPacket(packet, outputStream)
            } else {
                // Bukan DNS — forward langsung (koneksi biasa)
                outputStream.write(packet)
            }
        }
    }

    private fun handleDnsPacket(packet: ByteArray, outputStream: FileOutputStream) {
        val dnsQuery = DnsPacketParser.parse(packet)
        val domain = dnsQuery?.queryDomain ?: return

        if (blocklistManager.isBlocked(domain)) {
            // Domain ada di blocklist — balas dengan NXDOMAIN (domain not found)
            // Ini memblokir iklan tanpa crash app
            val nxDomainResponse = DnsPacketParser.buildNxDomainResponse(dnsQuery)
            outputStream.write(nxDomainResponse)

            // Catat ke log database
            logBlockedDomain(domain)
        } else {
            // Domain aman — forward DNS query ke DNS upstream
            forwardDnsQuery(packet, outputStream)
        }
    }

    private fun stopVpn() {
        isRunning = false
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val ACTION_START = "com.shieldnet.START_VPN"
        const val ACTION_STOP = "com.shieldnet.STOP_VPN"
    }
}
```

> ⚠️ **CATATAN AGENT:** Implementasi `DnsPacketParser` harus mampu:
> - Parse header DNS (RFC 1035)
> - Ekstrak `QNAME` (query domain) dari section Questions
> - Build response NXDOMAIN dengan Transaction ID yang sama dari query asal
> - Handle DNS over UDP dan TCP

---

### STEP 3 — Blocklist Manager (`BlocklistManager.kt`)

```kotlin
class BlocklistManager(private val context: Context) {

    // HashSet untuk lookup super cepat O(1) — penting untuk performa real-time
    private val blockedDomains = HashSet<String>(150_000)

    fun loadBlocklist() {
        // Load dari file bundled di res/raw/
        context.resources.openRawResource(R.raw.blocklist_default).bufferedReader().use { reader ->
            reader.lineSequence()
                .filter { line ->
                    line.isNotBlank() &&
                    !line.startsWith("#") && // Skip komentar
                    !line.startsWith("!") &&
                    line.startsWith("0.0.0.0") // Format hosts file
                }
                .forEach { line ->
                    // Format: "0.0.0.0 ads.example.com"
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        blockedDomains.add(parts[1].lowercase())
                    }
                }
        }

        // Juga load custom blocklist dari user jika ada
        loadUserCustomBlocklist()
    }

    fun isBlocked(domain: String): Boolean {
        val lowerDomain = domain.lowercase().trimEnd('.')

        // Cek exact match
        if (blockedDomains.contains(lowerDomain)) return true

        // Cek subdomain match (misal: "a.ads.google.com" diblokir jika "ads.google.com" ada di list)
        val parts = lowerDomain.split(".")
        for (i in 1 until parts.size - 1) {
            val parentDomain = parts.subList(i, parts.size).joinToString(".")
            if (blockedDomains.contains(parentDomain)) return true
        }

        return false
    }

    fun addCustomDomain(domain: String) {
        blockedDomains.add(domain.lowercase())
        saveUserCustomBlocklist()
    }

    fun removeCustomDomain(domain: String) {
        blockedDomains.remove(domain.lowercase())
        saveUserCustomBlocklist()
    }

    private fun loadUserCustomBlocklist() { /* Load dari SharedPreferences/DataStore */ }
    private fun saveUserCustomBlocklist() { /* Simpan ke SharedPreferences/DataStore */ }
}
```

**Sumber blocklist default yang harus di-bundle (`res/raw/blocklist_default.txt`):**
- Gunakan **StevenBlack Unified Hosts** dari: `https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts`
- File ini berisi 100.000+ domain iklan & tracker dalam format hosts
- Unduh saat build time atau sertakan versi snapshot terbaru

---

### STEP 4 — DNS Packet Parser (`DnsPacketParser.kt`)

```kotlin
object DnsPacketParser {

    data class DnsQuery(
        val transactionId: Short,
        val queryDomain: String,
        val queryType: Short,
        val rawPacket: ByteArray,
        val ipHeader: ByteArray,
        val udpHeader: ByteArray
    )

    fun parse(rawPacket: ByteArray): DnsQuery? {
        return try {
            val buffer = ByteBuffer.wrap(rawPacket)

            // Parse IP Header (20 bytes untuk IPv4)
            val ipVersion = (rawPacket[0].toInt() and 0xF0) shr 4
            if (ipVersion != 4) return null // Hanya handle IPv4 dulu

            val ipHeaderLength = (rawPacket[0].toInt() and 0x0F) * 4
            val protocol = rawPacket[9].toInt() and 0xFF
            if (protocol != 17) return null // 17 = UDP

            // Parse UDP Header
            val udpOffset = ipHeaderLength
            val destPort = ((rawPacket[udpOffset + 2].toInt() and 0xFF) shl 8) or
                           (rawPacket[udpOffset + 3].toInt() and 0xFF)
            if (destPort != 53) return null // Bukan DNS

            // Parse DNS Payload
            val dnsOffset = udpOffset + 8
            val dnsBuffer = ByteBuffer.wrap(rawPacket, dnsOffset, rawPacket.size - dnsOffset)

            val transactionId = dnsBuffer.short
            dnsBuffer.short // Flags
            val questionCount = dnsBuffer.short.toInt() and 0xFFFF
            dnsBuffer.short // Answer count
            dnsBuffer.short // Authority count
            dnsBuffer.short // Additional count

            if (questionCount == 0) return null

            // Parse QNAME
            val domain = parseDnsName(rawPacket, dnsOffset + 12)
            val queryType = dnsBuffer.short

            DnsQuery(
                transactionId = transactionId,
                queryDomain = domain,
                queryType = queryType,
                rawPacket = rawPacket,
                ipHeader = rawPacket.copyOfRange(0, ipHeaderLength),
                udpHeader = rawPacket.copyOfRange(udpOffset, udpOffset + 8)
            )
        } catch (e: Exception) {
            null // Paket tidak valid, abaikan
        }
    }

    private fun parseDnsName(packet: ByteArray, startOffset: Int): String {
        val sb = StringBuilder()
        var offset = startOffset
        while (offset < packet.size) {
            val length = packet[offset].toInt() and 0xFF
            if (length == 0) break
            if (sb.isNotEmpty()) sb.append('.')
            sb.append(String(packet, offset + 1, length, Charsets.US_ASCII))
            offset += length + 1
        }
        return sb.toString()
    }

    fun buildNxDomainResponse(query: DnsQuery): ByteArray {
        // Build DNS response dengan RCODE = 3 (NXDOMAIN)
        // Header: Transaction ID sama, QR=1, RCODE=3
        val dnsResponse = ByteBuffer.allocate(512)
        dnsResponse.putShort(query.transactionId)
        dnsResponse.putShort(0x8183.toShort()) // Response, Recursion Desired, NXDOMAIN
        dnsResponse.putShort(1)  // Question count
        dnsResponse.putShort(0)  // Answer count = 0
        dnsResponse.putShort(0)  // Authority count = 0
        dnsResponse.putShort(0)  // Additional count = 0

        // Tulis ulang question section dari query asli
        val questionBytes = buildQuestionSection(query.queryDomain, query.queryType)
        dnsResponse.put(questionBytes)

        val dnsPayload = dnsResponse.array().copyOf(dnsResponse.position())

        // Wrap dengan UDP dan IP header (swap src/dst)
        return wrapWithIpUdp(dnsPayload, query)
    }

    private fun buildQuestionSection(domain: String, queryType: Short): ByteArray { /* ... */ return ByteArray(0) }
    private fun wrapWithIpUdp(payload: ByteArray, query: DnsQuery): ByteArray { /* ... */ return ByteArray(0) }
}
```

> ⚠️ **CATATAN AGENT:** Lengkapi fungsi `buildQuestionSection` dan `wrapWithIpUdp`. Untuk `wrapWithIpUdp`, source dan destination IP/port harus di-swap (karena ini adalah reply), dan checksum IP + UDP harus dihitung ulang dengan benar.

---

### STEP 5 — UI dengan Jetpack Compose (`HomeScreen.kt`)

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val vpnState by viewModel.vpnState.collectAsState()
    val blockedCount by viewModel.blockedCount.collectAsState()
    val context = LocalContext.current

    // VPN Permission Launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startVpn(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Status indicator
        Text(
            text = if (vpnState == VpnState.CONNECTED) "🛡️ Protected" else "⚠️ Unprotected",
            style = MaterialTheme.typography.headlineMedium,
            color = if (vpnState == VpnState.CONNECTED) Color(0xFF4CAF50) else Color(0xFFF44336)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Big toggle button
        Button(
            onClick = {
                if (vpnState == VpnState.CONNECTED) {
                    viewModel.stopVpn(context)
                } else {
                    // Minta izin VPN ke user (muncul dialog sistem)
                    val intent = VpnService.prepare(context)
                    if (intent != null) {
                        vpnPermissionLauncher.launch(intent)
                    } else {
                        viewModel.startVpn(context)
                    }
                }
            },
            modifier = Modifier.size(160.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (vpnState == VpnState.CONNECTED)
                    Color(0xFF4CAF50) else Color(0xFF2196F3)
            )
        ) {
            Text(
                text = if (vpnState == VpnState.CONNECTED) "TAP TO\nSTOP" else "TAP TO\nSTART",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Statistik
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ads Blocked Today", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "$blockedCount",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
```

---

### STEP 6 — Room Database untuk Log (`BlockedLogEntity.kt` & DAO)

```kotlin
// Entity
@Entity(tableName = "blocked_logs")
data class BlockedLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val timestamp: Long = System.currentTimeMillis(),
    val appPackage: String? = null // Opsional: package app yang mencoba akses
)

// DAO
@Dao
interface BlockedLogDao {
    @Query("SELECT * FROM blocked_logs ORDER BY timestamp DESC LIMIT 500")
    fun getRecentLogs(): Flow<List<BlockedLogEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: BlockedLogEntity)

    @Query("SELECT COUNT(*) FROM blocked_logs WHERE timestamp > :since")
    fun getCountSince(since: Long): Flow<Int>

    @Query("DELETE FROM blocked_logs WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
```

---

### STEP 7 — Settings Screen & Blocklist Updater

**Settings yang harus ada:**

| Setting | Tipe | Default |
|---|---|---|
| Auto-start on boot | Toggle | OFF |
| Update blocklist otomatis | Toggle | ON |
| Interval update blocklist | Dropdown | Mingguan |
| DNS Upstream | Input | 1.1.1.1 (Cloudflare) |
| Blokir IPv6 juga | Toggle | ON |
| Simpan log | Toggle | ON |
| Hapus log otomatis | Dropdown | Setelah 7 hari |

**`BlocklistUpdater.kt` — update blocklist dari internet:**

```kotlin
class BlocklistUpdater(private val context: Context) {

    private val BLOCKLIST_URL =
        "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"

    suspend fun updateBlocklist(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(BLOCKLIST_URL).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return@withContext Result.failure(
                IOException("HTTP ${response.code}")
            )

            val content = response.body?.string() ?: return@withContext Result.failure(
                IOException("Empty response")
            )

            // Simpan ke file internal
            val file = File(context.filesDir, "blocklist_updated.txt")
            file.writeText(content)

            // Hitung jumlah domain
            val count = content.lines()
                .count { it.startsWith("0.0.0.0") && !it.startsWith("0.0.0.0 0.0.0.0") }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

### STEP 8 — Boot Receiver (Auto-start)

```kotlin
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Cek apakah user mengaktifkan auto-start di settings
            val prefs = context.getSharedPreferences("shieldnet_prefs", Context.MODE_PRIVATE)
            val autoStart = prefs.getBoolean("auto_start_on_boot", false)

            if (autoStart) {
                val vpnIntent = Intent(context, ShieldVpnService::class.java).apply {
                    action = ShieldVpnService.ACTION_START
                }
                context.startForegroundService(vpnIntent)
            }
        }
    }
}
```

---

## ⚡ OPTIMASI PERFORMA (Penting untuk Real-time Packet Processing)

Karena device target punya RAM 7.4 GB, kita bisa agresif:

1. **HashSet lookup** untuk blocklist — O(1), bukan list O(n)
2. **ByteBuffer** bukan String manipulation untuk parsing paket
3. **Coroutine Dispatchers.IO** untuk packet loop agar tidak block main thread
4. **Pre-allocate buffer** 32KB sekali di awal, reuse terus (hindari GC pressure)
5. **Batch insert** ke Room database (jangan insert per-paket, kumpulkan 50 lalu insert sekaligus)
6. **Memory limit:** BlocklistManager menggunakan ~15MB RAM untuk 100K domain di HashSet — aman untuk device ini

---

## 🔒 CATATAN KEAMANAN & LEGALITAS

- ✅ Menggunakan API resmi Android `VpnService` — tidak melanggar ToS
- ✅ Semua traffic diproses lokal, tidak ada server pihak ketiga
- ✅ User harus secara eksplisit menyetujui dialog izin VPN dari sistem Android
- ✅ Tidak bisa memblokir HTTPS content (hanya DNS) — tidak melanggar enkripsi
- ⚠️ Beberapa app mungkin mendeteksi VPN aktif dan menolak berjalan (VPN detection) — ini normal
- ⚠️ Tidak cocok untuk dipublish ke Google Play Store karena kebijakan Play Store melarang ad blocker. Distribusikan via APK langsung atau F-Droid.

---

## ✅ CHECKLIST FINAL UNTUK AGENT

Pastikan semua item ini selesai sebelum build:

- [ ] `ShieldVpnService.kt` — packet loop berjalan tanpa memory leak
- [ ] `DnsPacketParser.kt` — parse dan build NXDOMAIN response dengan benar (checksum valid)
- [ ] `BlocklistManager.kt` — load 100K+ domain tanpa crash/ANR
- [ ] `HomeScreen.kt` — tombol toggle + statistik real-time via StateFlow
- [ ] `LogScreen.kt` — list domain yang diblokir dengan timestamp
- [ ] `SettingsScreen.kt` — semua setting tersimpan ke DataStore
- [ ] `AndroidManifest.xml` — semua permission dan service declaration benar
- [ ] Notifikasi foreground tampil saat VPN aktif (wajib di Android 14)
- [ ] Test: aktifkan VPN → buka YouTube → iklan berkurang/hilang
- [ ] Test: nonaktifkan VPN → internet normal kembali
- [ ] Build APK release dengan `minifyEnabled = true` untuk ukuran optimal

---

*Prompt ini dibuat oleh Claude (Arsitek Aplikasi) untuk dieksekusi oleh AI Coding Agent.*
*Device target: Samsung Galaxy A56 SM-A566B | Android 14+ | RAM 7.4GB*
