package com.automicosta.app.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.automicosta.app.data.MaintenanceEntity
import com.automicosta.app.data.VehicleEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private sealed interface AgentAction {
    val vehicleId: Long
    data class Expense(
        override val vehicleId: Long,
        val category: String,
        val amount: Double,
        val km: Int?,
        val quantity: Double?,
        val unitPrice: Double?,
        val note: String,
        val dateEpochDay: Long
    ) : AgentAction
    data class Maintenance(
        override val vehicleId: Long,
        val component: String,
        val cost: Double,
        val km: Int?,
        val note: String,
        val dateEpochDay: Long
    ) : AgentAction
    data class Reminder(
        override val vehicleId: Long,
        val title: String,
        val dueKm: Int?,
        val dueEpochDay: Long?
    ) : AgentAction
}

@Composable
fun AiAssistantScreen(vm: AutoMiCostaViewModel, vehicles: List<VehicleEntity>) {
    var selectedId by remember(vehicles) { mutableStateOf(vehicles.firstOrNull()?.id) }
    val selected = vehicles.firstOrNull { it.id == selectedId } ?: vehicles.firstOrNull()
    var expanded by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Scrivi o detta quello che hai fatto. Prima di registrare i dati ti chiederò conferma.") }
    var pending by remember { mutableStateOf<AgentAction?>(null) }

    LaunchedEffect(selected?.id) { vm.selectVehicle(selected?.id) }
    val expenses by vm.expenses.collectAsState()
    val maintenance by vm.maintenance.collectAsState()

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) input = spoken
        }
    }

    fun analyze() {
        val vehicle = selected
        if (vehicle == null) {
            pending = null
            message = "Prima crea almeno un veicolo in AUTOMICOSTA."
            return
        }
        val result = parseAgentCommand(input, vehicle.id, expenses, maintenance)
        pending = result.first
        message = result.second
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Psychology, contentDescription = null)
            Text("Assistente AI", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        Text("Registra spese, rifornimenti, manutenzioni e promemoria usando frasi normali. Può anche rispondere a domande sui dati già salvati.")

        if (vehicles.isEmpty()) {
            Card(Modifier.fillMaxWidth()) { Text("Nessun veicolo presente. Aggiungi un veicolo prima di usare l'assistente.", Modifier.padding(14.dp)) }
        } else {
            Column {
                Text("Veicolo", fontWeight = FontWeight.Bold)
                FilledTonalButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selected?.nickname ?: "Scegli veicolo")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    vehicles.forEach { vehicle ->
                        DropdownMenuItem(
                            text = { Text(vehicle.nickname.ifBlank { listOf(vehicle.brand, vehicle.model).joinToString(" ") }) },
                            onClick = { selectedId = vehicle.id; expanded = false; pending = null }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it; pending = null },
            label = { Text("Parla con AUTOMICOSTA") },
            placeholder = { Text("Es. Ho fatto benzina: 62 euro, 48 litri, 72.300 km") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ITALIAN.toLanguageTag())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Parla con AUTOMICOSTA")
                    }
                    runCatching { speechLauncher.launch(intent) }.onFailure { message = "Riconoscimento vocale non disponibile su questo dispositivo." }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Text("  Voce")
            }
            Button(onClick = { analyze() }, enabled = input.isNotBlank() && selected != null, modifier = Modifier.weight(1f)) {
                Text("Analizza")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Assistente", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(message)
            }
        }

        pending?.let { action ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Conferma registrazione", fontWeight = FontWeight.Bold)
                    Text(actionPreview(action))
                    Button(
                        onClick = {
                            when (action) {
                                is AgentAction.Expense -> vm.addExpense(action.vehicleId, action.category, action.amount, action.km, action.quantity, action.unitPrice, action.note, action.dateEpochDay)
                                is AgentAction.Maintenance -> vm.addMaintenance(MaintenanceEntity(vehicleId = action.vehicleId, dateEpochDay = action.dateEpochDay, component = action.component, cost = action.cost, odometerKm = action.km, note = action.note))
                                is AgentAction.Reminder -> vm.addReminder(action.vehicleId, action.title, action.dueKm, action.dueEpochDay)
                            }
                            pending = null
                            input = ""
                            message = "Registrazione completata."
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Conferma e registra") }
                    TextButton(onClick = { pending = null; message = "Registrazione annullata." }, modifier = Modifier.fillMaxWidth()) { Text("Annulla") }
                }
            }
        }

        Text(
            "Esempi: “Ho pagato 138 euro di assicurazione”; “Ho cambiato le pastiglie anteriori a 71.800 km, 190 euro”; “Ricordami il tagliando a 80.000 km”; “Quanto ho speso?”",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun parseAgentCommand(
    text: String,
    vehicleId: Long,
    expenses: List<com.automicosta.app.data.ExpenseEntity>,
    maintenance: List<MaintenanceEntity>
): Pair<AgentAction?, String> {
    val clean = text.trim()
    val lower = clean.lowercase(Locale.ITALIAN)
    if (clean.isBlank()) return null to "Dimmi cosa vuoi registrare."

    if (("quanto" in lower || "totale" in lower) && ("spes" in lower || "pagat" in lower)) {
        val total = expenses.sumOf { it.amount }
        return null to "Per il veicolo selezionato risultano ${expenses.size} spese, per un totale di ${"%.2f".format(Locale.ITALIAN, total)} €."
    }
    if (("quando" in lower || "ultima" in lower) && ("cambiat" in lower || "sostituit" in lower || "manutenz" in lower)) {
        val last = maintenance.maxByOrNull { it.dateEpochDay }
        return if (last == null) null to "Non trovo manutenzioni registrate per questo veicolo."
        else null to "L'ultima manutenzione registrata è “${last.component}” del ${LocalDate.ofEpochDay(last.dateEpochDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}${last.odometerKm?.let { ", a $it km" }.orEmpty()}."
    }

    val km = extractKm(lower)
    val amount = extractAmount(lower)
    val quantity = extractQuantity(lower)
    val date = extractDate(lower) ?: LocalDate.now().toEpochDay()

    if (lower.contains("ricordami") || lower.contains("promemoria") || lower.contains("scadenza")) {
        val title = clean.replace(Regex("(?i)ricordami|promemoria|scadenza|di|a\\s+\\d[\\d. ]*\\s*km"), " ").replace(Regex("\\s+"), " ").trim().ifBlank { "Promemoria veicolo" }
        val dueKm = extractDueKm(lower) ?: km
        val dueDate = extractDate(lower)
        if (dueKm == null && dueDate == null) return null to "Ho capito che vuoi un promemoria, ma indicami una data o un chilometraggio."
        val action = AgentAction.Reminder(vehicleId, title, dueKm, dueDate)
        return action to "Ho preparato un promemoria. Controlla i dati qui sotto e conferma."
    }

    val maintenanceWords = listOf("cambiat", "sostituit", "tagliando", "revisione", "riparat", "manutenzione", "pastiglie", "olio", "filtro", "cinghia", "batteria", "pneumatic")
    if (maintenanceWords.any { lower.contains(it) }) {
        val component = detectComponent(lower)
        val action = AgentAction.Maintenance(vehicleId, component, amount ?: 0.0, km, clean, date)
        return action to "Ho riconosciuto una manutenzione. Controlla prima di registrarla."
    }

    val category = detectExpenseCategory(lower)
    if (amount != null) {
        val unitPrice = if (quantity != null && quantity > 0) amount / quantity else null
        val action = AgentAction.Expense(vehicleId, category, amount, km, quantity, unitPrice, clean, date)
        return action to "Ho riconosciuto una spesa. Controlla i dati prima di salvarli."
    }

    return null to "Ho capito la frase, ma mi manca almeno l'importo per una spesa oppure i dettagli di una manutenzione/promemoria. Prova ad aggiungere euro, km o una data."
}

private fun extractAmount(text: String): Double? {
    val explicit = Regex("(\\d{1,6}(?:[.,]\\d{1,2})?)\\s*(?:€|euro)").find(text)?.groupValues?.get(1)
    val contextual = Regex("(?:speso|pagato|costo|costato|totale|importo)\\s*(?:di|:)?\\s*(\\d{1,6}(?:[.,]\\d{1,2})?)").find(text)?.groupValues?.get(1)
    return (explicit ?: contextual)?.replace(',', '.')?.toDoubleOrNull()
}

private fun extractKm(text: String): Int? = Regex("(\\d{1,3}(?:[. ]\\d{3})+|\\d{4,7})\\s*km").find(text)?.groupValues?.get(1)?.replace(".", "")?.replace(" ", "")?.toIntOrNull()

private fun extractDueKm(text: String): Int? = Regex("(?:a|entro)\\s*(\\d{1,3}(?:[. ]\\d{3})+|\\d{4,7})\\s*km").find(text)?.groupValues?.get(1)?.replace(".", "")?.replace(" ", "")?.toIntOrNull()

private fun extractQuantity(text: String): Double? = Regex("(\\d{1,4}(?:[.,]\\d{1,3})?)\\s*(?:litri|litro|l\\b|kwh)").find(text)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()

private fun extractDate(text: String): Long? {
    if ("oggi" in text) return LocalDate.now().toEpochDay()
    if ("ieri" in text) return LocalDate.now().minusDays(1).toEpochDay()
    val match = Regex("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})").find(text) ?: return null
    return runCatching {
        val day = match.groupValues[1].toInt()
        val month = match.groupValues[2].toInt()
        var year = match.groupValues[3].toInt()
        if (year < 100) year += 2000
        LocalDate.of(year, month, day).toEpochDay()
    }.getOrNull()
}

private fun detectExpenseCategory(text: String): String = when {
    listOf("benzina", "diesel", "gasolio", "gpl", "metano", "carburante", "rifornimento").any { text.contains(it) } -> "Carburante"
    listOf("ricarica", "kwh", "colonnina").any { text.contains(it) } -> "Ricarica"
    "assicur" in text -> "Assicurazione"
    "bollo" in text -> "Bollo"
    "revisione" in text -> "Revisione"
    "pneumatic" in text || "gomme" in text -> "Pneumatici"
    "pedaggio" in text || "autostrada" in text -> "Pedaggi"
    "parcheggio" in text -> "Parcheggi"
    "lavaggio" in text -> "Lavaggio"
    "manutenz" in text || "ripar" in text -> "Manutenzione"
    else -> "Altro"
}

private fun detectComponent(text: String): String = when {
    "pastiglie" in text && ("anter" in text || "davanti" in text) -> "Pastiglie freni anteriori"
    "pastiglie" in text && ("poster" in text || "dietro" in text) -> "Pastiglie freni posteriori"
    "pastiglie" in text -> "Pastiglie freni anteriori"
    "olio" in text && "cambio" in text -> "Olio cambio"
    "olio" in text -> "Olio motore"
    "filtro olio" in text -> "Filtro olio"
    "filtro aria" in text -> "Filtro aria motore"
    "filtro abitacolo" in text || "antipolline" in text -> "Filtro abitacolo/antipolline"
    "cinghia" in text && "distrib" in text -> "Cinghia distribuzione"
    "batteria" in text -> "Batteria 12V"
    "pneumatic" in text || "gomme" in text -> "Set pneumatici"
    "revisione" in text -> "Revisione periodica"
    "tagliando" in text -> "Tagliando completo"
    else -> "Altro componente"
}

private fun actionPreview(action: AgentAction): String = when (action) {
    is AgentAction.Expense -> buildString {
        append("Spesa: ${action.category}\nImporto: ${"%.2f".format(Locale.ITALIAN, action.amount)} €")
        action.km?.let { append("\nChilometri: $it km") }
        action.quantity?.let { append("\nQuantità: ${"%.3f".format(Locale.ITALIAN, it)}") }
        append("\nData: ${LocalDate.ofEpochDay(action.dateEpochDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
    }
    is AgentAction.Maintenance -> buildString {
        append("Manutenzione: ${action.component}")
        if (action.cost > 0) append("\nCosto: ${"%.2f".format(Locale.ITALIAN, action.cost)} €")
        action.km?.let { append("\nChilometri: $it km") }
        append("\nData: ${LocalDate.ofEpochDay(action.dateEpochDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
    }
    is AgentAction.Reminder -> buildString {
        append("Promemoria: ${action.title}")
        action.dueKm?.let { append("\nScadenza: $it km") }
        action.dueEpochDay?.let { append("\nData: ${LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}") }
    }
}
