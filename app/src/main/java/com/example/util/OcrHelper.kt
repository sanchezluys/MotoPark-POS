package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.math.ceil

data class OcrExtractionResult(
    val rawText: String,
    val candidatePlate: String?,
    val candidateBrandOrColor: String?,
    val confidenceScore: Float = 1.0f,
    val errorMessage: String? = null
)

object OcrHelper {

    private const val MIN_MLKIT_DIMENSION = 32
    private const val TARGET_MIN_DIMENSION = 64
    private const val MAX_OCR_DIMENSION = 1280

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
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            return@withContext OcrExtractionResult(
                rawText = "",
                candidatePlate = null,
                candidateBrandOrColor = null,
                errorMessage = "Imagen inválida o vacía"
            )
        }

        var workingBitmap: Bitmap = bitmap
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && workingBitmap.config == Bitmap.Config.HARDWARE) {
                workingBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, false)
            }

            if (workingBitmap.width < MIN_MLKIT_DIMENSION || workingBitmap.height < MIN_MLKIT_DIMENSION) {
                val minSide = minOf(workingBitmap.width, workingBitmap.height).coerceAtLeast(1)
                val scale = TARGET_MIN_DIMENSION.toFloat() / minSide.toFloat()
                val targetW = ceil(workingBitmap.width * scale).toInt().coerceAtLeast(MIN_MLKIT_DIMENSION)
                val targetH = ceil(workingBitmap.height * scale).toInt().coerceAtLeast(MIN_MLKIT_DIMENSION)
                val upscaled = Bitmap.createScaledBitmap(workingBitmap, targetW, targetH, true)
                if (workingBitmap != bitmap && workingBitmap != upscaled) {
                    workingBitmap.recycle()
                }
                workingBitmap = upscaled
            }

            val downsampled = PhotoStorageHelper.downsampleBitmap(workingBitmap, MAX_OCR_DIMENSION)
            if (workingBitmap != bitmap && workingBitmap != downsampled) {
                workingBitmap.recycle()
            }
            workingBitmap = downsampled

            val image = InputImage.fromBitmap(workingBitmap, 0)
            processImage(image)
        } catch (e: Exception) {
            OcrExtractionResult(
                rawText = "",
                candidatePlate = null,
                candidateBrandOrColor = null,
                errorMessage = e.localizedMessage ?: "No se pudo analizar la imagen"
            )
        } finally {
            if (workingBitmap != bitmap && !workingBitmap.isRecycled) {
                workingBitmap.recycle()
            }
        }
    }

    suspend fun recognizeTextFromFile(context: Context, filePath: String): OcrExtractionResult = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists() || file.length() <= 0L) {
                return@withContext OcrExtractionResult(
                    rawText = "",
                    candidatePlate = null,
                    candidateBrandOrColor = null,
                    errorMessage = "El archivo de imagen no existe"
                )
            }
            val decoded = PhotoStorageHelper.loadDownsampledBitmap(
                filePath = file.absolutePath,
                reqWidth = MAX_OCR_DIMENSION,
                reqHeight = MAX_OCR_DIMENSION,
                preferLowMemory = false
            ) ?: return@withContext OcrExtractionResult(
                rawText = "",
                candidatePlate = null,
                candidateBrandOrColor = null,
                errorMessage = "No se pudo decodificar la fotografía"
            )

            val oriented = PhotoStorageHelper.applyExifOrientation(file.absolutePath, decoded)
            try {
                recognizeTextFromBitmap(oriented)
            } finally {
                if (!oriented.isRecycled) {
                    oriented.recycle()
                }
            }
        } catch (e: Exception) {
            OcrExtractionResult(
                rawText = "",
                candidatePlate = null,
                candidateBrandOrColor = null,
                errorMessage = e.localizedMessage ?: "Error leyendo la fotografía"
            )
        }
    }

    suspend fun recognizeTextFromUri(context: Context, uri: Uri): OcrExtractionResult = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            processImage(image)
        } catch (e: Exception) {
            OcrExtractionResult(
                rawText = "",
                candidatePlate = null,
                candidateBrandOrColor = null,
                errorMessage = e.localizedMessage ?: "Error cargando la imagen"
            )
        }
    }

    private suspend fun processImage(image: InputImage): OcrExtractionResult =
        suspendCancellableCoroutine { continuation ->
            try {
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        if (!continuation.isActive) return@addOnSuccessListener
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
                        if (!continuation.isActive) return@addOnFailureListener
                        continuation.resume(
                            OcrExtractionResult(
                                rawText = "",
                                candidatePlate = null,
                                candidateBrandOrColor = null,
                                errorMessage = exception.localizedMessage ?: "Error en el motor OCR"
                            )
                        )
                    }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(
                        OcrExtractionResult(
                            rawText = "",
                            candidatePlate = null,
                            candidateBrandOrColor = null,
                            errorMessage = e.localizedMessage ?: "Error iniciando el lector OCR"
                        )
                    )
                }
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
