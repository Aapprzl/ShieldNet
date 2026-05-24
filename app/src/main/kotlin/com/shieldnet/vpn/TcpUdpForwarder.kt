package com.shieldnet.vpn

import com.shieldnet.utils.Constants
import com.shieldnet.utils.NetworkUtils
import kotlinx.coroutines.*
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class TcpUdpForwarder(private val vpnService: android.net.VpnService) {

    suspend fun forwardDnsQuery(
        originalPacket: ByteArray,
        outputStream: FileOutputStream,
        dnsServerIp: String = Constants.DEFAULT_DNS_SERVER
    ) {
        try {
            val ipHeaderLength = (originalPacket[0].toInt() and 0x0F) * 4
            val udpOffset = ipHeaderLength
            val dnsOffset = udpOffset + 8
            val dnsPayload = originalPacket.copyOfRange(dnsOffset, originalPacket.size)

            val primaryIp = if (dnsServerIp == "1.1.1.1") "8.8.8.8" else dnsServerIp
            val secondaryIp = "8.8.4.4"

            // Query Primary DNS with 3-second timeout
            var dnsResponse = coroutineScope {
                async(Dispatchers.IO) {
                    performDnsQuery(dnsPayload, primaryIp, 3000)
                }.await()
            }

            // Fallback to Secondary DNS if Primary timed out or failed
            if (dnsResponse == null) {
                dnsResponse = coroutineScope {
                    async(Dispatchers.IO) {
                        performDnsQuery(dnsPayload, secondaryIp, Constants.DNS_TIMEOUT_MS.toInt())
                    }.await()
                }
            }

            if (dnsResponse != null) {
                // Wrap response with IP+UDP headers (swap src/dst from original packet)
                val wrappedResponse = wrapDnsResponse(originalPacket, dnsResponse)

                synchronized(outputStream) {
                    outputStream.write(wrappedResponse)
                    outputStream.flush()
                }
            }
        } catch (e: Exception) {
            // Timeout or network error — silently drop
        }
    }

    private suspend fun performDnsQuery(
        dnsPayload: ByteArray,
        serverIp: String,
        timeoutMs: Int
    ): ByteArray? = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            vpnService.protect(socket)
            socket.soTimeout = timeoutMs
            val serverAddress = InetAddress.getByName(serverIp)
            val requestPacket = DatagramPacket(dnsPayload, dnsPayload.size, serverAddress, Constants.DNS_PORT)
            socket.send(requestPacket)

            val responseBuffer = ByteArray(1024)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)
            responseBuffer.copyOf(responsePacket.length)
        } catch (e: Exception) {
            null
        } finally {
            socket?.close()
        }
    }

    private fun wrapDnsResponse(originalPacket: ByteArray, dnsPayload: ByteArray): ByteArray {
        val ipHeaderLen = (originalPacket[0].toInt() and 0x0F) * 4
        val udpLength = 8 + dnsPayload.size
        val totalLength = ipHeaderLen + udpLength

        val packet = ByteArray(totalLength)
        val buffer = ByteBuffer.wrap(packet)

        // Copy original IP header as template
        System.arraycopy(originalPacket, 0, packet, 0, ipHeaderLen)

        // Fix IP header
        buffer.putShort(2, totalLength.toShort()) // Total length
        buffer.putShort(4, 0) // Identification
        buffer.putShort(6, 0x4000.toShort()) // Don't Fragment
        buffer.put(8, 64) // TTL
        buffer.putShort(10, 0) // Clear checksum

        // Swap source and destination IP
        val srcIp = originalPacket.copyOfRange(12, 16)
        val dstIp = originalPacket.copyOfRange(16, 20)
        System.arraycopy(dstIp, 0, packet, 12, 4) // new src = original dst
        System.arraycopy(srcIp, 0, packet, 16, 4) // new dst = original src

        // Recalculate IP checksum
        val ipChecksum = NetworkUtils.calculateChecksum(packet, 0, ipHeaderLen)
        buffer.putShort(10, ipChecksum)

        // UDP header with swapped ports
        val origSrcPort = ((originalPacket[ipHeaderLen].toInt() and 0xFF) shl 8) or
                          (originalPacket[ipHeaderLen + 1].toInt() and 0xFF)
        val origDstPort = ((originalPacket[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or
                          (originalPacket[ipHeaderLen + 3].toInt() and 0xFF)

        buffer.position(ipHeaderLen)
        buffer.putShort(origDstPort.toShort()) // src port = original dst (53)
        buffer.putShort(origSrcPort.toShort()) // dst port = original src
        buffer.putShort(udpLength.toShort())   // UDP length
        buffer.putShort(0)                     // UDP checksum placeholder

        // DNS payload
        buffer.put(dnsPayload)

        // Calculate UDP checksum
        val udpPacket = packet.copyOfRange(ipHeaderLen, totalLength)
        val udpChecksum = NetworkUtils.calculateUdpChecksum(
            packet.copyOfRange(12, 16),
            packet.copyOfRange(16, 20),
            udpPacket
        )
        buffer.putShort(ipHeaderLen + 6, udpChecksum)

        return packet
    }
}
