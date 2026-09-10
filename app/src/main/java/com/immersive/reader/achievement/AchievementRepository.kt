package com.immersive.reader.achievement

import com.immersive.reader.core.database.AchievementUnlockDao
import com.immersive.reader.core.database.AchievementUnlockEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class AchievementRepository @Inject constructor(
    private val dao: AchievementUnlockDao,
) {
    val unlocks: Flow<List<AchievementUnlock>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }

    suspend fun sync(snapshots: List<AchievementProgress>, nowMillis: Long = System.currentTimeMillis()) {
        val existing = dao.getAll().associate { it.id to it.toModel() }
        val inserts = AchievementUnlockSync.toInsert(snapshots, existing, nowMillis)
        if (inserts.isNotEmpty()) {
            dao.insertAll(inserts.map { it.toEntity() })
        }
    }
}

private fun AchievementUnlockEntity.toModel() = AchievementUnlock(id, unlockedAtEpochMillis, createdAtEpochMillis)

private fun AchievementUnlock.toEntity() = AchievementUnlockEntity(id, unlockedAtEpochMillis, createdAtEpochMillis)
