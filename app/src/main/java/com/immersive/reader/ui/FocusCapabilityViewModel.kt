package com.immersive.reader.ui

import androidx.lifecycle.ViewModel
import com.immersive.reader.focus.FocusCapabilities
import com.immersive.reader.focus.FocusCapabilityDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class FocusCapabilityViewModel @Inject constructor(
    private val detector: FocusCapabilityDetector,
) : ViewModel() {
    private val _capabilities = MutableStateFlow(detector.getCapabilities())
    val capabilities: StateFlow<FocusCapabilities> = _capabilities.asStateFlow()

    fun refresh() {
        _capabilities.value = detector.getCapabilities()
    }
}
