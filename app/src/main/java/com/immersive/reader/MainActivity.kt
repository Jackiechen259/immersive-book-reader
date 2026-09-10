package com.immersive.reader

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.immersive.reader.focus.FocusController
import com.immersive.reader.session.ReadingSessionCoordinator
import com.immersive.reader.ui.ImmersiveReaderApp
import com.immersive.reader.update.ApkInstaller
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var sessionCoordinator: ReadingSessionCoordinator
    @Inject lateinit var focusController: FocusController
    @Inject lateinit var apkInstaller: ApkInstaller

    private var resumeInstall = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            if (sessionCoordinator.restore() == null) focusController.exit()
        }
        setContent { ImmersiveReaderApp() }
    }

    override fun onResume() {
        super.onResume()
        if (!resumeInstall) return
        resumeInstall = false
        if (apkInstaller.canInstallPackages()) {
            startActivity(apkInstaller.installIntent())
        }
    }

    fun requestApkInstall() {
        if (apkInstaller.canInstallPackages()) {
            startActivity(apkInstaller.installIntent())
        } else {
            resumeInstall = true
            startActivity(apkInstaller.unknownSourcesSettingsIntent())
        }
    }
}
