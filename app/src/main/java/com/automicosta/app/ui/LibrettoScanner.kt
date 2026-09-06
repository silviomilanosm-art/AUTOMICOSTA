package com.automicosta.app.ui

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** Dati principali estratti dalla carta di circolazione italiana/europea. */
data class LibrettoData(
    val plate: String = "",
    val firstRegistrationDate: String = "",
    val brand: String = "",
    val model: String = "",
    val vin: String = "",
    val displacementCc: Int? = null,
    val powerKw: Double? = null,
    val fuelType: String = "",
    val bodyType: String = "",
    val rawText: String = ""
)

fun scanLibretto(
    context: Context,
    uri: Uri,
    onResult: (Result<LibrettoData>) -> Unit
) {
    val image = runCatching { InputImage.fromFilePath(context, uri) }
        .getOrElse { onResult(Result.failure(it)); return }

    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    recognizer.process(image)
        .addOnSuccessListener { result ->
            onResult(Result.success(parseLibrettoText(result.text)))
        }
        .addOnFailureListener { onResult(Result.failure(it)) }
        .addOnCompleteListener { recognizer.close() }
}

private fun parseLibrettoText(raw: String): LibrettoData {
    val normalized = raw.replace('\u00A0', ' ')
    val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }

    fun valueAfter(code: String): String {
        val escaped = Regex.escape(code)
        val inline = Regex("(?i)(?:^|\\s)[(]?$escaped[)]?[.:]?\\s+(.+)$")
        lines.forEachIndexed { index, line ->
            inline.find(line)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
            if (line.matches(Regex("(?i)^[(]?$escaped[)]?[.:]?$"))) {
                return lines.getOrNull(index + 1).orEmpty()
            }
        }
        return ""
    }

    fun firstMatching(regex: Regex): String = regex.find(normalized)?.value.orEmpty()

    val vin = valueAfter("E")
        .uppercase()
        .replace(" ", "")
        .takeIf { it.matches(Regex("[A-HJ-NPR-Z0-9]{17}")) }
        ?: firstMatching(Regex("\\b[A-HJ-NPR-Z0-9]{17}\\b"))

    val plateCandidate = valueAfter("A").uppercase().replace(" ", "")
    val plate = plateCandidate.takeIf { it.length in 5..8 && it.matches(Regex("[A-Z0-9]+")) }
        ?: firstMatching(Regex("\\b[A-Z]{2}[0-9]{3}[A-Z]{2}\\b"))

    val registration = valueAfter("B")
        .replace('.', '/')
        .replace('-', '/')
        .takeIf { it.matches(Regex("\\d{1,2}/\\d{1,2}/\\d{4}")) }
        .orEmpty()

    val brand = valueAfter("D.1")
    val model = valueAfter("D.3").ifBlank { valueAfter("D.2") }
    val displacement = valueAfter("P.1").filter { it.isDigit() }.toIntOrNull()
    val power = valueAfter("P.2").replace(',', '.').let { Regex("\\d+(?:\\.\\d+)?").find(it)?.value?.toDoubleOrNull() }
    val fuelRaw = valueAfter("P.3")
    val fuel = when {
        fuelRaw.contains("benz", ignoreCase = true) -> "Benzina"
        fuelRaw.contains("diesel", ignoreCase = true) || fuelRaw.contains("gasolio", ignoreCase = true) -> "Diesel"
        fuelRaw.contains("elet", ignoreCase = true) -> "Elettrica"
        fuelRaw.contains("ibrid", ignoreCase = true) -> "Ibrida"
        fuelRaw.contains("gpl", ignoreCase = true) -> "GPL"
        fuelRaw.contains("metano", ignoreCase = true) || fuelRaw.contains("cng", ignoreCase = true) -> "Metano"
        else -> fuelRaw
    }
    val body = valueAfter("J.2").ifBlank { valueAfter("J") }

    return LibrettoData(
        plate = plate,
        firstRegistrationDate = registration,
        brand = brand,
        model = model,
        vin = vin,
        displacementCc = displacement,
        powerKw = power,
        fuelType = fuel,
        bodyType = body,
        rawText = normalized.trim()
    )
}
