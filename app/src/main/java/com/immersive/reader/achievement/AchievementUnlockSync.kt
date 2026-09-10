package com.immersive.reader.achievement

object AchievementUnlockSync {
    fun toInsert(
        snapshots: List<AchievementProgress>,
        existing: Map<String, AchievementUnlock>,
        nowMillis: Long,
    ): List<AchievementUnlock> = snapshots.mapNotNull { snapshot ->
        val unlockedAt = snapshot.unlockedAtEpochMillis ?: return@mapNotNull null
        if (existing.containsKey(snapshot.definition.id)) return@mapNotNull null
        AchievementUnlock(
            id = snapshot.definition.id,
            unlockedAtEpochMillis = unlockedAt,
            createdAtEpochMillis = nowMillis,
        )
    }

    fun present(
        snapshots: List<AchievementProgress>,
        existing: Map<String, AchievementUnlock>,
    ): List<AchievementProgress> = snapshots.map { snapshot ->
        val stored = existing[snapshot.definition.id] ?: return@map snapshot
        snapshot.copy(unlockedAtEpochMillis = stored.unlockedAtEpochMillis)
    }
}
