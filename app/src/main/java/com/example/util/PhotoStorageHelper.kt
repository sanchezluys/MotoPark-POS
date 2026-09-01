package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PhotoStorageHelper {

    fun getPhotosDirectory(context: Context): File {
        val photosDir = File(context.filesDir, "parking_photos")
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }
        return photosDir
    }

    fun createTempImageUri(context: Context, prefix: String = "photo"): Pair<Uri, File> {
        val photosDir = getPhotosDirectory(context)
        val file = File.createTempFile("${prefix}_${System.currentTimeMillis()}_", ".jpg", photosDir)
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        return Pair(uri, file)
    }

    suspend fun saveBitmapToFile(context: Context, bitmap: Bitmap, prefix: String): String = withContext(Dispatchers.IO) {
        val photosDir = getPhotosDirectory(context)
        val file = File(photosDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.flush()
        }
        file.absolutePath
    }

    fun loadBitmap(filePath: String): Bitmap? {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
