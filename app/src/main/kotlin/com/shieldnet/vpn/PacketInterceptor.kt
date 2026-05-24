package com.shieldnet.vpn

import com.shieldnet.blocklist.BlocklistManager
import com.shieldnet.data.db.BlockedLogEntity
import com.shieldnet.data.repository.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream

class PacketInterceptor(
    private val blocklistManager: BlocklistManager,
    private val logRepository: LogRepository,
    private val forwarder: TcpUdpForwarder,
    private val dnsServerIp: String,
    private val saveLog: Boolean = true
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun interceptPacket(packet: ByteArray, outputStream: FileOutputStream) {
        if (isDnsPacket(packet)) {
            handleDnsPacket(packet, outputStream)
        } else {
            // Write non-DNS packets back to VPN interface unmodified
            try {
                synchronized(outputStream) {
                    outputStream.write(packet)
                    outputStream.flush()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun isDnsPacket(packet: ByteArray): Boolean {
        if (packet.size < 28) return false
        val ipVersion = (packet[0].toInt() and 0xF0) shr 4
        if (ipVersion != 4) return false
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return false // Not UDP
        val ipHeaderLength = (packet[0].toInt() and 0x0F) * 4
        val destPort = ((packet[ipHeaderLength + 2].toInt() and 0xFF) shl 8) or
                       (packet[ipHeaderLength + 3].toInt() and 0xFF)
        return destPort == 53
    }

    private fun handleDnsPacket(packet: ByteArray, outputStream: FileOutputStream) {
        val dnsQuery = DnsPacketParser.parse(packet) ?: return
        val domain = dnsQuery.queryDomain

        if (blocklistManager.isBlocked(domain)) {
            // Blocked — respond with 0.0.0.0 (A record) for cleaner handling
            val response = DnsPacketParser.buildAddressResponse(
                dnsQuery,
                byteArrayOf(0, 0, 0, 0)
            )
            synchronized(outputStream) {
                outputStream.write(response)
                outputStream.flush()
            }

            // Immediately insert log entry to Room database
            if (saveLog) {
                CoroutineScope(Dispatchers.IO).launch {
                    logRepository.insert(BlockedLogEntity(domain = domain))
                }
            }
        } else {
            // Not blocked — forward to real DNS
            CoroutineScope(Dispatchers.IO).launch {
                forwarder.forwardDnsQuery(packet, outputStream, dnsServerIp)
            }
        }
    }

    fun flushLogs() {
        // No-op: logs are now inserted immediately
    }
}

