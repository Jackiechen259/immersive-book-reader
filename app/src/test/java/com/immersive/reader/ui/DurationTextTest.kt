package com.immersive.reader.ui

import com.immersive.reader.ui.components.formatClock
import com.immersive.reader.ui.components.formatProgressPercent
import com.immersive.reader.ui.components.formatReadingDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationTextTest {
    @Test
    fun humanDurationFloorsToMinutesAndShowsSecondsUnderOneMinute() {
        assertEquals("0 min", formatReadingDuration(0L))
        assertEquals("45s", formatReadingDuration(45_000L))
        assertEquals("1 min", formatReadingDuration(90_000L))
        assertEquals("32 min", formatReadingDuration(32 * 60_000L))
        assertEquals("1h 30m", formatReadingDuration(90 * 60_000L))
        assertEquals("2h", formatReadingDuration(120 * 60_000L))
    }

    @Test
    fun clockFormatsMinutesAndHours() {
        assertEquals("01:30", formatClock(90_000L))
        assertEquals("1:00:00", formatClock(3_600_000L))
        assertEquals("00:00", formatClock(0L))
    }

    @Test
    fun progressPercentIsWholeNumber() {
        assertEquals("42%", formatProgressPercent(0.42))
        assertEquals("0%", formatProgressPercent(0.0))
    }
}
