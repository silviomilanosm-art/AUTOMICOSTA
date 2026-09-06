package com.automicosta.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.FilledTonalButton
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

@Composable
fun LibrettoScanButton(onData: (LibrettoData) -> Unit) {
    val context = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Scatta una foto nitida del libretto oppure scegli una foto già presente.") }
    var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }

    fun process(uri: android.net.Uri) {
        busy = true
        message = "Sto leggendo il libretto…"
        scanLibretto(context, uri) { result ->
            busy = false
            result.onSuccess { data ->
                onData(data)
                val count = listOf(data.plate, data.vin, data.brand, data.model, data.firstRegistrationDate, data.fuelType).count { it.isNotBlank() }
                message = if (count > 0) "Dati trovati e precompilati. Controllali prima di salvare." else "Testo letto, ma non ho riconosciuto automaticamente i campi principali. Il testo OCR è stato aggiunto alle note."
            }.onFailure {
                message = "Non sono riuscito a leggere la foto. Prova con più luce e senza riflessi."
            }
        }
    }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingUri
        if (ok && uri != null) process(uri) else message = "Foto annullata."
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) process(uri)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text("📄 Compila dal libretto", style = MaterialTheme.typography.titleMedium)
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
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
