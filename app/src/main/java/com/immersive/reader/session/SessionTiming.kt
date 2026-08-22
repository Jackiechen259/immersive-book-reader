package com.immersive.reader.session

import com.immersive.reader.core.model.ReadingSession
import com.immersive.reader.core.model.TimerMode

object SessionTiming {
    fun elapsedMillis(session: ReadingSession, nowMillis: Long): Long =
        (nowMillis - session.startEpochMillis).coerceAtLeast(0L)

    fun remainingMillis(session: ReadingSession, nowMillis: Long): Long? =
        session.plannedEndEpochMillis?.let { (it - nowMillis).coerceAtLeast(0L) }

    fun isExpired(session: ReadingSession, nowMillis: Long): Boolean =
        session.timerMode == TimerMode.COUNTDOWN &&
            session.plannedEndEpochMillis?.let { it <= nowMillis } == true
}
