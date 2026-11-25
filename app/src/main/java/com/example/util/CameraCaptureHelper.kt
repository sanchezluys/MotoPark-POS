package com.example.util

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.R
import java.io.File

/**
 * Remembers a camera capture trigger that safely verifies and requests [Manifest.permission.CAMERA]
 * at runtime before launching [ActivityResultContracts.TakePicture] with a FileProvider URI.
 */
@Composable
fun rememberCameraCaptureLauncher(
    prefix: String,
    useCacheDir: Boolean = false,
    onPhotoFileCaptured: (File) -> Unit,
    onCaptureError: ((String) -> Unit)? = null
): () -> Unit {
    val context = LocalContext.current
    val currentOnCaptured by rememberUpdatedState(onPhotoFileCaptured)
    val currentOnError by rememberUpdatedState(onCaptureError)

    var pendingFilePath by rememberSaveable { mutableStateOf<String?>(null) }

    fun reportError(message: String) {
        val handler = currentOnError
        if (handler != null) {
            handler(message)
        } else {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val path = pendingFilePath
        pendingFilePath = null
        if (path != null) {
            val file = File(path)
            if (success && file.exists() && file.length() > 0L) {
                currentOnCaptured(file)
            } else {
                file.delete()
                if (success) {
                    reportError(context.getString(R.string.camera_capture_error))
                }
            }
        }
    }

    fun launchCameraIntent() {
        try {
            val (uri, file) = PhotoStorageHelper.createTempImageUri(
                context = context,
                prefix = prefix,
                useCacheDir = useCacheDir
            )
            pendingFilePath = file.absolutePath
            takePictureLauncher.launch(uri)
        } catch (_: ActivityNotFoundException) {
            pendingFilePath?.let { File(it).delete() }
            pendingFilePath = null
            reportError(context.getString(R.string.camera_not_available))
        } catch (_: SecurityException) {
            pendingFilePath?.let { File(it).delete() }
            pendingFilePath = null
            reportError(context.getString(R.string.camera_permission_required))
        } catch (_: Exception) {
            pendingFilePath?.let { File(it).delete() }
            pendingFilePath = null
            reportError(context.getString(R.string.camera_capture_error))
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            launchCameraIntent()
        } else {
            reportError(context.getString(R.string.camera_permission_required))
        }
    }

    return {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            launchCameraIntent()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}
