package com.automicosta.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.xmlpull.v1.XmlPullParser
import android.util.Xml
import java.io.File
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlin.math.max
import kotlin.math.min

private const val MANUAL_FILE = "parts_manual.txt"
private const val MANUAL_PREFS = "automicosta_parts_manual"
private const val KEY_NAME = "manual_name"
private const val KEY_TYPE = "manual_type"
private const val MAX_MANUAL_CHARS = 2_000_000
private const val MAX_PDF_PAGES = 80

data class PartsManualInfo(val name: String, val type: String, val chars: Int)
data class ManualSearchResult(val excerpt: String, val score: Int)

fun getPartsManualInfo(context: Context): PartsManualInfo? {
    val file = File(context.filesDir, MANUAL_FILE)
    if (!file.exists() || file.length() == 0L) return null
    val prefs = context.getSharedPreferences(MANUAL_PREFS, Context.MODE_PRIVATE)
    return PartsManualInfo(
        name = prefs.getString(KEY_NAME, "Manuale ricambi") ?: "Manuale ricambi",
        type = prefs.getString(KEY_TYPE, "documento") ?: "documento",
        chars = file.length().toInt()
    )
}

fun deletePartsManual(context: Context) {
    File(context.filesDir, MANUAL_FILE).delete()
    context.getSharedPreferences(MANUAL_PREFS, Context.MODE_PRIVATE).edit().clear().apply()
}

fun importPartsManual(context: Context, uri: Uri, onResult: (Result<PartsManualInfo>) -> Unit) {
    val name = displayName(context, uri)
    val lower = name.lowercase(Locale.ROOT)
    when {
        lower.endsWith(".docx") -> Thread {
            runCatching { extractDocx(context, uri) }
                .mapCatching { saveManual(context, name, "Word", it) }
                .let { result -> android.os.Handler(android.os.Looper.getMainLooper()).post { onResult(result) } }
        }.start()
        lower.endsWith(".txt") || lower.endsWith(".csv") -> Thread {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Impossibile leggere il file")
            }.mapCatching { saveManual(context, name, "Testo", it) }
                .let { result -> android.os.Handler(android.os.Looper.getMainLooper()).post { onResult(result) } }
        }.start()
        lower.endsWith(".pdf") -> extractPdf(context, uri, name, onResult)
        else -> onResult(Result.failure(IllegalArgumentException("Formato non supportato. Usa PDF, DOCX, TXT o CSV.")))
    }
}

fun searchPartsManual(context: Context, question: String, maxResults: Int = 6): List<ManualSearchResult> {
    val file = File(context.filesDir, MANUAL_FILE)
    if (!file.exists()) return emptyList()
    val text = file.readText()
    val terms = question.lowercase(Locale.ROOT)
        .replace(Regex("[^a-zàèéìòù0-9./_-]+"), " ")
        .split(" ")
        .map { it.trim() }
        .filter { it.length >= 3 && it !in STOP_WORDS }
        .distinct()
    if (terms.isEmpty()) return emptyList()

    val blocks = text.split(Regex("""\n{2,}|(?<=\.)\s+(?=[A-ZÀ-Ý0-9])"""))
    return blocks.mapNotNull { block ->
        val normalized = block.lowercase(Locale.ROOT)
        var score = 0
        terms.forEach { term -> if (normalized.contains(term)) score += if (term.any(Char::isDigit)) 4 else 2 }
        if (score == 0) null else ManualSearchResult(block.trim().take(1200), score)
    }.sortedByDescending { it.score }.take(maxResults)
}

private val STOP_WORDS = setOf("della", "delle", "degli", "dello", "dell", "come", "quale", "quali", "questo", "questa", "sono", "pezzo", "pezzi", "ricambio", "ricambi", "auto", "automobile", "veicolo", "serve")

private fun saveManual(context: Context, name: String, type: String, raw: String): PartsManualInfo {
    val clean = raw.replace("\u0000", "").trim()
    require(clean.isNotBlank()) { "Nel documento non è stato trovato testo leggibile." }
    val clipped = clean.take(MAX_MANUAL_CHARS)
    File(context.filesDir, MANUAL_FILE).writeText(clipped)
    context.getSharedPreferences(MANUAL_PREFS, Context.MODE_PRIVATE).edit()
        .putString(KEY_NAME, name).putString(KEY_TYPE, type).apply()
    return PartsManualInfo(name, type, clipped.length)
}

private fun displayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) return c.getString(0) ?: "manuale"
    }
    return uri.lastPathSegment ?: "manuale"
}

private fun extractDocx(context: Context, uri: Uri): String {
    val out = StringBuilder()
    context.contentResolver.openInputStream(uri)?.use { input ->
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val parser: XmlPullParser = Xml.newPullParser()
                    parser.setInput(zip, "UTF-8")
                    var event = parser.eventType
                    while (event != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.START_TAG) {
                            when (parser.name.substringAfter(':')) {
                                "t" -> out.append(parser.nextText())
                                "tab" -> out.append('\t')
                                "br", "cr", "p" -> out.append('\n')
                            }
                        }
                        event = parser.next()
                    }
                    break
                }
                entry = zip.nextEntry
            }
        }
    } ?: error("Impossibile aprire il file Word")
    return out.toString()
}

private fun extractPdf(context: Context, uri: Uri, name: String, onResult: (Result<PartsManualInfo>) -> Unit) {
    val pfd = runCatching { context.contentResolver.openFileDescriptor(uri, "r") }.getOrNull()
    if (pfd == null) { onResult(Result.failure(IllegalStateException("Impossibile aprire il PDF"))); return }
    val renderer = runCatching { PdfRenderer(pfd) }.getOrElse { pfd.close(); onResult(Result.failure(it)); return }
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val out = StringBuilder()
    val pageCount = min(renderer.pageCount, MAX_PDF_PAGES)

    fun finish(result: Result<PartsManualInfo>) {
        runCatching { recognizer.close() }
        runCatching { renderer.close() }
        runCatching { pfd.close() }
        android.os.Handler(android.os.Looper.getMainLooper()).post { onResult(result) }
    }

    fun process(index: Int) {
        if (index >= pageCount || out.length >= MAX_MANUAL_CHARS) {
            finish(runCatching { saveManual(context, name, "PDF OCR", out.toString()) })
            return
        }
        val page = renderer.openPage(index)
        val longest = max(page.width, page.height).coerceAtLeast(1)
        val scale = min(2.0, 1800.0 / longest)
        val w = max(1, (page.width * scale).toInt())
        val h = max(1, (page.height * scale).toInt())
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result ->
                out.append("\n\n--- PAGINA ${index + 1} ---\n").append(result.text)
                bitmap.recycle()
                process(index + 1)
            }
            .addOnFailureListener { e -> bitmap.recycle(); finish(Result.failure(e)) }
    }
    process(0)
}
