package com.shieldnet.utils

import java.net.InetAddress
import java.nio.ByteBuffer

object NetworkUtils {

    fun calculateChecksum(data: ByteArray, offset: Int = 0, length: Int = data.size): Short {
        var sum = 0L
        var i = offset
        var remaining = length

        while (remaining > 1) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
            remaining -= 2
        }

        if (remaining == 1) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }

        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        return (sum.inv() and 0xFFFF).toShort()
    }

    fun calculateUdpChecksum(
        srcAddr: ByteArray,
        dstAddr: ByteArray,
        udpPacket: ByteArray
    ): Short {
        val pseudoHeaderSize = 12 + udpPacket.size + (if (udpPacket.size % 2 != 0) 1 else 0)
        val pseudoPacket = ByteArray(pseudoHeaderSize)
        val buffer = ByteBuffer.wrap(pseudoPacket)

        buffer.put(srcAddr)
        buffer.put(dstAddr)
        buffer.put(0) // Zero
        buffer.put(17) // UDP protocol
        buffer.putShort(udpPacket.size.toShort())
        buffer.put(udpPacket)

        return calculateChecksum(pseudoPacket)
    }

    fun ipToBytes(ip: String): ByteArray {
        return InetAddress.getByName(ip).address
    }
}
