package com.immersive.reader.achievement

import com.immersive.reader.core.model.Book
import com.immersive.reader.core.model.ExitPolicy
import com.immersive.reader.core.model.ReadingSession
import com.immersive.reader.core.model.SessionStatus
import com.immersive.reader.core.model.TimerMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object AchievementEvaluator {
    const val BOOK_FINISH_THRESHOLD = 0.98

    fun evaluate(
        books: List<Book>,
        sessions: List<ReadingSession>,
        zoneId: ZoneId,
    ): List<AchievementProgress> = AchievementCatalog.all.map { definition ->
        when (definition.id) {
            AchievementIds.FIRST_SESSION -> countAtMostOne(definition, sessions) { true }
            AchievementIds.HOURS_1, AchievementIds.HOURS_10 -> hours(definition, sessions)
            AchievementIds.DAYS_7 -> distinctDays(definition, sessions, zoneId)
            AchievementIds.FIRST_IMPORT -> firstImport(definition, books)
            AchievementIds.FIRST_FINISH -> finishedBooks(definition, books, targetCount = 1)
            AchievementIds.BOOKS_5 -> finishedBooks(definition, books, targetCount = 5)
            AchievementIds.COUNTDOWN_COMPLETE -> countAtMostOne(definition, sessions) {
                it.timerMode == TimerMode.COUNTDOWN && it.status == SessionStatus.COMPLETED
            }
            AchievementIds.HOLD_TO_EXIT -> countAtMostOne(definition, sessions) {
                it.exitPolicy == ExitPolicy.HOLD_TO_EXIT &&
                    (it.status == SessionStatus.COMPLETED || it.status == SessionStatus.USER_ENDED)
            }
            AchievementIds.DEEP_FOCUS -> countAtMostOne(definition, sessions) {
                it.exitPolicy == ExitPolicy.TIME_LOCKED && it.status == SessionStatus.COMPLETED
            }
            AchievementIds.FIVE_COMPLETED -> completedSessions(definition, sessions)
            else -> AchievementProgress(definition, current = 0L, unlockedAtEpochMillis = null)
        }
    }

    private fun hours(definition: AchievementDefinition, sessions: List<ReadingSession>): AchievementProgress {
        val countable = sessions
            .filter { it.status != SessionStatus.INTERRUPTED }
            .sortedBy(::eventTime)
        var sum = 0L
        var unlockedAt: Long? = null
        for (session in countable) {
            sum += session.accumulatedReadingMillis
            if (unlockedAt == null && sum >= definition.target) {
                unlockedAt = eventTime(session)
            }
        }
        return AchievementProgress(definition, current = sum, unlockedAtEpochMillis = unlockedAt)
    }

    private fun distinctDays(
        definition: AchievementDefinition,
        sessions: List<ReadingSession>,
        zoneId: ZoneId,
    ): AchievementProgress {
        val byDay = sessions
            .groupBy { localDate(it.startEpochMillis, zoneId) }
            .toSortedMap()
        val days = byDay.keys.toList()
        val unlockedAt = if (days.size >= definition.target) {
            byDay.getValue(days[definition.target.toInt() - 1]).minOf { it.startEpochMillis }
        } else {
            null
        }
        return AchievementProgress(definition, current = days.size.toLong(), unlockedAtEpochMillis = unlockedAt)
    }

    private fun firstImport(definition: AchievementDefinition, books: List<Book>): AchievementProgress {
        val earliest = books.minByOrNull { it.addedAt }
        return AchievementProgress(
            definition,
            current = if (earliest == null) 0L else 1L,
            unlockedAtEpochMillis = earliest?.addedAt,
        )
    }

    private fun finishedBooks(
        definition: AchievementDefinition,
        books: List<Book>,
        targetCount: Int,
    ): AchievementProgress {
        val finished = books
            .filter { (it.progression ?: 0.0) >= BOOK_FINISH_THRESHOLD }
            .sortedBy { it.lastOpenedAt ?: it.addedAt }
        val unlockedAt = finished.getOrNull(targetCount - 1)?.let { it.lastOpenedAt ?: it.addedAt }
        val current = if (targetCount == 1) {
            if (finished.isEmpty()) 0L else 1L
        } else {
            finished.size.toLong()
        }
        return AchievementProgress(definition, current = current, unlockedAtEpochMillis = unlockedAt)
    }

    private fun completedSessions(definition: AchievementDefinition, sessions: List<ReadingSession>): AchievementProgress {
        val completed = sessions
            .filter { it.status == SessionStatus.COMPLETED }
            .sortedBy(::eventTime)
        val unlockedAt = completed.getOrNull(definition.target.toInt() - 1)?.let(::eventTime)
        return AchievementProgress(definition, current = completed.size.toLong(), unlockedAtEpochMillis = unlockedAt)
    }

    private fun countAtMostOne(
        definition: AchievementDefinition,
        sessions: List<ReadingSession>,
        predicate: (ReadingSession) -> Boolean,
    ): AchievementProgress {
        val match = sessions.filter(predicate).minByOrNull(::eventTime)
        return AchievementProgress(
            definition,
            current = if (match == null) 0L else 1L,
            unlockedAtEpochMillis = match?.let(::eventTime),
        )
    }

    private fun eventTime(session: ReadingSession): Long = session.actualEndEpochMillis ?: session.startEpochMillis

    private fun localDate(epochMillis: Long, zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
}
