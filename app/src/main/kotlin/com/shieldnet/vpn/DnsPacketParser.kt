package com.shieldnet.vpn

import com.shieldnet.utils.NetworkUtils
import java.nio.ByteBuffer

object DnsPacketParser {

    data class DnsQuery(
        val transactionId: Short,
        val queryDomain: String,
        val queryType: Short,
        val rawPacket: ByteArray,
        val ipHeader: ByteArray,
        val udpHeader: ByteArray,
        val dnsPayloadOffset: Int
    )

    fun parse(rawPacket: ByteArray): DnsQuery? {
        return try {
            if (rawPacket.size < 28) return null // Min: 20 IP + 8 UDP

            val ipVersion = (rawPacket[0].toInt() and 0xF0) shr 4
            if (ipVersion != 4) return null

            val ipHeaderLength = (rawPacket[0].toInt() and 0x0F) * 4
            if (ipHeaderLength < 20 || rawPacket.size < ipHeaderLength + 8) return null

            val protocol = rawPacket[9].toInt() and 0xFF
            if (protocol != 17) return null // 17 = UDP

            val udpOffset = ipHeaderLength
            val destPort = ((rawPacket[udpOffset + 2].toInt() and 0xFF) shl 8) or
                           (rawPacket[udpOffset + 3].toInt() and 0xFF)
            if (destPort != 53) return null

            val dnsOffset = udpOffset + 8
            if (rawPacket.size < dnsOffset + 12) return null // DNS header is 12 bytes minimum

            val dnsBuffer = ByteBuffer.wrap(rawPacket, dnsOffset, rawPacket.size - dnsOffset)

            val transactionId = dnsBuffer.short
            val flags = dnsBuffer.short
            val isQuery = (flags.toInt() and 0x8000) == 0
            if (!isQuery) return null // Only parse queries

            val questionCount = dnsBuffer.short.toInt() and 0xFFFF
            dnsBuffer.short // Answer count
            dnsBuffer.short // Authority count
            dnsBuffer.short // Additional count

            if (questionCount == 0) return null

            val domain = parseDnsName(rawPacket, dnsOffset + 12)
            if (domain.isBlank()) return null

            // Skip past the QNAME to get QTYPE
            var pos = dnsOffset + 12
            while (pos < rawPacket.size && rawPacket[pos].toInt() and 0xFF != 0) {
                val labelLen = rawPacket[pos].toInt() and 0xFF
                pos += labelLen + 1
            }
            pos++ // skip null terminator

            if (pos + 2 > rawPacket.size) return null
            val queryType = ((rawPacket[pos].toInt() and 0xFF) shl 8 or (rawPacket[pos + 1].toInt() and 0xFF)).toShort()

            DnsQuery(
                transactionId = transactionId,
                queryDomain = domain,
                queryType = queryType,
                rawPacket = rawPacket,
                ipHeader = rawPacket.copyOfRange(0, ipHeaderLength),
                udpHeader = rawPacket.copyOfRange(udpOffset, udpOffset + 8),
                dnsPayloadOffset = dnsOffset
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDnsName(packet: ByteArray, startOffset: Int): String {
        val sb = StringBuilder()
        var offset = startOffset
        var jumps = 0
        val maxJumps = 10 // prevent infinite loops from malformed packets

        while (offset < packet.size && jumps < maxJumps) {
            val length = packet[offset].toInt() and 0xFF
            if (length == 0) break

            // Handle DNS compression pointer
            if ((length and 0xC0) == 0xC0) {
                if (offset + 1 >= packet.size) break
                val pointer = ((length and 0x3F) shl 8) or (packet[offset + 1].toInt() and 0xFF)
                offset = pointer
                jumps++
                continue
            }

            if (offset + 1 + length > packet.size) break
            if (sb.isNotEmpty()) sb.append('.')
            sb.append(String(packet, offset + 1, length, Charsets.US_ASCII))
            offset += length + 1
        }
        return sb.toString()
    }

    fun buildNxDomainResponse(query: DnsQuery): ByteArray {
        // Build DNS NXDOMAIN response
        val questionSection = buildQuestionSection(query.queryDomain, query.queryType)
        val dnsResponseSize = 12 + questionSection.size // 12 byte DNS header + question
        val dnsResponse = ByteBuffer.allocate(dnsResponseSize)

        dnsResponse.putShort(query.transactionId)
        dnsResponse.putShort(0x8183.toShort()) // QR=1, RD=1, RA=1, RCODE=3 (NXDOMAIN)
        dnsResponse.putShort(1)  // QDCOUNT = 1
        dnsResponse.putShort(0)  // ANCOUNT = 0
        dnsResponse.putShort(0)  // NSCOUNT = 0
        dnsResponse.putShort(0)  // ARCOUNT = 0
        dnsResponse.put(questionSection)

        val dnsPayload = ByteArray(dnsResponse.position())
        dnsResponse.flip()
        dnsResponse.get(dnsPayload)

        return wrapWithIpUdp(dnsPayload, query)
    }

    fun buildAddressResponse(query: DnsQuery, address: ByteArray): ByteArray {
        // Build DNS response with A record pointing to 0.0.0.0
        val questionSection = buildQuestionSection(query.queryDomain, query.queryType)
        val dnsResponseSize = 12 + questionSection.size + 16 // header + question + answer (pointer + type + class + ttl + rdlength + rdata)
        val dnsResponse = ByteBuffer.allocate(dnsResponseSize)

        dnsResponse.putShort(query.transactionId)
        dnsResponse.putShort(0x8180.toShort()) // QR=1, RD=1, RA=1, RCODE=0 (NOERROR)
        dnsResponse.putShort(1)  // QDCOUNT = 1
        dnsResponse.putShort(1)  // ANCOUNT = 1
        dnsResponse.putShort(0)  // NSCOUNT = 0
        dnsResponse.putShort(0)  // ARCOUNT = 0
        dnsResponse.put(questionSection)

        // Answer section - A record
        dnsResponse.putShort(0xC00C.toInt().toShort()) // Name pointer to question
        dnsResponse.putShort(1)  // Type A
        dnsResponse.putShort(1)  // Class IN
        dnsResponse.putInt(300)  // TTL = 300 seconds
        dnsResponse.putShort(4)  // RDLENGTH = 4 bytes
        dnsResponse.put(address) // 0.0.0.0

        val dnsPayload = ByteArray(dnsResponse.position())
        dnsResponse.flip()
        dnsResponse.get(dnsPayload)

        return wrapWithIpUdp(dnsPayload, query)
    }

    private fun buildQuestionSection(domain: String, queryType: Short): ByteArray {
        val labels = domain.split(".")
        // Calculate size: each label = 1 byte length + label bytes, plus 1 null terminator + 4 bytes (type + class)
        val size = labels.sumOf { it.length + 1 } + 1 + 4
        val buffer = ByteBuffer.allocate(size)

        for (label in labels) {
            buffer.put(label.length.toByte())
            buffer.put(label.toByteArray(Charsets.US_ASCII))
        }
        buffer.put(0) // Null terminator
        buffer.putShort(queryType) // QTYPE
        buffer.putShort(1) // QCLASS = IN

        return buffer.array()
    }

    private fun wrapWithIpUdp(dnsPayload: ByteArray, query: DnsQuery): ByteArray {
        val ipHeaderLen = query.ipHeader.size
        val udpLength = 8 + dnsPayload.size
        val totalLength = ipHeaderLen + udpLength

        val packet = ByteArray(totalLength)
        val buffer = ByteBuffer.wrap(packet)

        // --- IP Header (swap src/dst) ---
        buffer.put(query.ipHeader) // Copy original IP header as base
        buffer.position(0)

        // Set total length
        buffer.putShort(2, totalLength.toShort())

        // Set identification to 0
        buffer.putShort(4, 0)

        // Set flags: Don't Fragment
        buffer.putShort(6, 0x4000.toShort())

        // Set TTL
        buffer.put(8, 64)

        // Zero out checksum for recalculation
        buffer.putShort(10, 0)

        // Swap source and destination IP addresses
        val srcIp = query.ipHeader.copyOfRange(12, 16)
        val dstIp = query.ipHeader.copyOfRange(16, 20)
        buffer.position(12)
        buffer.put(dstIp) // new source = original destination
        buffer.put(srcIp) // new dest = original source

        // Calculate IP header checksum
        val ipChecksum = NetworkUtils.calculateChecksum(packet, 0, ipHeaderLen)
        buffer.putShort(10, ipChecksum)

        // --- UDP Header (swap ports) ---
        val origSrcPort = ((query.udpHeader[0].toInt() and 0xFF) shl 8) or (query.udpHeader[1].toInt() and 0xFF)
        val origDstPort = ((query.udpHeader[2].toInt() and 0xFF) shl 8) or (query.udpHeader[3].toInt() and 0xFF)

        buffer.position(ipHeaderLen)
        buffer.putShort(origDstPort.toShort()) // Swap: src port = original dst port (53)
        buffer.putShort(origSrcPort.toShort()) // Swap: dst port = original src port
        buffer.putShort(udpLength.toShort())   // UDP length
        buffer.putShort(0)                     // UDP checksum (0 = disabled for UDP over IPv4)

        // --- DNS Payload ---
        buffer.put(dnsPayload)

        // Calculate UDP checksum
        val udpPacket = packet.copyOfRange(ipHeaderLen, totalLength)
        val udpChecksum = NetworkUtils.calculateUdpChecksum(
            packet.copyOfRange(12, 16), // src IP
            packet.copyOfRange(16, 20), // dst IP
            udpPacket
        )
        buffer.putShort(ipHeaderLen + 6, udpChecksum)

        return packet
    }
}
