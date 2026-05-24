package com.shieldnet.blocklist

import android.content.Context
import com.shieldnet.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class BlocklistUpdater(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun updateBlocklist(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(Constants.BLOCKLIST_URL)
                .build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return@withContext Result.failure(
                IOException("HTTP ${response.code}")
            )

            val content = response.body?.string() ?: return@withContext Result.failure(
                IOException("Empty response")
            )

            val file = File(context.filesDir, Constants.BLOCKLIST_FILENAME)
            file.writeText(content)

            val count = content.lines()
                .count { it.startsWith("0.0.0.0") && !it.startsWith("0.0.0.0 0.0.0.0") }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLastUpdateTime(): Long {
        val file = File(context.filesDir, Constants.BLOCKLIST_FILENAME)
        return if (file.exists()) file.lastModified() else 0L
    }

    fun hasUpdatedBlocklist(): Boolean {
        return File(context.filesDir, Constants.BLOCKLIST_FILENAME).exists()
    }
}
