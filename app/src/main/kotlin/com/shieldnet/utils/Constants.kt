package com.shieldnet.utils

object Constants {
    const val VPN_ADDRESS = "10.0.0.2"
    const val VPN_ROUTE = "0.0.0.0"
    const val VPN_DNS = "10.0.0.1"
    const val VPN_MTU = 1400
    const val DNS_PORT = 53
    const val PACKET_BUFFER_SIZE = 32767

    const val DEFAULT_DNS_SERVER = "8.8.8.8" // Google Primary
    const val SECONDARY_DNS_SERVER = "8.8.4.4" // Google Secondary
    const val DNS_TIMEOUT_MS = 5000L

    const val ACTION_START_VPN = "com.shieldnet.START_VPN"
    const val ACTION_STOP_VPN = "com.shieldnet.STOP_VPN"

    const val BLOCKLIST_URL = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
    const val BLOCKLIST_FILENAME = "blocklist_updated.txt"

    const val NOTIFICATION_ID = 1
    const val CHANNEL_ID = "shieldnet_vpn_channel"

    const val PREFS_NAME = "shieldnet_prefs"
    const val PREF_AUTO_START = "auto_start_on_boot"
    const val PREF_DNS_SERVER = "dns_server"
    const val PREF_BLOCK_IPV6 = "block_ipv6"
    const val PREF_SAVE_LOG = "save_log"
    const val PREF_AUTO_UPDATE = "auto_update_blocklist"
    const val PREF_LOG_RETENTION_DAYS = "log_retention_days"
}
