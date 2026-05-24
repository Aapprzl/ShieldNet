package com.shieldnet.data.repository

import com.shieldnet.data.db.BlockedLogDao
import com.shieldnet.data.db.BlockedLogEntity
import com.shieldnet.data.db.DomainCount
import kotlinx.coroutines.flow.Flow

class LogRepository(private val dao: BlockedLogDao) {

    fun getRecentLogs(): Flow<List<BlockedLogEntity>> = dao.getRecentLogs()

    fun getCountSince(since: Long): Flow<Int> = dao.getCountSince(since)

    fun getTotalCount(): Flow<Int> = dao.getTotalCount()

    fun getTopBlockedDomains(): Flow<List<DomainCount>> = dao.getTopBlockedDomains()

    suspend fun insert(log: BlockedLogEntity) = dao.insert(log)

    suspend fun insertAll(logs: List<BlockedLogEntity>) = dao.insertAll(logs)

    suspend fun deleteOlderThan(before: Long) = dao.deleteOlderThan(before)

    suspend fun deleteAll() = dao.deleteAll()
}
