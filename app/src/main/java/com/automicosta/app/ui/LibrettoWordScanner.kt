package com.automicosta.app.ui

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Xml
import java.util.zip.ZipInputStream

/** Legge localmente un file Word .docx e interpreta i dati del libretto. */
fun scanLibrettoDocx(
    context: Context,
    uri: Uri,
    onResult: (Result<LibrettoData>) -> Unit
) {
    val mainHandler = Handler(Looper.getMainLooper())
    Thread {
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Impossibile aprire il file Word")
            val text = input.use { stream ->
                ZipInputStream(stream).use { zip ->
                    var entry = zip.nextEntry
                    var documentText = ""
                    while (entry != null) {
                        if (entry.name == "word/document.xml") {
                            val parser = Xml.newPullParser()
                            parser.setInput(zip, "UTF-8")
                            val out = StringBuilder()
                            var event = parser.eventType
                            while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                                if (event == org.xmlpull.v1.XmlPullParser.START_TAG) {
                                    when (parser.name) {
                                        "t" -> out.append(parser.nextText())
                                        "tab" -> out.append(' ')
                                        "br", "cr" -> out.append('\n')
                                    }
                                } else if (event == org.xmlpull.v1.XmlPullParser.END_TAG && parser.name == "p") {
                                    out.append('\n')
                                }
                                event = parser.next()
                            }
                            documentText = out.toString()
                            break
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                    documentText
                }
            }
            if (text.isBlank()) throw IllegalArgumentException("Il documento Word non contiene testo leggibile")
            val result = parseDocxLibrettoText(text)
            mainHandler.post { onResult(Result.success(result)) }
        } catch (error: Throwable) {
            mainHandler.post { onResult(Result.failure(error)) }
        }
    }.start()
}

private fun parseDocxLibrettoText(raw: String): LibrettoData {
    val normalized = raw.replace('\u00A0', ' ')
    val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }

    fun valueAfter(code: String): String {
        val escaped = Regex.escape(code)
        val patterns = listOf(
            Regex("(?i)^\\(?$escaped\\)?[.:]?\\s+(.+)$"),
            Regex("(?i)^$escaped\\s*[-:=]\\s*(.+)$")
        )
        lines.forEachIndexed { index, line ->
            patterns.forEach { pattern ->
                pattern.find(line)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
            }
            if (line.matches(Regex("(?i)^\\(?$escaped\\)?[.:]?$"))) {
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

    val vin = valueAfter("E").uppercase().replace(" ", "")
        .takeIf { it.matches(Regex("[A-HJ-NPR-Z0-9]{17}")) }
        ?: firstMatching(Regex("\\b[A-HJ-NPR-Z0-9]{17}\\b"))

    val plateCandidate = valueAfter("A").uppercase().replace(" ", "")
    val plate = plateCandidate.takeIf { it.length in 5..8 && it.matches(Regex("[A-Z0-9]+")) }
        ?: firstMatching(Regex("\\b[A-Z]{2}[0-9]{3}[A-Z]{2}\\b"))

    val registration = valueAfter("B").replace('.', '/').replace('-', '/')
        .let { Regex("\\d{1,2}/\\d{1,2}/\\d{4}").find(it)?.value.orEmpty() }

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
        p3.contains("benz", true) -> "Benzina"
        p3.contains("diesel", true) || p3.contains("gasolio", true) -> "Diesel"
        p3.contains("elet", true) -> "Elettrica"
        p3.contains("ibrid", true) -> "Ibrida"
        p3.contains("gpl", true) -> "GPL"
        p3.contains("metano", true) || p3.contains("cng", true) -> "Metano"
        else -> p3
    }

    val j = valueAfter("J")
    val j1 = valueAfter("J.1")
    val j2 = valueAfter("J.2")
    val body = j2.ifBlank { j1.ifBlank { j } }

    val technical = mutableListOf<String>()
    addTechnical(technical, "A", "Targa", plate)
    addTechnical(technical, "B", "Prima immatricolazione", registration)
    addTechnical(technical, "D.1", "Marca", brand)
    addTechnical(technical, "D.2", "Tipo / variante / versione", typeVariantVersion)
    addTechnical(technical, "D.3", "Denominazione commerciale", commercialName)
    addTechnical(technical, "E", "Telaio VIN", vin)
    addTechnical(technical, "F.1", "Massa massima tecnicamente ammissibile", valueAfter("F.1"))
    addTechnical(technical, "F.2", "Massa massima ammissibile in servizio", valueAfter("F.2"))
    addTechnical(technical, "F.3", "Massa massima complesso", valueAfter("F.3"))
    addTechnical(technical, "J", "Categoria del veicolo", j)
    addTechnical(technical, "J.1", "Destinazione / uso", j1)
    addTechnical(technical, "J.2", "Carrozzeria", j2)
    addTechnical(technical, "K", "Numero di omologazione", valueAfter("K"))
    addTechnical(technical, "P.1", "Cilindrata cm³", p1)
    addTechnical(technical, "P.2", "Potenza netta massima kW", p2)
    addTechnical(technical, "P.3", "Alimentazione", p3)
    addTechnical(technical, "P.5", "Identificazione motore", valueAfter("P.5"))
    addTechnical(technical, "Q", "Rapporto potenza/massa", valueAfter("Q"))
    addTechnical(technical, "S.1", "Posti a sedere", valueAfter("S.1"))
    addTechnical(technical, "S.2", "Posti in piedi", valueAfter("S.2"))
    addTechnical(technical, "T", "Velocità massima km/h", valueAfter("T"))
    addTechnical(technical, "V.7", "CO₂ g/km", valueAfter("V.7"))
    addTechnical(technical, "V.9", "Classe ambientale", valueAfter("V.9"))

    val tires = Regex("(?i)\\b\\d{3}/\\d{2}\\s*[RZ]R?\\s*\\d{2}(?:\\s*\\d{2,3}[A-Z])?\\b")
        .findAll(normalized).map { it.value.replace(Regex("\\s+"), " ").uppercase() }.distinct().take(8).toList()
    if (tires.isNotEmpty()) technical += "Pneumatici rilevati: ${tires.joinToString(" · ")}"

    val annotated = buildString {
        if (technical.isNotEmpty()) {
            appendLine("SCHEDA TECNICA RICONOSCIUTA DA WORD")
            technical.forEach { appendLine(it) }
            appendLine()
            appendLine("TESTO COMPLETO DEL DOCUMENTO WORD")
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
        rawText = annotated
    )
}
