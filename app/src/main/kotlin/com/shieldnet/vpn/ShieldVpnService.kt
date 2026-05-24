package com.shieldnet.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.shieldnet.MainActivity
import com.shieldnet.R
import com.shieldnet.ShieldNetApp
import com.shieldnet.blocklist.BlocklistManager
import com.shieldnet.utils.Constants
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class ShieldVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private lateinit var blocklistManager: BlocklistManager
    private lateinit var packetInterceptor: PacketInterceptor
    private val isRunning = AtomicBoolean(false)
    private var vpnJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constants.ACTION_START_VPN -> startVpn()
            Constants.ACTION_STOP_VPN -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning.get()) return

        // Load blocklist
        blocklistManager = BlocklistManager(this)
        blocklistManager.loadBlocklist()

        // Get settings
        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val dnsServer = prefs.getString(Constants.PREF_DNS_SERVER, Constants.DEFAULT_DNS_SERVER) ?: Constants.DEFAULT_DNS_SERVER
        val saveLog = prefs.getBoolean(Constants.PREF_SAVE_LOG, true)

        // Setup repository (singleton from Application)
        val app = application as ShieldNetApp
        val logRepository = app.logRepository

        // Create interceptor
        val forwarder = TcpUdpForwarder(this)
        packetInterceptor = PacketInterceptor(
            blocklistManager = blocklistManager,
            logRepository = logRepository,
            forwarder = forwarder,
            dnsServerIp = dnsServer,
            saveLog = saveLog
        )

        // Build VPN interface
        val builder = Builder()
            .setSession("ShieldNet")
            .addAddress(Constants.VPN_ADDRESS, 32)
            .addRoute(Constants.VPN_DNS, 32)
            .addDnsServer(Constants.VPN_DNS)
            .setMtu(1400)
            .setBlocking(true)

        // Exclude this app from VPN to prevent loops
        try {
            builder.addDisallowedApplication(packageName)
        } catch (e: Exception) {
            // Ignore
        }

        vpnInterface = builder.establish() ?: run {
            stopSelf()
            return
        }

        isRunning.set(true)
        isVpnRunning = true

        // Show foreground notification
        startForeground(Constants.NOTIFICATION_ID, buildNotification())

        // Start packet processing
        vpnJob = serviceScope.launch {
            processPackets()
        }
    }

    private suspend fun processPackets() {
        val fileDescriptor = vpnInterface?.fileDescriptor ?: return
        val inputStream = FileInputStream(fileDescriptor)
        val outputStream = FileOutputStream(fileDescriptor)
        val packetBuffer = ByteArray(Constants.PACKET_BUFFER_SIZE)

        try {
            while (isRunning.get()) {
                val length = inputStream.read(packetBuffer)
                if (length <= 0) {
                    delay(1)
                    continue
                }

                val packet = packetBuffer.copyOf(length)
                packetInterceptor.interceptPacket(packet, outputStream)
            }
        } catch (e: Exception) {
            if (isRunning.get()) {
                // Unexpected error — try to restart
                delay(1000)
                if (isRunning.get()) processPackets()
            }
        } finally {
            inputStream.close()
            outputStream.close()
        }
    }

    private fun stopVpn() {
        isRunning.set(false)
        isVpnRunning = false
        vpnJob?.cancel()

        // Flush remaining logs
        if (::packetInterceptor.isInitialized) {
            packetInterceptor.flushLogs()
        }

        vpnInterface?.close()
        vpnInterface = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, ShieldVpnService::class.java).apply {
                action = Constants.ACTION_STOP_VPN
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, ShieldNetApp.CHANNEL_ID)
            .setContentTitle("ShieldNet Active")
            .setContentText("Your device is protected from ads & trackers")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        @Volatile
        var isVpnRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, ShieldVpnService::class.java).apply {
                action = Constants.ACTION_START_VPN
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ShieldVpnService::class.java).apply {
                action = Constants.ACTION_STOP_VPN
            }
            context.startService(intent)
        }
    }
}
