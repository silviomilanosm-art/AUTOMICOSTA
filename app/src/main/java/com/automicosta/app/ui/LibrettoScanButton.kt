package com.automicosta.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

private const val LIBRETTO_PREFS = "automicosta_libretto_document"
private const val LIBRETTO_PATH = "saved_path"
private const val LIBRETTO_MIME = "saved_mime"

@Composable
fun LibrettoScanButton(onData: (LibrettoData) -> Unit) {
    val context = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Scatta una foto, scegli un'immagine oppure carica PDF/Word del libretto.") }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var hasSavedDocument by remember { mutableStateOf(hasSavedLibretto(context)) }

    fun handleResult(result: Result<LibrettoData>, source: String) {
        busy = false
        result.onSuccess { data ->
            onData(data)
            val count = listOf(data.plate, data.vin, data.brand, data.model, data.firstRegistrationDate, data.fuelType).count { it.isNotBlank() }
            message = if (count > 0) {
                "Dati trovati nel $source e precompilati. Il documento è stato salvato e puoi visualizzarlo in qualsiasi momento."
            } else {
                "Il $source è stato letto e salvato. Non ho riconosciuto automaticamente tutti i campi principali."
            }
        }.onFailure {
            message = when (source) {
                "PDF" -> "Il PDF è stato salvato, ma non sono riuscito a leggerne i dati. Verifica che non sia protetto e che le pagine siano leggibili."
                "Word" -> "Il file Word è stato salvato, ma non sono riuscito a leggerne i dati. Usa un documento .docx non protetto."
                else -> "La foto è stata salvata, ma non sono riuscito a leggerne i dati. Prova con più luce e senza riflessi."
            }
        }
    }

    fun saveAndProcess(uri: Uri, mime: String, fallbackExt: String, source: String) {
        val savedUri = saveLibrettoDocument(context, uri, mime, fallbackExt)
        if (savedUri != null) hasSavedDocument = true
        val target = savedUri ?: uri
        busy = true
        message = when (source) {
            "PDF" -> "Sto leggendo il PDF del libretto…"
            "Word" -> "Sto leggendo il file Word…"
            else -> "Sto leggendo il libretto…"
        }
        when (source) {
            "PDF" -> scanLibrettoPdf(context, target) { handleResult(it, source) }
            "Word" -> scanLibrettoDocx(context, target) { handleResult(it, source) }
            else -> scanLibretto(context, target) { handleResult(it, source) }
        }
    }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingUri
        if (ok && uri != null) saveAndProcess(uri, "image/jpeg", "jpg", "libretto") else message = "Foto annullata."
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            saveAndProcess(uri, mime, extensionFor(mime, "jpg"), "libretto")
        }
    }
    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) saveAndProcess(uri, "application/pdf", "pdf", "PDF")
    }
    val pickWord = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) saveAndProcess(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx", "Word")
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text("📄 Compila dal libretto", style = MaterialTheme.typography.titleMedium)

        FilledTonalButton(
            enabled = hasSavedDocument && !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (!openSavedLibretto(context)) {
                    message = "Non trovo un'app sul telefono in grado di aprire questo documento."
                }
            }
        ) {
            Icon(Icons.Default.Description, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (hasSavedDocument) "Visualizza libretto di circolazione" else "Nessun libretto salvato")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(
                enabled = !busy,
                modifier = Modifier.weight(1f),
                onClick = {
                    val dir = File(context.cacheDir, "libretto").apply { mkdirs() }
                    val file = File(dir, "libretto_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    pendingUri = uri
                    takePhoto.launch(uri)
                }
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Scatta foto")
            }
            FilledTonalButton(enabled = !busy, modifier = Modifier.weight(1f), onClick = { pickPhoto.launch("image/*") }) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Galleria")
            }
        }
        FilledTonalButton(
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = { pickPdf.launch("application/pdf") }
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Carica PDF del libretto")
        }
        FilledTonalButton(
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = { pickWord.launch("application/vnd.openxmlformats-officedocument.wordprocessingml.document") }
        ) {
            Icon(Icons.Default.Description, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Carica file Word (.docx)")
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        BackupControls()
    }
}

private fun extensionFor(mime: String, fallback: String): String =
    MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)?.takeIf { it.isNotBlank() } ?: fallback

private fun saveLibrettoDocument(context: Context, sourceUri: Uri, mime: String, extension: String): Uri? = runCatching {
    val dir = File(context.filesDir, "libretto_document").apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() }
    val safeExt = extension.lowercase().filter { it.isLetterOrDigit() }.ifBlank { "bin" }
    val destination = File(dir, "carta_circolazione.$safeExt")
    context.contentResolver.openInputStream(sourceUri).use { input ->
        requireNotNull(input) { "Documento non leggibile" }
        destination.outputStream().use { output -> input.copyTo(output) }
    }
    context.getSharedPreferences(LIBRETTO_PREFS, Context.MODE_PRIVATE).edit()
        .putString(LIBRETTO_PATH, destination.absolutePath)
        .putString(LIBRETTO_MIME, mime)
        .apply()
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destination)
}.getOrNull()

private fun hasSavedLibretto(context: Context): Boolean {
    val path = context.getSharedPreferences(LIBRETTO_PREFS, Context.MODE_PRIVATE).getString(LIBRETTO_PATH, null)
    return !path.isNullOrBlank() && File(path).exists()
}

private fun openSavedLibretto(context: Context): Boolean = runCatching {
    val prefs = context.getSharedPreferences(LIBRETTO_PREFS, Context.MODE_PRIVATE)
    val path = prefs.getString(LIBRETTO_PATH, null) ?: return false
    val mime = prefs.getString(LIBRETTO_MIME, null) ?: "application/octet-stream"
    val file = File(path)
    if (!file.exists()) return false
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Apri libretto di circolazione"))
    true
}.getOrDefault(false)
