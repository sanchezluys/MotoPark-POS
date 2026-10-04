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

    suspend fun saveBitmapToFile(
        context: Context,
        bitmap: Bitmap,
        prefix: String,
        maxDimension: Int = 1280
    ): String = withContext(Dispatchers.IO) {
        val photosDir = getPhotosDirectory(context)
        val file = File(photosDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        val scaledBitmap = downsampleBitmap(bitmap, maxDimension)
        FileOutputStream(file).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.flush()
        }
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        file.absolutePath
    }

    /**
     * Calculates the optimal inSampleSize for BitmapFactory options according to Google Play
     * memory optimization guidelines.
     */
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than or equal to the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    /**
     * Loads a bitmap from disk using efficient downsampling (inSampleSize) and optimal pixel format
     * to avoid OutOfMemory errors and excessive RAM consumption.
     */
    fun loadDownsampledBitmap(
        filePath: String,
        reqWidth: Int = 1024,
        reqHeight: Int = 1024,
        preferLowMemory: Boolean = true
    ): Bitmap? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            // First decode with inJustDecodeBounds=true to check dimensions without allocating memory
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            if (preferLowMemory) {
                options.inPreferredConfig = Bitmap.Config.RGB_565
            }

            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Loads a downsampled bitmap from an Android content Uri.
     */
    fun loadDownsampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int = 1024,
        reqHeight: Int = 1024,
        preferLowMemory: Boolean = true
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            if (preferLowMemory) {
                options.inPreferredConfig = Bitmap.Config.RGB_565
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Downsamples a bitmap to a maximum width or height to prevent large memory spikes.
     */
    fun downsampleBitmap(bitmap: Bitmap, maxDimension: Int = 1280): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    fun loadBitmap(filePath: String): Bitmap? {
        return loadDownsampledBitmap(filePath, reqWidth = 1024, reqHeight = 1024)
    }
}
