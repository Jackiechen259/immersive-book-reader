package com.immersive.reader.focus

import android.app.Activity
import android.os.Build
import com.immersive.reader.core.datastore.ReaderPreferencesRepository
import com.immersive.reader.core.model.ExitPolicy
import com.immersive.reader.core.model.ReadingSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class AndroidFocusController @Inject constructor(
    private val capabilitiesDetector: FocusCapabilityDetector,
    private val immersiveController: ImmersiveController,
    private val dndController: DndController,
    private val preferencesRepository: ReaderPreferencesRepository,
    private val devicePolicyController: DevicePolicyController,
) : FocusController {
    private var activity: Activity? = null
    private var activeSession: ReadingSession? = null
    private var activeTier: FocusTier = FocusTier.IMMERSIVE

    override fun attach(activity: Activity) {
        this.activity = activity
    }

    override fun detach(activity: Activity) {
        if (this.activity === activity) this.activity = null
    }

    override fun getCapabilities(): FocusCapabilities = capabilitiesDetector.getCapabilities()

    override suspend fun enter(session: ReadingSession) {
        val host = activity ?: return
        activeSession = session
        immersiveController.enter(host)
        if (preferencesRepository.preferences.first().useDnd) dndController.enter()

        if (session.exitPolicy == ExitPolicy.CONFIRM || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            activeTier = FocusTier.IMMERSIVE
            return
        }

        val capabilities = getCapabilities()
        val canUseDeviceOwnerLock = capabilities.isDeviceOwner && devicePolicyController.configureLockTask()
        if (canUseDeviceOwnerLock || capabilities.lockTaskPermitted || capabilities.screenPinningAvailable) {
            runCatching {
                host.startLockTask()
                activeTier = if (canUseDeviceOwnerLock || capabilities.lockTaskPermitted) FocusTier.LOCK_TASK else FocusTier.PINNED
            }
        }
    }

    override suspend fun exit() {
        val host = activity
        if (host != null && activeTier != FocusTier.IMMERSIVE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching { host.stopLockTask() }
        }
        dndController.exit()
        host?.let(immersiveController::exit)
        activeTier = FocusTier.IMMERSIVE
        activeSession = null
    }

    override suspend fun restore() {
        val session = activeSession
        if (session == null) exit() else enter(session)
    }
}
