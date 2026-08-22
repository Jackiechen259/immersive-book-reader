package com.immersive.reader.ui

import androidx.lifecycle.ViewModel
import com.immersive.reader.session.ReadingSessionCoordinator
import com.immersive.reader.session.ReadingSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SessionStateViewModel @Inject constructor(
    coordinator: ReadingSessionCoordinator,
) : ViewModel() {
    val state: StateFlow<ReadingSessionState> = coordinator.state
}
