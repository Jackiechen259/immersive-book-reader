package com.immersive.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.immersive.reader.ui.ImmersiveReaderApp
import dagger.hilt.android.AndroidEntryPoint
import com.immersive.reader.session.ReadingSessionCoordinator
import com.immersive.reader.focus.FocusController
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var sessionCoordinator: ReadingSessionCoordinator
    @Inject lateinit var focusController: FocusController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            if (sessionCoordinator.restore() == null) focusController.exit()
        }
        setContent { ImmersiveReaderApp() }
    }
}
