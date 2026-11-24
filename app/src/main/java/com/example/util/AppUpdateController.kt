package com.example.util

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AppUpdateState {
    data object Idle : AppUpdateState
    data object Checking : AppUpdateState
    data class Available(
        val availableVersionCode: Int,
        val updatePriority: Int,
        val isImmediateAllowed: Boolean,
        val isFlexibleAllowed: Boolean,
        val isBannerDismissed: Boolean = false
    ) : AppUpdateState
    data class Downloading(val progressPercent: Int) : AppUpdateState
    data object Downloaded : AppUpdateState
    data object UpToDate : AppUpdateState
    data class Unavailable(val reason: String) : AppUpdateState
}

/**
 * Manages Google Play In-App Updates following official Google Play Core best practices:
 * - Supports Immediate updates for high-priority releases (priority >= 4) and resumes stalled updates.
 * - Supports Flexible background updates for normal releases so POS operations are not interrupted.
 * - Monitors download progress and prompts for restart when InstallStatus.DOWNLOADED is reached.
 * - Fails silently on automatic checks if installed outside Google Play or offline.
 */
class AppUpdateController(context: Context) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context.applicationContext)

    private val _updateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val updateState: StateFlow<AppUpdateState> = _updateState.asStateFlow()

    private var cachedAppUpdateInfo: AppUpdateInfo? = null
    private var updateLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var hasAutoPromptedThisSession = false
    private var isListenerRegistered = false

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val total = state.totalBytesToDownload()
                val downloaded = state.bytesDownloaded()
                val percent = if (total > 0L) {
                    ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
                } else {
                    0
                }
                _updateState.value = AppUpdateState.Downloading(percent)
            }
            InstallStatus.DOWNLOADED -> {
                _updateState.value = AppUpdateState.Downloaded
            }
            InstallStatus.INSTALLED -> {
                unregisterListenerIfNeeded()
                _updateState.value = AppUpdateState.Idle
            }
            InstallStatus.FAILED, InstallStatus.CANCELED -> {
                unregisterListenerIfNeeded()
                val info = cachedAppUpdateInfo
                if (info != null && info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    _updateState.value = AppUpdateState.Available(
                        availableVersionCode = info.availableVersionCode(),
                        updatePriority = info.updatePriority(),
                        isImmediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
                        isFlexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
                        isBannerDismissed = hasAutoPromptedThisSession
                    )
                } else {
                    _updateState.value = AppUpdateState.Idle
                }
            }
            else -> Unit
        }
    }

    fun attachLauncher(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        updateLauncher = launcher
    }

    fun onUpdateFlowResult(resultCode: Int) {
        if (resultCode != Activity.RESULT_OK) {
            hasAutoPromptedThisSession = true
            val current = _updateState.value
            if (current is AppUpdateState.Available) {
                _updateState.value = current.copy(isBannerDismissed = false)
            }
        }
    }

    fun checkForUpdates(userInitiated: Boolean = false) {
        if (userInitiated) {
            _updateState.value = AppUpdateState.Checking
        }

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                cachedAppUpdateInfo = info

                // 1. If a flexible update was already downloaded, prompt user to restart
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    _updateState.value = AppUpdateState.Downloaded
                    return@addOnSuccessListener
                }

                // 2. If an immediate update is already in progress, resume it immediately
                if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    launchUpdateFlow(info, AppUpdateType.IMMEDIATE)
                    return@addOnSuccessListener
                }

                // 3. If a new update is available on Google Play Store
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    val isImmediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                    val isFlexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                    val priority = info.updatePriority()

                    if (priority >= HIGH_PRIORITY_THRESHOLD && isImmediateAllowed) {
                        launchUpdateFlow(info, AppUpdateType.IMMEDIATE)
                    } else if (isFlexibleAllowed || isImmediateAllowed) {
                        _updateState.value = AppUpdateState.Available(
                            availableVersionCode = info.availableVersionCode(),
                            updatePriority = priority,
                            isImmediateAllowed = isImmediateAllowed,
                            isFlexibleAllowed = isFlexibleAllowed,
                            isBannerDismissed = false
                        )

                        if (userInitiated || !hasAutoPromptedThisSession) {
                            hasAutoPromptedThisSession = true
                            val preferredType = if (isFlexibleAllowed) AppUpdateType.FLEXIBLE else AppUpdateType.IMMEDIATE
                            launchUpdateFlow(info, preferredType)
                        }
                    }
                    return@addOnSuccessListener
                }

                // 4. App is up to date
                if (userInitiated) {
                    _updateState.value = AppUpdateState.UpToDate
                } else if (_updateState.value !is AppUpdateState.Downloading &&
                    _updateState.value !is AppUpdateState.Downloaded
                ) {
                    _updateState.value = AppUpdateState.Idle
                }
            }
            .addOnFailureListener { error ->
                if (userInitiated) {
                    _updateState.value = AppUpdateState.Unavailable(
                        error.localizedMessage ?: "No se pudo conectar con Google Play Store"
                    )
                }
            }
    }

    fun startFlexibleOrImmediateUpdate() {
        val info = cachedAppUpdateInfo
        if (info == null) {
            checkForUpdates(userInitiated = true)
            return
        }
        val type = when {
            info.updatePriority() >= HIGH_PRIORITY_THRESHOLD &&
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
            else -> return
        }
        launchUpdateFlow(info, type)
    }

    fun dismissBanner() {
        hasAutoPromptedThisSession = true
        val current = _updateState.value
        if (current is AppUpdateState.Available) {
            _updateState.value = current.copy(isBannerDismissed = true)
        }
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    fun dispose() {
        unregisterListenerIfNeeded()
        updateLauncher = null
    }

    private fun launchUpdateFlow(info: AppUpdateInfo, updateType: Int) {
        val launcher = updateLauncher ?: return
        if (updateType == AppUpdateType.FLEXIBLE) {
            registerListenerIfNeeded()
        }
        val options = AppUpdateOptions.newBuilder(updateType).build()
        try {
            appUpdateManager.startUpdateFlowForResult(info, launcher, options)
        } catch (_: Exception) {
            // Ignore intent sender failures gracefully
        }
    }

    private fun registerListenerIfNeeded() {
        if (!isListenerRegistered) {
            appUpdateManager.registerListener(installStateUpdatedListener)
            isListenerRegistered = true
        }
    }

    private fun unregisterListenerIfNeeded() {
        if (isListenerRegistered) {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
            isListenerRegistered = false
        }
    }

    companion object {
        private const val HIGH_PRIORITY_THRESHOLD = 4
    }
}
