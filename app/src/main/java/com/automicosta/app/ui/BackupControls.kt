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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.automicosta.app.data.AutoMiCostaBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupControls() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Crea un backup prima di reinstallare o cambiare telefono.") }
    var restoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            busy = true
            message = "Creazione backup in corso…"
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { AutoMiCostaBackupManager.exportBackup(context, uri) }
                }.onSuccess {
                    message = "Backup creato. Conservalo in un posto sicuro."
                }.onFailure {
                    message = "Non sono riuscito a creare il backup: ${it.message ?: "errore sconosciuto"}."
                }
                busy = false
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) restoreUri = uri
    }

    restoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text("Ripristinare il backup?") },
            text = { Text("I dati attualmente presenti in AUTOMICOSTA verranno sostituiti da quelli contenuti nel backup selezionato. Questa operazione va eseguita solo con un file di backup AUTOMICOSTA affidabile.") },
            confirmButton = {
                TextButton(onClick = {
                    restoreUri = null
                    busy = true
                    message = "Ripristino backup in corso…"
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) { AutoMiCostaBackupManager.importBackup(context, uri) }
                        }.onSuccess { summary ->
                            message = "Ripristino completato: ${summary.vehicles} veicoli, ${summary.expenses} spese, ${summary.maintenance} manutenzioni e ${summary.reminders} scadenze."
                        }.onFailure {
                            message = "Ripristino non eseguito: ${it.message ?: "file non valido"}."
                        }
                        busy = false
                    }
                }) { Text("Ripristina") }
            },
            dismissButton = { TextButton(onClick = { restoreUri = null }) { Text("Annulla") } }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text("💾 Backup e ripristino", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(
                enabled = !busy,
                modifier = Modifier.weight(1f),
                onClick = {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY).format(Date())
                    exportLauncher.launch("AUTOMICOSTA-backup-$date.json")
                }
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Crea backup")
            }
            FilledTonalButton(
                enabled = !busy,
                modifier = Modifier.weight(1f),
                onClick = { importLauncher.launch(arrayOf("application/json", "text/json", "text/plain", "*/*")) }
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Ripristina")
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
