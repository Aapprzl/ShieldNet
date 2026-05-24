package com.shieldnet.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedLogDao {
    @Query("SELECT * FROM blocked_logs ORDER BY timestamp DESC LIMIT 500")
    fun getRecentLogs(): Flow<List<BlockedLogEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: BlockedLogEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(logs: List<BlockedLogEntity>)

    @Query("SELECT COUNT(*) FROM blocked_logs WHERE timestamp > :since")
    fun getCountSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocked_logs")
    fun getTotalCount(): Flow<Int>

    @Query("DELETE FROM blocked_logs WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM blocked_logs")
    suspend fun deleteAll()

    @Query("SELECT domain, COUNT(*) as count FROM blocked_logs GROUP BY domain ORDER BY count DESC LIMIT 10")
    fun getTopBlockedDomains(): Flow<List<DomainCount>>
}

data class DomainCount(
    val domain: String,
    val count: Int
)
