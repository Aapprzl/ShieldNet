package com.shieldnet.blocklist

import android.content.Context
import com.shieldnet.R
import java.io.File
import java.io.FileInputStream

class BlocklistManager(private val context: Context) {

    private val blockedDomains = HashSet<String>(150_000)
    private val whitelistedDomains = HashSet<String>()
    private val customBlockedDomains = HashSet<String>()

    val domainCount: Int get() = blockedDomains.size

    fun loadBlocklist() {
        blockedDomains.clear()

        // Try loading updated blocklist first
        val updatedFile = File(context.filesDir, "blocklist_updated.txt")
        if (updatedFile.exists()) {
            val domains = BlocklistParser.parse(FileInputStream(updatedFile))
            blockedDomains.addAll(domains)
        } else {
            // Fallback to bundled blocklist
            try {
                val domains = BlocklistParser.parse(
                    context.resources.openRawResource(R.raw.blocklist_default)
                )
                blockedDomains.addAll(domains)
            } catch (e: Exception) {
                // If no bundled blocklist, add some essential ad domains
                addEssentialAdDomains()
            }
        }

        // Load custom user domains
        loadUserCustomBlocklist()
        loadUserWhitelist()

        blockedDomains.addAll(customBlockedDomains)
        blockedDomains.removeAll(whitelistedDomains)
    }

    fun isBlocked(domain: String): Boolean {
        val lowerDomain = domain.lowercase().trimEnd('.')

        // Check whitelist first
        if (whitelistedDomains.contains(lowerDomain)) return false

        // Check exact match
        if (blockedDomains.contains(lowerDomain)) return true

        // Check subdomain match
        val parts = lowerDomain.split(".")
        for (i in 1 until parts.size - 1) {
            val parentDomain = parts.subList(i, parts.size).joinToString(".")
            if (blockedDomains.contains(parentDomain)) return true
        }

        return false
    }

    fun addCustomDomain(domain: String) {
        val d = domain.lowercase().trim()
        customBlockedDomains.add(d)
        blockedDomains.add(d)
        saveUserCustomBlocklist()
    }

    fun removeCustomDomain(domain: String) {
        val d = domain.lowercase().trim()
        customBlockedDomains.remove(d)
        blockedDomains.remove(d)
        saveUserCustomBlocklist()
    }

    fun addWhitelistDomain(domain: String) {
        val d = domain.lowercase().trim()
        whitelistedDomains.add(d)
        blockedDomains.remove(d)
        saveUserWhitelist()
    }

    fun removeWhitelistDomain(domain: String) {
        val d = domain.lowercase().trim()
        whitelistedDomains.remove(d)
        saveUserWhitelist()
    }

    private fun loadUserCustomBlocklist() {
        val prefs = context.getSharedPreferences("shieldnet_custom_blocklist", Context.MODE_PRIVATE)
        val domains = prefs.getStringSet("custom_domains", emptySet()) ?: emptySet()
        customBlockedDomains.addAll(domains)
    }

    private fun saveUserCustomBlocklist() {
        val prefs = context.getSharedPreferences("shieldnet_custom_blocklist", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("custom_domains", customBlockedDomains).apply()
    }

    private fun loadUserWhitelist() {
        val prefs = context.getSharedPreferences("shieldnet_whitelist", Context.MODE_PRIVATE)
        val domains = prefs.getStringSet("whitelist_domains", emptySet()) ?: emptySet()
        whitelistedDomains.addAll(domains)
    }

    private fun saveUserWhitelist() {
        val prefs = context.getSharedPreferences("shieldnet_whitelist", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("whitelist_domains", whitelistedDomains).apply()
    }

    private fun addEssentialAdDomains() {
        val essentials = listOf(
            "ads.google.com", "pagead2.googlesyndication.com", "googleads.g.doubleclick.net",
            "adservice.google.com", "ad.doubleclick.net", "static.doubleclick.net",
            "m.doubleclick.net", "mediavisor.doubleclick.net",
            "googleadservices.com", "adn.fusionads.net",
            "analytics.google.com", "ssl.google-analytics.com",
            "graph.facebook.com", "an.facebook.com", "ads.facebook.com",
            "pixel.facebook.com", "www.facebook.com/tr",
            "ads.yahoo.com", "global.adserver.yahoo.com",
            "ads.pubmatic.com", "t.appsflyer.com",
            "app.adjust.com", "settings.crashlytics.com",
            "e.crashlytics.com", "pagead2.googleadservices.com",
            "securepubads.g.doubleclick.net", "adclick.g.doubleclick.net"
        )
        blockedDomains.addAll(essentials)
    }
}
