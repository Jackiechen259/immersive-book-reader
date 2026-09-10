package com.immersive.reader.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immersive.reader.update.DataStoreUpdatePreferences
import com.immersive.reader.update.UpdateCoordinator
import com.immersive.reader.update.UpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val coordinator: UpdateCoordinator,
    preferences: DataStoreUpdatePreferences,
) : ViewModel() {
    val state: StateFlow<UpdateState> = coordinator.state
    val skippedTag: StateFlow<String?> = preferences.skippedTagFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )
    private val _dismissedTag = MutableStateFlow<String?>(null)
    val dismissedTag: StateFlow<String?> = _dismissedTag
    private val _offerAutoPrompt = MutableStateFlow(false)
    val offerAutoPrompt: StateFlow<Boolean> = _offerAutoPrompt

    private var downloadJob: Job? = null
    var installAfterDownload: Boolean = false
        private set

    fun maybeAutoCheck() {
        viewModelScope.launch {
            coordinator.maybeAutoCheck()
            _offerAutoPrompt.value = coordinator.state.value is UpdateState.Available
        }
    }

    fun check() {
        _offerAutoPrompt.value = false
        viewModelScope.launch { coordinator.check() }
    }

    fun download(installWhenReady: Boolean = true) {
        _offerAutoPrompt.value = false
        installAfterDownload = installWhenReady
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch { coordinator.download() }
    }

    fun cancelDownload() {
        installAfterDownload = false
        coordinator.cancelDownload()
        downloadJob?.cancel()
    }

    fun later() {
        _offerAutoPrompt.value = false
        val tag = (coordinator.state.value as? UpdateState.Available)?.release?.tagName ?: return
        _dismissedTag.value = tag
    }

    fun skip() {
        later()
        viewModelScope.launch { coordinator.skip() }
    }

    fun consumeInstallAfterDownload(): Boolean {
        val shouldInstall = installAfterDownload && coordinator.state.value is UpdateState.ReadyToInstall
        if (shouldInstall) installAfterDownload = false
        return shouldInstall
    }
}
