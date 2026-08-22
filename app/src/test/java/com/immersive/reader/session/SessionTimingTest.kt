package com.immersive.reader.session

import com.immersive.reader.core.model.ExitPolicy
import com.immersive.reader.core.model.ReadingSession
import com.immersive.reader.core.model.SessionStatus
import com.immersive.reader.core.model.TimerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTimingTest {
    @Test
    fun openEndedElapsedTimeUsesEpochTimestamp() {
        val session = session(timerMode = TimerMode.OPEN_ENDED, plannedEnd = null)

        assertEquals(90_000L, SessionTiming.elapsedMillis(session, 190_000L))
        assertEquals(null, SessionTiming.remainingMillis(session, 190_000L))
        assertFalse(SessionTiming.isExpired(session, 190_000L))
    }

    @Test
    fun countdownRemainingTimeIsDerivedFromPlannedEnd() {
        val session = session(timerMode = TimerMode.COUNTDOWN, plannedEnd = 250_000L)

        assertEquals(60_000L, SessionTiming.remainingMillis(session, 190_000L))
        assertFalse(SessionTiming.isExpired(session, 190_000L))
        assertTrue(SessionTiming.isExpired(session, 250_000L))
        assertEquals(0L, SessionTiming.remainingMillis(session, 300_000L))
    }

    @Test
    fun elapsedTimeNeverBecomesNegativeAfterClockAdjustment() {
        val session = session(timerMode = TimerMode.OPEN_ENDED, plannedEnd = null)

        assertEquals(0L, SessionTiming.elapsedMillis(session, 90_000L))
    }

    private fun session(timerMode: TimerMode, plannedEnd: Long?): ReadingSession = ReadingSession(
        id = "session",
        bookId = "book",
        timerMode = timerMode,
        targetDurationMillis = plannedEnd?.minus(100_000L),
        exitPolicy = ExitPolicy.HOLD_TO_EXIT,
        startEpochMillis = 100_000L,
        plannedEndEpochMillis = plannedEnd,
        actualEndEpochMillis = null,
        accumulatedReadingMillis = 0L,
        startLocatorJson = null,
        endLocatorJson = null,
        status = SessionStatus.ACTIVE,
        emergencyExit = false,
        createdAt = 100_000L,
        updatedAt = 100_000L,
    )
}
