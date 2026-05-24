package com.shieldnet

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.shieldnet.data.db.AppDatabase
import com.shieldnet.data.repository.LogRepository

class ShieldNetApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var logRepository: LogRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        logRepository = LogRepository(database.blockedLogDao())
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ShieldNet VPN Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification for active VPN protection"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "shieldnet_vpn_channel"
        lateinit var instance: ShieldNetApp
            private set
    }
}
