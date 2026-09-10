package com.immersive.reader.achievement

import com.immersive.reader.core.model.Book
import com.immersive.reader.core.model.ExitPolicy
import com.immersive.reader.core.model.ReadingSession
import com.immersive.reader.core.model.SessionStatus
import com.immersive.reader.core.model.TimerMode
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementEvaluatorTest {
    private val utc = ZoneId.of("UTC")

    @Test
    fun catalogContainsElevenDefinitions() {
        assertEquals(11, AchievementCatalog.all.size)
        assertEquals(
            listOf(
                AchievementIds.FIRST_SESSION,
                AchievementIds.HOURS_1,
                AchievementIds.HOURS_10,
                AchievementIds.DAYS_7,
                AchievementIds.FIRST_IMPORT,
                AchievementIds.FIRST_FINISH,
                AchievementIds.BOOKS_5,
                AchievementIds.COUNTDOWN_COMPLETE,
                AchievementIds.HOLD_TO_EXIT,
                AchievementIds.DEEP_FOCUS,
                AchievementIds.FIVE_COMPLETED,
            ),
            AchievementCatalog.all.map { it.id },
        )
    }

    @Test
    fun emptyLibraryUnlocksNothing() {
        val progress = evaluate()
        progress.forEach { item ->
            assertEquals(0L, item.current)
            assertNull(item.unlockedAtEpochMillis)
        }
    }

    @Test
    fun firstSessionUnlocksOnAnySessionIncludingInterrupted() {
        val interrupted = session(
            id = "s1",
            start = 1_000L,
            end = 2_000L,
            accumulated = 0L,
            status = SessionStatus.INTERRUPTED,
        )
        val item = evaluate(sessions = listOf(interrupted)).byId(AchievementIds.FIRST_SESSION)
        assertEquals(1L, item.current)
        assertEquals(2_000L, item.unlockedAtEpochMillis)
    }

    @Test
    fun interruptedTimeDoesNotCountTowardHours() {
        val interrupted = session(
            start = 0L,
            end = TimeUnit.HOURS.toMillis(3),
            accumulated = TimeUnit.HOURS.toMillis(3),
            status = SessionStatus.INTERRUPTED,
        )
        val hours = evaluate(sessions = listOf(interrupted)).byId(AchievementIds.HOURS_1)
        assertEquals(0L, hours.current)
        assertNull(hours.unlockedAtEpochMillis)
    }

    @Test
    fun oneHourUnlocksWhenCumulativeReadingCrossesThreshold() {
        val first = session(
            id = "a",
            start = 10_000L,
            end = 10_000L + TimeUnit.MINUTES.toMillis(40),
            accumulated = TimeUnit.MINUTES.toMillis(40),
            status = SessionStatus.USER_ENDED,
        )
        val second = session(
            id = "b",
            start = TimeUnit.HOURS.toMillis(2),
            end = TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(25),
            accumulated = TimeUnit.MINUTES.toMillis(25),
            status = SessionStatus.COMPLETED,
        )
        val hours = evaluate(sessions = listOf(second, first)).byId(AchievementIds.HOURS_1)
        assertEquals(TimeUnit.MINUTES.toMillis(65), hours.current)
        assertEquals(second.actualEndEpochMillis, hours.unlockedAtEpochMillis)
        assertNull(evaluate(sessions = listOf(second, first)).byId(AchievementIds.HOURS_10).unlockedAtEpochMillis)
    }

    @Test
    fun tenHoursUnlocksAtCrossingSession() {
        val sessions = (0 until 10).map { index ->
            session(
                id = "h$index",
                start = index * 1_000_000L,
                end = index * 1_000_000L + TimeUnit.HOURS.toMillis(1),
                accumulated = TimeUnit.HOURS.toMillis(1),
                status = SessionStatus.USER_ENDED,
            )
        }
        val hours = evaluate(sessions = sessions).byId(AchievementIds.HOURS_10)
        assertEquals(TimeUnit.HOURS.toMillis(10), hours.current)
        assertEquals(sessions.last().actualEndEpochMillis, hours.unlockedAtEpochMillis)
    }

    @Test
    fun sevenDaysCountsDistinctLocalDaysNotConsecutiveStreak() {
        val sessions = listOf(1, 2, 3, 5, 8, 9, 10).mapIndexed { index, day ->
            session(
                id = "d$index",
                start = utcDay(day),
                end = utcDay(day) + 1_000L,
                accumulated = 1_000L,
                status = SessionStatus.USER_ENDED,
            )
        }
        val days = evaluate(sessions = sessions).byId(AchievementIds.DAYS_7)
        assertEquals(7L, days.current)
        assertEquals(utcDay(10), days.unlockedAtEpochMillis)
    }

    @Test
    fun sameLocalDayCountsOnceForSevenDays() {
        val morning = session(id = "m", start = utcDay(1), end = utcDay(1) + 1_000L, status = SessionStatus.USER_ENDED)
        val evening = session(id = "e", start = utcDay(1) + TimeUnit.HOURS.toMillis(12), end = utcDay(1) + TimeUnit.HOURS.toMillis(13), status = SessionStatus.COMPLETED)
        val days = evaluate(sessions = listOf(morning, evening)).byId(AchievementIds.DAYS_7)
        assertEquals(1L, days.current)
        assertNull(days.unlockedAtEpochMillis)
    }

    @Test
    fun sevenDaysUsesLocalTimeZone() {
        val losAngeles = ZoneId.of("America/Los_Angeles")
        val lateUtc = utcDay(2) + TimeUnit.HOURS.toMillis(7)
        val sessions = (0 until 7).map { index ->
            session(
                id = "z$index",
                start = lateUtc + TimeUnit.DAYS.toMillis(index.toLong()),
                end = lateUtc + TimeUnit.DAYS.toMillis(index.toLong()) + 1_000L,
                status = SessionStatus.USER_ENDED,
            )
        }
        val days = AchievementEvaluator.evaluate(emptyList(), sessions, losAngeles).byId(AchievementIds.DAYS_7)
        assertEquals(7L, days.current)
        assertEquals(sessions.last().startEpochMillis, days.unlockedAtEpochMillis)
    }

    @Test
    fun firstImportUnlocksFromEarliestAddedBook() {
        val later = book(id = "b2", addedAt = 50L)
        val earlier = book(id = "b1", addedAt = 10L)
        val item = evaluate(books = listOf(later, earlier)).byId(AchievementIds.FIRST_IMPORT)
        assertEquals(1L, item.current)
        assertEquals(10L, item.unlockedAtEpochMillis)
    }

    @Test
    fun firstFinishRequiresProgressionAtLeastNinetyEightPercent() {
        val almost = book(id = "almost", progression = 0.979, lastOpenedAt = 20L)
        val done = book(id = "done", progression = 0.98, lastOpenedAt = 40L)
        assertNull(evaluate(books = listOf(almost)).byId(AchievementIds.FIRST_FINISH).unlockedAtEpochMillis)
        val finished = evaluate(books = listOf(almost, done)).byId(AchievementIds.FIRST_FINISH)
        assertEquals(1L, finished.current)
        assertEquals(40L, finished.unlockedAtEpochMillis)
    }

    @Test
    fun fiveFinishedBooksUnlocksOnFifthByOpenTime() {
        val books = (0 until 5).map { index ->
            book(id = "f$index", progression = 1.0, lastOpenedAt = (index + 1) * 100L)
        }
        val item = evaluate(books = books).byId(AchievementIds.BOOKS_5)
        assertEquals(5L, item.current)
        assertEquals(500L, item.unlockedAtEpochMillis)
    }

    @Test
    fun countdownCompleteRequiresCompletedCountdown() {
        val endedEarly = session(
            timerMode = TimerMode.COUNTDOWN,
            status = SessionStatus.USER_ENDED,
            start = 1L,
            end = 2L,
        )
        val completed = session(
            id = "c",
            timerMode = TimerMode.COUNTDOWN,
            status = SessionStatus.COMPLETED,
            start = 3L,
            end = 4L,
        )
        assertNull(evaluate(sessions = listOf(endedEarly)).byId(AchievementIds.COUNTDOWN_COMPLETE).unlockedAtEpochMillis)
        val item = evaluate(sessions = listOf(endedEarly, completed)).byId(AchievementIds.COUNTDOWN_COMPLETE)
        assertEquals(1L, item.current)
        assertEquals(4L, item.unlockedAtEpochMillis)
    }

    @Test
    fun holdToExitAcceptsCompletedOrUserEnded() {
        val hold = session(
            exitPolicy = ExitPolicy.HOLD_TO_EXIT,
            status = SessionStatus.USER_ENDED,
            start = 8L,
            end = 9L,
        )
        val item = evaluate(sessions = listOf(hold)).byId(AchievementIds.HOLD_TO_EXIT)
        assertEquals(1L, item.current)
        assertEquals(9L, item.unlockedAtEpochMillis)
    }

    @Test
    fun deepFocusRequiresTimeLockedCompletedSession() {
        val lockedButLeft = session(
            exitPolicy = ExitPolicy.TIME_LOCKED,
            status = SessionStatus.EMERGENCY_EXIT,
            start = 1L,
            end = 2L,
        )
        val completed = session(
            id = "df",
            exitPolicy = ExitPolicy.TIME_LOCKED,
            status = SessionStatus.COMPLETED,
            start = 3L,
            end = 4L,
        )
        assertNull(evaluate(sessions = listOf(lockedButLeft)).byId(AchievementIds.DEEP_FOCUS).unlockedAtEpochMillis)
        assertEquals(4L, evaluate(sessions = listOf(lockedButLeft, completed)).byId(AchievementIds.DEEP_FOCUS).unlockedAtEpochMillis)
    }

    @Test
    fun fiveCompletedCountsOnlyCompletedSessions() {
        val sessions = (0 until 4).map { index ->
            session(id = "c$index", status = SessionStatus.COMPLETED, start = index * 10L + 1, end = index * 10L + 2)
        } + session(id = "u", status = SessionStatus.USER_ENDED, start = 100L, end = 101L)
        val short = evaluate(sessions = sessions).byId(AchievementIds.FIVE_COMPLETED)
        assertEquals(4L, short.current)
        assertNull(short.unlockedAtEpochMillis)

        val fifth = session(id = "c4", status = SessionStatus.COMPLETED, start = 200L, end = 201L)
        val unlocked = evaluate(sessions = sessions + fifth).byId(AchievementIds.FIVE_COMPLETED)
        assertEquals(5L, unlocked.current)
        assertEquals(201L, unlocked.unlockedAtEpochMillis)
    }

    @Test
    fun syncInsertsNewUnlocksAndNeverRevokes() {
        val snapshots = evaluate(books = listOf(book(progression = 0.99, lastOpenedAt = 40L)))
        val inserts = AchievementUnlockSync.toInsert(snapshots, existing = emptyMap(), nowMillis = 99L)
        val firstFinish = inserts.single { it.id == AchievementIds.FIRST_FINISH }
        assertEquals(40L, firstFinish.unlockedAtEpochMillis)
        assertEquals(99L, firstFinish.createdAtEpochMillis)

        val stored = mapOf(
            AchievementIds.FIRST_FINISH to AchievementUnlock(
                id = AchievementIds.FIRST_FINISH,
                unlockedAtEpochMillis = 40L,
                createdAtEpochMillis = 99L,
            ),
        )
        val afterDelete = AchievementUnlockSync.present(evaluate(books = emptyList()), stored)
        val kept = afterDelete.byId(AchievementIds.FIRST_FINISH)
        assertNotNull(kept.unlockedAtEpochMillis)
        assertEquals(40L, kept.unlockedAtEpochMillis)
        assertTrue(AchievementUnlockSync.toInsert(evaluate(books = emptyList()), stored, nowMillis = 200L).none { it.id == AchievementIds.FIRST_FINISH })
    }

    private fun evaluate(
        books: List<Book> = emptyList(),
        sessions: List<ReadingSession> = emptyList(),
        zoneId: ZoneId = utc,
    ): List<AchievementProgress> = AchievementEvaluator.evaluate(books, sessions, zoneId)

    private fun List<AchievementProgress>.byId(id: String): AchievementProgress = single { it.definition.id == id }

    private fun utcDay(dayOfJanuary2024: Int): Long =
        java.time.LocalDate.of(2024, 1, dayOfJanuary2024)
            .atStartOfDay(utc)
            .toInstant()
            .toEpochMilli()

    private fun book(
        id: String = "book",
        progression: Double? = null,
        addedAt: Long = 1L,
        lastOpenedAt: Long? = null,
    ) = Book(
        id = id,
        title = id,
        author = null,
        filePath = "/tmp/$id.epub",
        coverPath = null,
        lastLocatorJson = null,
        progression = progression,
        addedAt = addedAt,
        lastOpenedAt = lastOpenedAt,
    )

    private fun session(
        id: String = "session",
        bookId: String = "book",
        timerMode: TimerMode = TimerMode.OPEN_ENDED,
        exitPolicy: ExitPolicy = ExitPolicy.CONFIRM,
        start: Long = 1L,
        end: Long? = 2L,
        accumulated: Long = 0L,
        status: SessionStatus = SessionStatus.USER_ENDED,
    ) = ReadingSession(
        id = id,
        bookId = bookId,
        timerMode = timerMode,
        targetDurationMillis = null,
        exitPolicy = exitPolicy,
        startEpochMillis = start,
        plannedEndEpochMillis = null,
        actualEndEpochMillis = end,
        accumulatedReadingMillis = accumulated,
        startLocatorJson = null,
        endLocatorJson = null,
        status = status,
        emergencyExit = false,
        createdAt = start,
        updatedAt = end ?: start,
    )
}
