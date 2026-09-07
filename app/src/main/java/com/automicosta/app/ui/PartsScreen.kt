package com.automicosta.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.automicosta.app.data.VehicleEntity
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class PartsCategory(val title: String, val examples: List<String>)

private val partsCategories = listOf(
    PartsCategory("Elettrico e lampadine", listOf("Lampadina anabbagliante", "Lampadina abbagliante", "Fendinebbia", "Posizione W5W/T10", "Freccia PY21W", "Stop P21W", "Stop/posizione P21/5W", "Retromarcia W16W", "Targa C5W/W5W", "Xenon D1S/D2S/D3S/D4S", "Modulo/faro LED", "Batteria 12V", "Alternatore", "Motorino avviamento")),
    PartsCategory("Filtri e tagliando", listOf("Filtro olio", "Filtro aria motore", "Filtro abitacolo", "Filtro carburante", "Olio motore", "Candele", "Candelette")),
    PartsCategory("Freni", listOf("Pastiglie anteriori", "Pastiglie posteriori", "Dischi anteriori", "Dischi posteriori", "Pinze freno", "Liquido freni")),
    PartsCategory("Distribuzione e motore", listOf("Kit distribuzione", "Cinghia distribuzione", "Catena distribuzione", "Pompa acqua", "Cinghia servizi", "Tendicinghia", "Turbina", "EGR", "DPF/FAP")),
    PartsCategory("Sospensioni e sterzo", listOf("Ammortizzatori anteriori", "Ammortizzatori posteriori", "Bracci oscillanti", "Testine sterzo", "Biellette", "Silent block", "Cuscinetti ruota")),
    PartsCategory("Trasmissione", listOf("Frizione", "Volano bimassa", "Semiasse", "Giunto omocinetico", "Olio cambio")),
    PartsCategory("Climatizzazione", listOf("Compressore clima", "Condensatore", "Evaporatore", "Filtro abitacolo", "Gas climatizzatore")),
    PartsCategory("Carrozzeria e vetri", listOf("Parabrezza", "Specchietto", "Faro anteriore", "Fanale posteriore", "Paraurti", "Alzacristallo")),
    PartsCategory("Pneumatici e ruote", listOf("Pneumatici", "Cerchi", "Sensore TPMS", "Cuscinetto ruota"))
)

@Composable
fun PartsScreen(vehicle: VehicleEntity?) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var manualInfo by remember { mutableStateOf(getPartsManualInfo(context)) }
    var manualQuestion by remember { mutableStateOf("") }
    var manualResults by remember { mutableStateOf<List<ManualSearchResult>>(emptyList()) }
    var manualStatus by remember { mutableStateOf("") }
    var importingManual by remember { mutableStateOf(false) }
    var confirmDeleteManual by remember { mutableStateOf(false) }

    val manualPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importingManual = true
            manualStatus = "Acquisizione del manuale in corso…"
            importPartsManual(context, uri) { result ->
                importingManual = false
                result.onSuccess {
                    manualInfo = it
                    manualResults = emptyList()
                    manualStatus = "Manuale acquisito. Ora puoi fare domande sui pezzi."
                }.onFailure {
                    manualStatus = "Errore: ${it.message ?: "impossibile leggere il manuale"}"
                }
            }
        }
    }

    fun vehicleIdentity(): String = buildString {
        if (!vehicle?.vin.isNullOrBlank()) append("VIN ${vehicle?.vin} ")
        if (!vehicle?.plate.isNullOrBlank()) append("targa ${vehicle?.plate} ")
        append(listOfNotNull(vehicle?.brand, vehicle?.model, vehicle?.registrationYear?.toString())
            .filter { it.isNotBlank() }.joinToString(" "))
    }.trim()

    fun openSearch(part: String, oeOnly: Boolean = false) {
        val identity = vehicleIdentity()
        val terms = buildString {
            append(part)
            if (identity.isNotBlank()) append(" ").append(identity)
            if (oeOnly) append(" codice OE OEM compatibile") else append(" ricambio prezzo")
        }
        val encoded = URLEncoder.encode(terms, StandardCharsets.UTF_8.toString())
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded")))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Ricambi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(
                vehicle?.let { "Ricerca per ${it.nickname}${if (it.vin.isNotBlank()) " · VIN ${it.vin}" else if (it.plate.isNotBlank()) " · ${it.plate}" else ""}" }
                    ?: "Seleziona prima un veicolo in AUTOMICOSTA.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Description, null)
                        Text("Manuale pezzi dell'automobile", fontWeight = FontWeight.Bold)
                    }
                    if (manualInfo == null) {
                        Text("Carica il manuale o catalogo ricambi dell'auto. AUTOMICOSTA ne acquisisce il testo e poi cerca dentro il documento quando fai una domanda sui pezzi.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Caricato: ${manualInfo?.name}", fontWeight = FontWeight.SemiBold)
                        Text("${manualInfo?.type} · ${manualInfo?.chars ?: 0} caratteri indicizzati localmente", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { manualPicker.launch(arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain", "text/csv")) },
                        enabled = !importingManual,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (manualInfo == null) "Carica manuale ricambi" else "Sostituisci manuale") }
                    if (importingManual) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (manualInfo != null) {
                        OutlinedTextField(
                            value = manualQuestion,
                            onValueChange = { manualQuestion = it },
                            label = { Text("Chiedi un pezzo al manuale") },
                            placeholder = { Text("Es. Qual è il codice della lampadina anabbagliante?") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                manualResults = searchPartsManual(context, manualQuestion)
                                manualStatus = if (manualResults.isEmpty()) "Non ho trovato una corrispondenza chiara nel manuale." else "Ho trovato ${manualResults.size} passaggi pertinenti nel manuale."
                            },
                            enabled = manualQuestion.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Cerca nel manuale") }
                        OutlinedButton(onClick = { confirmDeleteManual = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Delete, null)
                            Text("  Elimina manuale")
                        }
                    }
                    if (manualStatus.isNotBlank()) Text(manualStatus, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (manualResults.isNotEmpty()) {
            item { Text("Risposte estratte dal manuale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(manualResults) { result ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(result.excerpt)
                        Text("Corrispondenza: ${result.score}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Cerca un pezzo online", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Es. lampadina anabbagliante, filtro olio…") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null) }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { if (query.isNotBlank()) openSearch(query) }, enabled = query.isNotBlank(), modifier = Modifier.weight(1f)) { Text("Prezzi online") }
                        Button(onClick = { if (query.isNotBlank()) openSearch(query, true) }, enabled = query.isNotBlank(), modifier = Modifier.weight(1f)) { Text("Codice OE/OEM") }
                    }
                    Text("La ricerca usa VIN quando disponibile, altrimenti targa e dati del veicolo. Verifica sempre la compatibilità prima dell'acquisto.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Catalogo professionale", fontWeight = FontWeight.Bold)
                    Text("L'app è predisposta per un futuro collegamento TecDoc/TecAlliance per una corrispondenza professionale VIN/targa → ricambio → codice OE. Il manuale caricato resta invece salvato localmente sul telefono.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        items(partsCategories) { category ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(category.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    category.examples.forEach { part ->
                        AssistChip(
                            onClick = { query = part; selectedCategory = category.title; openSearch(part) },
                            label = { Text(part) },
                            leadingIcon = { Icon(Icons.Default.OpenInBrowser, null) }
                        )
                    }
                    if (selectedCategory == category.title) {
                        Spacer(Modifier.height(2.dp))
                        Text("Tocca un altro componente per cercarlo con i dati del veicolo.", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    if (confirmDeleteManual) {
        AlertDialog(
            onDismissRequest = { confirmDeleteManual = false },
            title = { Text("Eliminare il manuale?") },
            text = { Text("Verrà eliminata solo la copia acquisita da AUTOMICOSTA. Il file originale sul telefono non verrà toccato.") },
            confirmButton = {
                Button(onClick = {
                    deletePartsManual(context)
                    manualInfo = null
                    manualResults = emptyList()
                    manualQuestion = ""
                    manualStatus = "Manuale eliminato dall'app."
                    confirmDeleteManual = false
                }) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteManual = false }) { Text("Annulla") } }
        )
    }
}
