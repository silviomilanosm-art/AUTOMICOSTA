package com.automicosta.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.max
import kotlin.math.min

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

/**
 * Legge un PDF del libretto senza inviarlo a servizi esterni.
 * Le prime 4 pagine vengono renderizzate localmente e passate all'OCR on-device.
 */
fun scanLibrettoPdf(
    context: Context,
    uri: Uri,
    onResult: (Result<LibrettoData>) -> Unit
) {
    val mainHandler = Handler(Looper.getMainLooper())

    Thread {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw IllegalArgumentException("Impossibile aprire il PDF")

            val renderer = PdfRenderer(descriptor)
            val pagesToRead = min(renderer.pageCount, 4)
            if (pagesToRead <= 0) throw IllegalArgumentException("Il PDF non contiene pagine leggibili")

            repeat(pagesToRead) { index ->
                val page = renderer.openPage(index)
                try {
                    val longestSide = max(page.width, page.height).coerceAtLeast(1)
                    val scale = min(2.0f, 2200f / longestSide.toFloat()).coerceAtLeast(1.0f)
                    val width = (page.width * scale).toInt().coerceAtLeast(1)
                    val height = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps += bitmap
                } finally {
                    page.close()
                }
            }
            renderer.close()
            descriptor.close()

            mainHandler.post {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val texts = mutableListOf<String>()

                fun finish(result: Result<LibrettoData>) {
                    bitmaps.forEach { if (!it.isRecycled) it.recycle() }
                    recognizer.close()
                    onResult(result)
                }

                fun readPage(index: Int) {
                    if (index >= bitmaps.size) {
                        val merged = texts.joinToString("\n\n--- PAGINA PDF ---\n\n")
                        finish(Result.success(parseLibrettoText(merged)))
                        return
                    }

                    val image = InputImage.fromBitmap(bitmaps[index], 0)
                    recognizer.process(image)
                        .addOnSuccessListener { result ->
                            texts += result.text
                            readPage(index + 1)
                        }
                        .addOnFailureListener { error -> finish(Result.failure(error)) }
                }

                readPage(0)
            }
        } catch (error: Throwable) {
            bitmaps.forEach { if (!it.isRecycled) it.recycle() }
            mainHandler.post { onResult(Result.failure(error)) }
        }
    }.start()
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
    fun cleanNumber(value: String): String = Regex("\\d+(?:[.,]\\d+)?").find(value)?.value.orEmpty()
    fun addTechnical(target: MutableList<String>, code: String, label: String, value: String) {
        if (value.isNotBlank()) target += "$code · $label: $value"
    }

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
        .let { candidate -> Regex("\\d{1,2}/\\d{1,2}/\\d{4}").find(candidate)?.value.orEmpty() }

    val brand = valueAfter("D.1")
    val typeVariantVersion = valueAfter("D.2")
    val commercialName = valueAfter("D.3")
    val model = commercialName.ifBlank { typeVariantVersion }

    val p1 = valueAfter("P.1")
    val p2 = valueAfter("P.2")
    val p3 = valueAfter("P.3")
    val displacement = cleanNumber(p1).substringBefore(',').substringBefore('.').toIntOrNull()
    val power = cleanNumber(p2).replace(',', '.').toDoubleOrNull()

    val fuel = when {
        p3.contains("benz", ignoreCase = true) -> "Benzina"
        p3.contains("diesel", ignoreCase = true) || p3.contains("gasolio", ignoreCase = true) -> "Diesel"
        p3.contains("elet", ignoreCase = true) -> "Elettrica"
        p3.contains("ibrid", ignoreCase = true) -> "Ibrida"
        p3.contains("gpl", ignoreCase = true) -> "GPL"
        p3.contains("metano", ignoreCase = true) || p3.contains("cng", ignoreCase = true) -> "Metano"
        else -> p3
    }

    val j = valueAfter("J")
    val j1 = valueAfter("J.1")
    val j2 = valueAfter("J.2")
    val body = j2.ifBlank { j1.ifBlank { j } }

    val f1 = valueAfter("F.1")
    val f2 = valueAfter("F.2")
    val f3 = valueAfter("F.3")
    val k = valueAfter("K")
    val p5 = valueAfter("P.5")
    val q = valueAfter("Q")
    val s1 = valueAfter("S.1")
    val s2 = valueAfter("S.2")
    val t = valueAfter("T")
    val v7 = valueAfter("V.7")
    val v9 = valueAfter("V.9")

    val tireMatches = Regex("(?i)\\b\\d{3}/\\d{2}\\s*[RZ]R?\\s*\\d{2}(?:\\s*\\d{2,3}[A-Z])?\\b")
        .findAll(normalized)
        .map { it.value.replace(Regex("\\s+"), " ").uppercase() }
        .distinct()
        .take(8)
        .toList()

    val technical = mutableListOf<String>()
    addTechnical(technical, "A", "Targa", plate)
    addTechnical(technical, "B", "Prima immatricolazione", registration)
    addTechnical(technical, "D.1", "Marca", brand)
    addTechnical(technical, "D.2", "Tipo / variante / versione", typeVariantVersion)
    addTechnical(technical, "D.3", "Denominazione commerciale", commercialName)
    addTechnical(technical, "E", "Telaio VIN", vin)
    addTechnical(technical, "F.1", "Massa massima tecnicamente ammissibile", f1)
    addTechnical(technical, "F.2", "Massa massima ammissibile in servizio", f2)
    addTechnical(technical, "F.3", "Massa massima complesso", f3)
    addTechnical(technical, "J", "Categoria del veicolo", j)
    addTechnical(technical, "J.1", "Destinazione / uso", j1)
    addTechnical(technical, "J.2", "Carrozzeria", j2)
    addTechnical(technical, "K", "Numero di omologazione", k)
    addTechnical(technical, "P.1", "Cilindrata cm³", p1)
    addTechnical(technical, "P.2", "Potenza netta massima kW", p2)
    addTechnical(technical, "P.3", "Alimentazione", p3)
    addTechnical(technical, "P.5", "Identificazione motore", p5)
    addTechnical(technical, "Q", "Rapporto potenza/massa", q)
    addTechnical(technical, "S.1", "Posti a sedere", s1)
    addTechnical(technical, "S.2", "Posti in piedi", s2)
    addTechnical(technical, "T", "Velocità massima km/h", t)
    addTechnical(technical, "V.7", "CO₂ g/km", v7)
    addTechnical(technical, "V.9", "Classe ambientale", v9)
    if (tireMatches.isNotEmpty()) technical += "Pneumatici rilevati: ${tireMatches.joinToString(" · ")}"

    val annotatedRaw = buildString {
        if (technical.isNotEmpty()) {
            appendLine("SCHEDA TECNICA RICONOSCIUTA")
            technical.forEach { appendLine(it) }
            appendLine()
            appendLine("TESTO COMPLETO OCR")
        }
        append(normalized.trim())
    }

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
        rawText = annotatedRaw
    )
}
