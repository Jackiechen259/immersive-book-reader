package com.immersive.reader.session

import android.content.Context
import com.immersive.reader.R
import com.immersive.reader.core.data.ReadingSessionRepository
import com.immersive.reader.core.model.ExitPolicy
import com.immersive.reader.core.model.ReadingSession
import com.immersive.reader.core.model.SessionStatus
import com.immersive.reader.core.model.TimerMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ReadingSessionState {
    data object Idle : ReadingSessionState

    data class Active(
        val session: ReadingSession,
        val elapsedMillis: Long,
        val remainingMillis: Long?,
        val expired: Boolean,
    ) : ReadingSessionState

    data class Finished(val session: ReadingSession) : ReadingSessionState
}

@Singleton
class ReadingSessionCoordinator @Inject constructor(
    private val repository: ReadingSessionRepository,
    @param:ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<ReadingSessionState>(ReadingSessionState.Idle)
    private var tickerJob: Job? = null

    val state: StateFlow<ReadingSessionState> = _state.asStateFlow()

    suspend fun start(
        bookId: String,
        timerMode: TimerMode,
        targetDurationMillis: Long?,
        exitPolicy: ExitPolicy,
        startLocatorJson: String?,
    ): ReadingSession = withContext(Dispatchers.IO) {
        check(repository.getActive() == null) { context.getString(R.string.error_session_already_active) }
        if (timerMode == TimerMode.COUNTDOWN) {
            require(targetDurationMillis != null && targetDurationMillis > 0) {
                context.getString(R.string.error_countdown_not_positive)
            }
        }

        val now = System.currentTimeMillis()
        val session = ReadingSession(
            id = UUID.randomUUID().toString(),
            bookId = bookId,
            timerMode = timerMode,
            targetDurationMillis = targetDurationMillis,
            exitPolicy = exitPolicy,
            startEpochMillis = now,
            plannedEndEpochMillis = targetDurationMillis?.let { now + it },
            actualEndEpochMillis = null,
            accumulatedReadingMillis = 0L,
            startLocatorJson = startLocatorJson,
            endLocatorJson = null,
            status = SessionStatus.ACTIVE,
            emergencyExit = false,
            createdAt = now,
            updatedAt = now,
        )
        repository.insert(session)
        setActiveState(session, now)
        startTicker(session)
        session
    }

    suspend fun restore(): ReadingSession? = withContext(Dispatchers.IO) {
        val session = repository.getActive()
        if (session == null) {
            _state.value = ReadingSessionState.Idle
            return@withContext null
        }

        val now = System.currentTimeMillis()
        if (SessionTiming.isExpired(session, now)) {
            finishInternal(session, SessionStatus.COMPLETED, null, emergencyExit = false, nowMillis = now)
            return@withContext null
        }

        setActiveState(session, now)
        startTicker(session)
        session
    }

    suspend fun finish(
        status: SessionStatus,
        endLocatorJson: String?,
        emergencyExit: Boolean = false,
    ): ReadingSession? = withContext(Dispatchers.IO) {
        val session = repository.getActive() ?: return@withContext null
        finishInternal(session, status, endLocatorJson, emergencyExit, System.currentTimeMillis())
    }

    private suspend fun finishInternal(
        session: ReadingSession,
        status: SessionStatus,
        endLocatorJson: String?,
        emergencyExit: Boolean,
        nowMillis: Long,
    ): ReadingSession {
        val finished = session.copy(
            actualEndEpochMillis = nowMillis,
            accumulatedReadingMillis = SessionTiming.elapsedMillis(session, nowMillis),
            endLocatorJson = endLocatorJson,
            status = status,
            emergencyExit = emergencyExit,
            updatedAt = nowMillis,
        )
        repository.update(finished)
        tickerJob?.cancel()
        _state.value = ReadingSessionState.Finished(finished)
        return finished
    }

    private fun setActiveState(session: ReadingSession, nowMillis: Long) {
        _state.value = ReadingSessionState.Active(
            session = session,
            elapsedMillis = SessionTiming.elapsedMillis(session, nowMillis),
            remainingMillis = SessionTiming.remainingMillis(session, nowMillis),
            expired = SessionTiming.isExpired(session, nowMillis),
        )
    }

    private fun startTicker(session: ReadingSession) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                if (SessionTiming.isExpired(session, now)) {
                    finishInternal(session, SessionStatus.COMPLETED, null, emergencyExit = false, nowMillis = now)
                    break
                }
                setActiveState(session, now)
                delay(TICK_INTERVAL_MILLIS)
            }
        }
    }

    private companion object {
        const val TICK_INTERVAL_MILLIS = 500L
    }
}
