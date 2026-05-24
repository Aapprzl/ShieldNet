package com.shieldnet.blocklist

import java.io.InputStream

object BlocklistParser {

    fun parse(inputStream: InputStream): Set<String> {
        val domains = HashSet<String>(150_000)

        inputStream.bufferedReader().use { reader ->
            reader.lineSequence()
                .filter { line ->
                    line.isNotBlank() &&
                    !line.startsWith("#") &&
                    !line.startsWith("!") &&
                    (line.startsWith("0.0.0.0") || line.startsWith("127.0.0.1"))
                }
                .forEach { line ->
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        val domain = parts[1].lowercase().trimEnd('.')
                        if (domain.isNotBlank() &&
                            domain != "0.0.0.0" &&
                            domain != "127.0.0.1" &&
                            domain != "localhost" &&
                            domain != "localhost.localdomain" &&
                            domain != "local" &&
                            domain != "broadcasthost" &&
                            domain.contains(".")) {
                            domains.add(domain)
                        }
                    }
                }
        }

        return domains
    }
}
