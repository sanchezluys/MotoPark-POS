package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern
import kotlin.coroutines.resume

data class OcrExtractionResult(
    val rawText: String,
    val candidatePlate: String?,
    val candidateBrandOrColor: String?,
    val confidenceScore: Float = 1.0f
)

object OcrHelper {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    // Common motorcycle & car plate patterns in Colombia / Latin America:
    // Moto: 3 letters + 2 numbers + 1 letter (e.g. ABC 12D, ABC12D) or 3 letters + 3 numbers (ABC123)
    private val plateRegexes = listOf(
        Pattern.compile("\\b([A-Z]{3})\\s*[-]?\\s*([0-9]{2})\\s*([A-Z])\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b([A-Z]{3})\\s*[-]?\\s*([0-9]{3})\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b([A-Z]{1,3})\\s*[-]?\\s*([0-9]{2,4})\\s*([A-Z]?)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b([0-9]{3})\\s*[-]?\\s*([A-Z]{3})\\b", Pattern.CASE_INSENSITIVE)
    )

    private val brandKeywords = listOf(
        "YAMAHA", "HONDA", "BAJAJ", "PULSAR", "SUZUKI", "AKT", "AUTECO", "KTM", "HERO",
        "KAWASAKI", "TVS", "BMW", "DUCATI", "BWS", "NMAX", "AGILITY", "DISCOVER",
        "CHEVROLET", "RENAULT", "MAZDA", "TOYOTA", "KIA", "HYUNDAI", "NISSAN", "FORD", "VOLKSWAGEN"
    )

    private val colorKeywords = listOf(
        "NEGRO", "NEGRA", "BLANCO", "BLANCA", "ROJO", "ROJA", "AZUL", "GRIS", "PLATA",
        "VERDE", "AMARILLO", "AMARILLA", "NARANJA", "MATE"
    )

    suspend fun recognizeTextFromBitmap(bitmap: Bitmap): OcrExtractionResult = withContext(Dispatchers.Default) {
        val downsampled = PhotoStorageHelper.downsampleBitmap(bitmap, 1280)
        val image = InputImage.fromBitmap(downsampled, 0)
        val result = processImage(image)
        if (downsampled != bitmap) {
            downsampled.recycle()
        }
        result
    }

    suspend fun recognizeTextFromFile(context: Context, filePath: String): OcrExtractionResult = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext OcrExtractionResult(
                    rawText = "",
                    candidatePlate = null,
                    candidateBrandOrColor = null
                )
            }
            val bitmap = PhotoStorageHelper.loadDownsampledBitmap(file.absolutePath, 1280, 1280, preferLowMemory = false)
            if (bitmap != null) {
                val image = InputImage.fromBitmap(bitmap, 0)
                val result = processImage(image)
                bitmap.recycle()
                result
            } else {
                OcrExtractionResult("", null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            OcrExtractionResult("", null, null)
        }
    }

    suspend fun recognizeTextFromUri(context: Context, uri: Uri): OcrExtractionResult = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            processImage(image)
        } catch (e: Exception) {
            e.printStackTrace()
            OcrExtractionResult("", null, null)
        }
    }

    private suspend fun processImage(image: InputImage): OcrExtractionResult =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val fullText = visionText.text
                    val candidatePlate = extractPlateCandidate(fullText)
                    val candidateBrandOrColor = extractBrandOrColor(fullText)

                    continuation.resume(
                        OcrExtractionResult(
                            rawText = fullText.trim(),
                            candidatePlate = candidatePlate,
                            candidateBrandOrColor = candidateBrandOrColor
                        )
                    )
                }
                .addOnFailureListener { exception ->
                    continuation.resume(
                        OcrExtractionResult(
                            rawText = "Error al procesar imagen: ${exception.localizedMessage ?: "Desconocido"}",
                            candidatePlate = null,
                            candidateBrandOrColor = null
                        )
                    )
                }
        }

    fun extractPlateCandidate(text: String): String? {
        val cleanUpper = text.uppercase().replace("\n", " ")

        for (regex in plateRegexes) {
            val matcher = regex.matcher(cleanUpper)
            if (matcher.find()) {
                val candidate = matcher.group(0)?.replace(" ", "")?.replace("-", "")?.trim()
                if (!candidate.isNullOrBlank() && candidate.length in 5..7) {
                    return candidate
                }
            }
        }

        // Fallback: look for 6-character alphanumeric tokens (e.g. ABC12D or ABC123)
        val tokens = cleanUpper.split(Regex("[^A-Z0-9]+"))
        for (token in tokens) {
            if (token.length in 5..6 && token.any { it.isLetter() } && token.any { it.isDigit() }) {
                return token
            }
        }

        return null
    }

    private fun extractBrandOrColor(text: String): String? {
        val cleanUpper = text.uppercase()
        val foundBrands = brandKeywords.filter { cleanUpper.contains(it) }
        val foundColors = colorKeywords.filter { cleanUpper.contains(it) }

        val parts = mutableListOf<String>()
        if (foundBrands.isNotEmpty()) parts.add(foundBrands.first())
        if (foundColors.isNotEmpty()) parts.add(foundColors.first())

        return if (parts.isNotEmpty()) parts.joinToString(" ") else null
    }
}
