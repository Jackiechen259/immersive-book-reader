package com.immersive.reader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immersive.reader.core.model.SessionStatus
import com.immersive.reader.focus.FocusController
import com.immersive.reader.session.ReadingSessionCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SessionRecoveryViewModel @Inject constructor(
    private val coordinator: ReadingSessionCoordinator,
    private val focusController: FocusController,
) : ViewModel() {
    fun endRecoveredSession(onEnded: (String) -> Unit) {
        viewModelScope.launch {
            coordinator.finish(SessionStatus.USER_ENDED, endLocatorJson = null)?.let {
                focusController.exit()
                onEnded(it.id)
            }
        }
    }
}
