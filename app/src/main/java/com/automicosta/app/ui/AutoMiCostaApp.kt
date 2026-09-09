@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.automicosta.app.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.automicosta.app.data.ExpenseEntity
import com.automicosta.app.data.MaintenanceEntity
import com.automicosta.app.data.VehicleEntity
import com.automicosta.app.data.defaultCategories
import com.automicosta.app.data.maintenanceComponents
import java.security.MessageDigest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

private enum class Tab { HOME, VEICOLI, SPESE, MANUTENZIONE, MANUALE, SCADENZE }
private const val COPYRIGHT = "© 2026 Pasquale Mancuso · Tutti i diritti riservati"

@Composable
fun AutoMiCostaApp(vm: AutoMiCostaViewModel) {
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val selected = vehicles.firstOrNull { it.id == selectedId } ?: vehicles.firstOrNull()

    LaunchedEffect(vehicles, selectedId) {
        val validId = selectedId?.takeIf { id -> vehicles.any { it.id == id } } ?: vehicles.firstOrNull()?.id
        if (selectedId != validId) selectedId = validId
        vm.selectVehicle(validId)
    }

    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val maintenance by vm.maintenance.collectAsStateWithLifecycle()
    val vinState by vm.vinLookupState.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(Tab.HOME) }
    var showExpense by remember { mutableStateOf(false) }
    var expenseEditor by remember { mutableStateOf<ExpenseEntity?>(null) }
    var showMaintenance by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }
    var vehicleEditor by remember { mutableStateOf<VehicleEntity?>(null) }
    var showNewVehicle by remember { mutableStateOf(false) }
    var vehicleToDelete by remember { mutableStateOf<VehicleEntity?>(null) }
    var showKiaHistoryImport by remember { mutableStateOf(false) }
    var kiaImportResult by remember { mutableStateOf<Int?>(null) }

    if (vehicles.isEmpty() && !showNewVehicle) showNewVehicle = true

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("AUTOMICOSTA", fontWeight = FontWeight.Black)
                    Text(selected?.let { "${it.nickname} · ${it.plate}".trimEnd(' ', '·') } ?: "Il costo vero della tua auto", style = MaterialTheme.typography.labelMedium)
                }
            })
        },
        bottomBar = {
            Column {
                NavigationBar {
                    NavigationBarItem(tab == Tab.HOME, { tab = Tab.HOME }, { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                    NavigationBarItem(tab == Tab.VEICOLI, { tab = Tab.VEICOLI }, { Icon(Icons.Default.DirectionsCar, null) }, label = { Text("Veicoli") })
                    NavigationBarItem(tab == Tab.SPESE, { tab = Tab.SPESE }, { Icon(Icons.Default.ReceiptLong, null) }, label = { Text("Spese") })
                    NavigationBarItem(tab == Tab.MANUTENZIONE, { tab = Tab.MANUTENZIONE }, { Icon(Icons.Default.Build, null) }, label = { Text("Manut.") })
                    NavigationBarItem(tab == Tab.MANUALE, { tab = Tab.MANUALE }, { Icon(Icons.Default.Search, null) }, label = { Text("Manuale") })
                }
                Text(COPYRIGHT, Modifier.fillMaxWidth().padding(vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        },
        floatingActionButton = {
            if (selected != null || tab == Tab.VEICOLI) {
                FloatingActionButton(onClick = {
                    when (tab) {
                        Tab.VEICOLI -> if (vehicles.size < 5) { vm.resetVinLookup(); showNewVehicle = true }
                        Tab.MANUTENZIONE -> showMaintenance = true
                        Tab.MANUALE -> Unit
                        Tab.SCADENZE -> showReminder = true
                        else -> if (selected != null) { expenseEditor = null; showExpense = true }
                    }
                }) { Icon(Icons.Default.Add, "Aggiungi") }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                vehicles.isEmpty() -> EmptyState("Aggiungi il primo veicolo", "Puoi gestire fino a 5 veicoli in AUTOMICOSTA.")
                tab == Tab.HOME -> Dashboard(selected!!, expenses, maintenance, reminders.size, { tab = Tab.MANUTENZIONE }, { tab = Tab.SCADENZE })
                tab == Tab.VEICOLI -> VehicleList(vehicles, selected?.id, { selectedId = it.id; vm.selectVehicle(it.id) }, { vm.resetVinLookup(); vehicleEditor = it }, { vehicleToDelete = it }) { if (vehicles.size < 5) { vm.resetVinLookup(); showNewVehicle = true } }
                tab == Tab.SPESE -> ExpenseList(expenses) { expenseEditor = it }
                tab == Tab.MANUTENZIONE -> MaintenanceList(maintenance) { showKiaHistoryImport = true }
                tab == Tab.MANUALE -> WorkshopManualScreen()
                tab == Tab.SCADENZE -> ReminderList(reminders.map { it.title to Pair(it.dueKm, it.dueEpochDay) })
            }
        }
    }

    if (showNewVehicle || vehicleEditor != null) {
        VehicleEditorDialog(
            existing = vehicleEditor,
            vinState = vinState,
            onLookupVin = vm::lookupVin,
            onDismiss = { if (vehicles.isNotEmpty()) { showNewVehicle = false; vehicleEditor = null; vm.resetVinLookup() } },
            onSave = { vehicle -> vm.saveVehicle(vehicle) { ok -> if (ok) { showNewVehicle = false; vehicleEditor = null; vm.resetVinLookup() } } }
        )
    }

    vehicleToDelete?.let { vehicle ->
        AlertDialog(
            onDismissRequest = { vehicleToDelete = null },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Eliminare ${vehicle.nickname}?") },
            text = { Text("Verranno eliminati definitivamente questo veicolo e tutti i suoi dati collegati: spese, manutenzioni e scadenze. Gli altri veicoli non saranno modificati.") },
            confirmButton = { Button(onClick = { if (selectedId == vehicle.id) selectedId = null; vm.deleteVehicle(vehicle.id); vehicleToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Elimina definitivamente") } },
            dismissButton = { TextButton(onClick = { vehicleToDelete = null }) { Text("Annulla") } }
        )
    }

    if (showExpense && selected != null) {
        ExpenseEditorDialog(existing = null, onDismiss = { showExpense = false }) { category, amount, km, qty, unitPrice, note, date ->
            vm.addExpense(selected.id, category, amount, km, qty, unitPrice, note, parseDate(date)); showExpense = false
        }
    }

    expenseEditor?.let { expense ->
        ExpenseEditorDialog(existing = expense, onDismiss = { expenseEditor = null }) { category, amount, km, qty, unitPrice, note, date ->
            vm.updateExpense(
                expense.copy(
                    dateEpochDay = parseDate(date),
                    category = category,
                    amount = amount,
                    odometerKm = km,
                    litersOrKwh = qty,
                    unitPrice = unitPrice,
                    note = note
                )
            )
            expenseEditor = null
        }
    }

    if (showKiaHistoryImport && selected != null) {
        AlertDialog(
            onDismissRequest = { showKiaHistoryImport = false },
            title = { Text("Importare storico Kia Venga EcoGPL?") },
            text = { Text("Verranno aggiunte al veicolo ${selected.nickname} le manutenzioni documentate dal 2015 al 2026. Le voci già presenti con stessa data, intervento, km e officina saranno saltate.") },
            confirmButton = {
                Button(onClick = {
                    vm.importKiaVengaEcoGplHistory(selected.id) { count -> kiaImportResult = count }
                    showKiaHistoryImport = false
                }) { Text("Importa storico") }
            },
            dismissButton = { TextButton(onClick = { showKiaHistoryImport = false }) { Text("Annulla") } }
        )
    }

    kiaImportResult?.let { count ->
        AlertDialog(
            onDismissRequest = { kiaImportResult = null },
            title = { Text("Importazione completata") },
            text = { Text(if (count > 0) "$count interventi aggiunti allo storico." else "Nessun nuovo intervento da aggiungere: lo storico risulta già importato.") },
            confirmButton = { TextButton(onClick = { kiaImportResult = null }) { Text("OK") } }
        )
    }

    if (showMaintenance && selected != null) AddMaintenanceDialog(selected.id, { showMaintenance = false }) { vm.addMaintenance(it); showMaintenance = false }
    if (showReminder && selected != null) AddReminderDialog({ showReminder = false }) { title, km, date -> vm.addReminder(selected.id, title, km, date?.let(::parseDate)); showReminder = false }
}

@Composable
private fun PinGate(context: Context, onUnlocked: () -> Unit) {
    val prefs = remember { context.getSharedPreferences("automicosta_security", Context.MODE_PRIVATE) }
    val savedHash = remember { prefs.getString("pin_hash", null) }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val creating = savedHash == null
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Default.Lock, null, Modifier.padding(24.dp).size(54.dp), tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(22.dp))
            Text("AUTOMICOSTA", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Text(if (creating) "Crea il PIN di accesso" else "Bentornato. Inserisci il PIN.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(8); error = "" }, label = { Text("PIN (4–8 cifre)") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            if (creating) { Spacer(Modifier.height(10.dp)); OutlinedTextField(confirm, { confirm = it.filter(Char::isDigit).take(8); error = "" }, label = { Text("Conferma PIN") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()) }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                when {
                    pin.length !in 4..8 -> error = "Il PIN deve avere da 4 a 8 cifre."
                    creating && pin != confirm -> error = "I PIN non coincidono."
                    creating -> { prefs.edit().putString("pin_hash", hashPin(pin)).apply(); onUnlocked() }
                    hashPin(pin) == savedHash -> onUnlocked()
                    else -> error = "PIN non corretto."
                }
            }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(if (creating) "Proteggi AUTOMICOSTA" else "Entra") }
            Spacer(Modifier.height(28.dp)); Text(COPYRIGHT, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Dashboard(vehicle: VehicleEntity, expenses: List<ExpenseEntity>, maintenance: List<MaintenanceEntity>, reminderCount: Int, onMaintenance: () -> Unit, onReminders: () -> Unit) {
    val totalExpenses = expenses.sumOf { it.amount }
    val maintenanceTotal = maintenance.sumOf { it.cost }
    val total = totalExpenses + maintenanceTotal
    val kms = (expenses.mapNotNull { it.odometerKm } + maintenance.mapNotNull { it.odometerKm } + listOf(vehicle.currentKm)).filter { it > 0 }
    val driven = if (kms.size > 1) ((kms.maxOrNull() ?: 0) - (kms.minOrNull() ?: 0)).coerceAtLeast(0) else 0
    val fuel = expenses.filter { it.category == "Carburante" || it.category == "Ricarica" }.sumOf { it.amount }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(20.dp)) { Text("🚗 ${vehicle.nickname}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text(listOf(vehicle.brand, vehicle.model, vehicle.registrationYear?.toString()).filter { !it.isNullOrBlank() }.joinToString(" · ")); if (vehicle.plate.isNotBlank()) AssistChip({}, { Text(vehicle.plate.uppercase()) }); Spacer(Modifier.height(8.dp)); Text("Quanto ti costa davvero?"); Text(euro(total), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black) } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { SmallMetric("Costo/km", if (driven > 0) euro(total / driven) else "—", Modifier.weight(1f)); SmallMetric("Carburante", euro(fuel), Modifier.weight(1f)) } }
        item { MetricCard("🔧 Manutenzione", euro(maintenanceTotal), "${maintenance.size} interventi registrati", onMaintenance) }
        item { MetricCard("⏰ Scadenze", reminderCount.toString(), if (reminderCount == 0) "Tutto tranquillo" else "Controlla cosa si avvicina", onReminders) }
        item { MetricCard("📍 Chilometraggio", "${vehicle.currentKm} km", if (driven > 0) "$driven km analizzati" else "Aggiorna i km nelle registrazioni") }
    }
}

@Composable
private fun VehicleList(vehicles: List<VehicleEntity>, selectedId: Long?, onSelect: (VehicleEntity) -> Unit, onEdit: (VehicleEntity) -> Unit, onDelete: (VehicleEntity) -> Unit, onAdd: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Il tuo garage", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text("${vehicles.size}/5 veicoli registrati") }; FilledTonalButton(onClick = onAdd, enabled = vehicles.size < 5) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Veicolo") } } }
        items(vehicles, key = { it.id }) { v ->
            ElevatedCard(Modifier.fillMaxWidth().clickable { onSelect(v) }, colors = if (v.id == selectedId) CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else CardDefaults.elevatedCardColors()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(v.nickname, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(listOf(v.brand, v.model, v.registrationYear?.toString()).filter { !it.isNullOrBlank() }.joinToString(" · ")) } }
                    HorizontalDivider(Modifier.padding(vertical = 10.dp)); Text("Targa: ${v.plate.ifBlank { "—" }}   •   VIN: ${v.vin.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall); Text("${v.currentKm} km   •   ${v.fuelType}   •   Pneumatici: ${v.tireCount}", style = MaterialTheme.typography.bodySmall); if (v.inspectionDate.isNotBlank()) Text("Revisione: ${v.inspectionDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton({ onEdit(v) }) { Text("Modifica") }; TextButton({ onDelete(v) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(4.dp)); Text("Elimina") } }
                }
            }
        }
    }
}

@Composable
private fun VehicleEditorDialog(existing: VehicleEntity?, vinState: VinLookupState, onLookupVin: (String, Int?) -> Unit, onDismiss: () -> Unit, onSave: (VehicleEntity) -> Unit) {
    var nickname by remember(existing) { mutableStateOf(existing?.nickname.orEmpty()) }
    var brand by remember(existing) { mutableStateOf(existing?.brand.orEmpty()) }
    var model by remember(existing) { mutableStateOf(existing?.model.orEmpty()) }
    var plate by remember(existing) { mutableStateOf(existing?.plate.orEmpty()) }
    var vin by remember(existing) { mutableStateOf(existing?.vin.orEmpty()) }
    var year by remember(existing) { mutableStateOf(existing?.registrationYear?.toString().orEmpty()) }
    var registrationDate by remember(existing) { mutableStateOf(existing?.firstRegistrationDate.orEmpty()) }
    var inspectionDate by remember(existing) { mutableStateOf(existing?.inspectionDate.orEmpty()) }
    var fuel by remember(existing) { mutableStateOf(existing?.fuelType ?: "Benzina") }
    var km by remember(existing) { mutableStateOf(existing?.currentKm?.toString() ?: "") }
    var purchaseDate by remember(existing) { mutableStateOf(existing?.purchaseDate.orEmpty()) }
    var purchasePrice by remember(existing) { mutableStateOf(existing?.purchasePrice?.toString().orEmpty()) }
    var tireCount by remember(existing) { mutableStateOf(existing?.tireCount?.toString() ?: "4") }
    var frontTires by remember(existing) { mutableStateOf(existing?.frontTireSize.orEmpty()) }
    var rearTires by remember(existing) { mutableStateOf(existing?.rearTireSize.orEmpty()) }
    var displacement by remember(existing) { mutableStateOf(existing?.engineDisplacementCc?.toString().orEmpty()) }
    var powerKw by remember(existing) { mutableStateOf(existing?.powerKw?.toString().orEmpty()) }
    var bodyType by remember(existing) { mutableStateOf(existing?.bodyType.orEmpty()) }
    var color by remember(existing) { mutableStateOf(existing?.color.orEmpty()) }
    var origin by remember(existing) { mutableStateOf(existing?.countryOfOrigin.orEmpty()) }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }

    LaunchedEffect(vinState) {
        if (vinState is VinLookupState.Success) {
            val v = vinState.vehicle
            if (nickname.isBlank()) nickname = v.nickname
            if (brand.isBlank()) brand = v.brand
            if (model.isBlank()) model = v.model
            if (year.isBlank()) year = v.registrationYear?.toString().orEmpty()
            if (fuel.isBlank() || fuel == "Benzina") fuel = v.fuelType
            if (displacement.isBlank()) displacement = v.engineDisplacementCc?.toString().orEmpty()
            if (bodyType.isBlank()) bodyType = v.bodyType
            if (origin.isBlank()) origin = v.countryOfOrigin
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(0.94f), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(if (existing == null) "Nuovo veicolo" else "Scheda veicolo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text("Puoi compilare i dati anche fotografando il libretto.", style = MaterialTheme.typography.bodySmall) }; TextButton(onClick = onDismiss) { Text("Chiudi") } }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        LibrettoScanButton { data ->
                            if (data.plate.isNotBlank()) plate = data.plate
                            if (data.vin.isNotBlank()) vin = data.vin
                            if (data.brand.isNotBlank()) brand = data.brand
                            if (data.model.isNotBlank()) model = data.model
                            if (nickname.isBlank() && (data.brand.isNotBlank() || data.model.isNotBlank())) nickname = listOf(data.brand, data.model).filter { it.isNotBlank() }.joinToString(" ")
                            if (data.firstRegistrationDate.isNotBlank()) { registrationDate = data.firstRegistrationDate; if (year.isBlank()) year = data.firstRegistrationDate.takeLast(4) }
                            data.displacementCc?.let { displacement = it.toString() }
                            data.powerKw?.let { powerKw = it.toString() }
                            if (data.fuelType.isNotBlank()) fuel = data.fuelType
                            if (data.bodyType.isNotBlank()) bodyType = data.bodyType
                            if (data.rawText.isNotBlank()) {
                                val block = "Dati letti dal libretto (OCR):\n${data.rawText}"
                                if (!notes.contains("Dati letti dal libretto (OCR):")) notes = listOf(notes.takeIf { it.isNotBlank() }, block).filterNotNull().joinToString("\n\n")
                            }
                            if (data.vin.length == 17) onLookupVin(data.vin, year.toIntOrNull())
                        }
                    }
                    item { Text("Identificazione", fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); OutlinedTextField(nickname, { nickname = it }, label = { Text("Nome del veicolo *") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(plate, { plate = it.uppercase() }, label = { Text("Targa") }, modifier = Modifier.fillMaxWidth()) }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(vin, { vin = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(17) }, label = { Text("Numero di telaio / VIN") }, modifier = Modifier.weight(1f)); FilledTonalIconButton(onClick = { onLookupVin(vin, year.toIntOrNull()) }, enabled = vin.length >= 11 && vinState !is VinLookupState.Loading) { Icon(Icons.Default.Search, "Cerca VIN") } }; when (vinState) { is VinLookupState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth()); is VinLookupState.Error -> Text(vinState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall); is VinLookupState.Success -> Text("Dati VIN trovati: controllali prima di salvare.", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall); else -> Text("Ricerca VIN online opzionale.", style = MaterialTheme.typography.bodySmall) } }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(brand, { brand = it }, label = { Text("Marca") }, modifier = Modifier.weight(1f)); OutlinedTextField(model, { model = it }, label = { Text("Modello") }, modifier = Modifier.weight(1f)) } }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, label = { Text("Anno") }, modifier = Modifier.weight(1f)); OutlinedTextField(registrationDate, { registrationDate = it }, label = { Text("1ª immatr. gg/mm/aaaa") }, modifier = Modifier.weight(1f)) } }
                    item { Text("Revisione", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                    item { OutlinedTextField(inspectionDate, { inspectionDate = it }, label = { Text("Data revisione gg/mm/aaaa") }, supportingText = { Text("Inserisci la data dell’ultima revisione effettuata") }, modifier = Modifier.fillMaxWidth()) }
                    item { Text("Motore e utilizzo", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(fuel, { fuel = it }, label = { Text("Alimentazione") }, modifier = Modifier.weight(1f)); OutlinedTextField(km, { km = it.filter(Char::isDigit) }, label = { Text("Km attuali") }, modifier = Modifier.weight(1f)) } }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(displacement, { displacement = it.filter(Char::isDigit) }, label = { Text("Cilindrata cc") }, modifier = Modifier.weight(1f)); OutlinedTextField(powerKw, { powerKw = normalizeNumber(it) }, label = { Text("Potenza kW") }, modifier = Modifier.weight(1f)) } }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(bodyType, { bodyType = it }, label = { Text("Carrozzeria") }, modifier = Modifier.weight(1f)); OutlinedTextField(color, { color = it }, label = { Text("Colore") }, modifier = Modifier.weight(1f)) } }
                    item { OutlinedTextField(origin, { origin = it }, label = { Text("Paese di produzione/origine") }, modifier = Modifier.fillMaxWidth()) }
                    item { Text("Pneumatici", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                    item { OutlinedTextField(tireCount, { tireCount = it.filter(Char::isDigit).take(2) }, label = { Text("Numero pneumatici") }, modifier = Modifier.fillMaxWidth()) }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(frontTires, { frontTires = it }, label = { Text("Misura anteriori") }, modifier = Modifier.weight(1f)); OutlinedTextField(rearTires, { rearTires = it }, label = { Text("Misura posteriori") }, modifier = Modifier.weight(1f)) } }
                    item { Text("Acquisto", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(purchaseDate, { purchaseDate = it }, label = { Text("Data acquisto") }, modifier = Modifier.weight(1f)); OutlinedTextField(purchasePrice, { purchasePrice = normalizeNumber(it) }, label = { Text("Prezzo €") }, modifier = Modifier.weight(1f)) } }
                    item { OutlinedTextField(notes, { notes = it }, label = { Text("Note veicolo") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }
                }
                Button(onClick = { onSave(VehicleEntity(id = existing?.id ?: 0, nickname = nickname.ifBlank { listOf(brand, model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Veicolo" } }, brand = brand, model = model, plate = plate, vin = vin, registrationYear = year.toIntOrNull(), firstRegistrationDate = registrationDate, inspectionDate = inspectionDate, fuelType = fuel, currentKm = km.toIntOrNull() ?: 0, purchaseDate = purchaseDate, purchasePrice = purchasePrice.toDoubleOrNull(), tireCount = tireCount.toIntOrNull() ?: 4, frontTireSize = frontTires, rearTireSize = rearTires, engineDisplacementCc = displacement.toIntOrNull(), powerKw = powerKw.toDoubleOrNull(), bodyType = bodyType, color = color, countryOfOrigin = origin, notes = notes)) }, enabled = nickname.isNotBlank() || brand.isNotBlank() || model.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(20.dp).height(52.dp)) { Text("Salva veicolo") }
            }
        }
    }
}

@Composable
private fun MaintenanceList(items: List<MaintenanceEntity>, onImportKiaHistory: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Storico manutenzione", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                FilledTonalButton(onClick = onImportKiaHistory, modifier = Modifier.fillMaxWidth()) {
                    Text("Importa storico Kia Venga EcoGPL 2015–2026")
                }
                if (items.isEmpty()) Text("Nessuna manutenzione registrata. Puoi importare lo storico Kia oppure usare + per aggiungere un intervento.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        items(items, key = { it.id }) { m -> ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Row { Text("🔧 ${m.component}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text(euro(m.cost), fontWeight = FontWeight.Bold) }; Text("${m.workType} · ${formatDate(m.dateEpochDay)}${m.odometerKm?.let { " · $it km" }.orEmpty()}", style = MaterialTheme.typography.bodySmall); if (m.workshop.isNotBlank()) Text("Officina: ${m.workshop}", style = MaterialTheme.typography.bodySmall); if (m.partBrand.isNotBlank() || m.partCode.isNotBlank()) Text("Ricambio: ${listOf(m.partBrand, m.partCode).filter { it.isNotBlank() }.joinToString(" · ")}", style = MaterialTheme.typography.bodySmall); if (m.nextDueKm != null || m.nextDueEpochDay != null) Text("Prossimo: ${m.nextDueKm?.let { "$it km" }.orEmpty()} ${m.nextDueEpochDay?.let { formatDate(it) }.orEmpty()}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall); if (m.note.isNotBlank()) Text(m.note, style = MaterialTheme.typography.bodySmall) } } }
    }
}

@Composable
private fun AddMaintenanceDialog(vehicleId: Long, onDismiss: () -> Unit, onSave: (MaintenanceEntity) -> Unit) {
    var component by remember { mutableStateOf("Tagliando completo") }; var workType by remember { mutableStateOf("Sostituzione") }; var date by remember { mutableStateOf(today()) }; var cost by remember { mutableStateOf("") }; var km by remember { mutableStateOf("") }; var workshop by remember { mutableStateOf("") }; var partBrand by remember { mutableStateOf("") }; var partCode by remember { mutableStateOf("") }; var nextDate by remember { mutableStateOf("") }; var nextKm by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }; var expanded by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) { Surface(Modifier.fillMaxWidth().fillMaxHeight(0.9f), shape = RoundedCornerShape(24.dp)) { Column { Text("Registra manutenzione", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, modifier = Modifier.padding(20.dp)); LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ExposedDropdownMenuBox(expanded, { expanded = !expanded }) { OutlinedTextField(component, {}, readOnly = true, label = { Text("Componente/intervento") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()); ExposedDropdownMenu(expanded, { expanded = false }) { maintenanceComponents.forEach { c -> DropdownMenuItem({ Text(c) }, { component = c; expanded = false }) } } } }
        item { OutlinedTextField(workType, { workType = it }, label = { Text("Tipo lavoro / esito revisione") }, modifier = Modifier.fillMaxWidth()) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(date, { date = it }, label = { Text("Data gg/mm/aaaa") }, modifier = Modifier.weight(1f)); OutlinedTextField(km, { km = it.filter(Char::isDigit) }, label = { Text("Km") }, modifier = Modifier.weight(1f)) } }
        item { OutlinedTextField(cost, { cost = normalizeNumber(it) }, label = { Text("Costo €") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(workshop, { workshop = it }, label = { Text("Officina / centro revisione") }, modifier = Modifier.fillMaxWidth()) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(partBrand, { partBrand = it }, label = { Text("Marca ricambio") }, modifier = Modifier.weight(1f)); OutlinedTextField(partCode, { partCode = it }, label = { Text("Codice ricambio") }, modifier = Modifier.weight(1f)) } }
        item { Text("Prossimo controllo/sostituzione/revisione", fontWeight = FontWeight.Bold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(nextDate, { nextDate = it }, label = { Text("Data (opz.)") }, modifier = Modifier.weight(1f)); OutlinedTextField(nextKm, { nextKm = it.filter(Char::isDigit) }, label = { Text("Km (opz.)") }, modifier = Modifier.weight(1f)) } }
        item { OutlinedTextField(note, { note = it }, label = { Text("Note") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }
    }; Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.End) { TextButton(onClick = onDismiss) { Text("Annulla") }; Button(onClick = { onSave(MaintenanceEntity(vehicleId = vehicleId, dateEpochDay = parseDate(date), component = component, workType = workType, cost = cost.toDoubleOrNull() ?: 0.0, odometerKm = km.toIntOrNull(), workshop = workshop, partBrand = partBrand, partCode = partCode, nextDueEpochDay = nextDate.takeIf { it.isNotBlank() }?.let(::parseDate), nextDueKm = nextKm.toIntOrNull(), note = note)) }) { Text("Registra") } } } } }
}

@Composable
private fun ExpenseList(expenses: List<ExpenseEntity>, onEdit: (ExpenseEntity) -> Unit) {
    if (expenses.isEmpty()) { EmptyState("Nessuna spesa", "Tocca + per registrare il primo costo."); return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Spese", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }
        items(expenses, key = { it.id }) { e ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.category, fontWeight = FontWeight.SemiBold)
                            Text("${formatDate(e.dateEpochDay)}${e.odometerKm?.let { " · $it km" }.orEmpty()}${e.note.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}", style = MaterialTheme.typography.bodySmall)
                            if ((e.category == "Carburante" || e.category == "Ricarica") && (e.litersOrKwh != null || e.unitPrice != null)) {
                                Text(listOfNotNull(e.litersOrKwh?.let { if (e.category == "Ricarica") "$it kWh" else "$it L" }, e.unitPrice?.let { if (e.category == "Ricarica") "${euro(it)}/kWh" else "${euro(it)}/L" }).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(euro(e.amount), fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onEdit(e) }) { Text("Modifica") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderList(items: List<Pair<String, Pair<Int?, Long?>>>) {
    if (items.isEmpty()) { EmptyState("Nessuna scadenza", "Aggiungi revisione, assicurazione, bollo o una scadenza tecnica."); return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { item { Text("Scadenze", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }; items(items) { (title, due) -> ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(listOfNotNull(due.first?.let { "$it km" }, due.second?.let { formatDate(it) }).joinToString(" · ").ifBlank { "Scadenza registrata" }) } } } }
}

@Composable
private fun MetricCard(title: String, value: String, subtitle: String, onClick: (() -> Unit)? = null) { ElevatedCard(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) { Column(Modifier.padding(18.dp)) { Text(title); Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text(subtitle, style = MaterialTheme.typography.bodySmall) } } }
@Composable private fun SmallMetric(title: String, value: String, modifier: Modifier = Modifier) { ElevatedCard(modifier) { Column(Modifier.padding(16.dp)) { Text(title); Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) } } }
@Composable private fun EmptyState(title: String, subtitle: String) { Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("🚘", style = MaterialTheme.typography.displayMedium); Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Spacer(Modifier.height(6.dp)); Text(subtitle) } } }

@Composable
private fun ExpenseEditorDialog(existing: ExpenseEntity?, onDismiss: () -> Unit, onSave: (String, Double, Int?, Double?, Double?, String, String) -> Unit) {
    var category by remember(existing) { mutableStateOf(existing?.category ?: "Carburante") }
    var amount by remember(existing) { mutableStateOf(existing?.amount?.toString().orEmpty()) }
    var km by remember(existing) { mutableStateOf(existing?.odometerKm?.toString().orEmpty()) }
    var quantity by remember(existing) { mutableStateOf(existing?.litersOrKwh?.toString().orEmpty()) }
    var unitPrice by remember(existing) { mutableStateOf(existing?.unitPrice?.toString().orEmpty()) }
    var note by remember(existing) { mutableStateOf(existing?.note.orEmpty()) }
    var date by remember(existing) { mutableStateOf(existing?.let { formatDate(it.dateEpochDay) } ?: today()) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Aggiungi spesa" else "Modifica spesa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
                    OutlinedTextField(category, {}, readOnly = true, label = { Text("Categoria") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        defaultCategories.forEach { c -> DropdownMenuItem({ Text(c) }, { category = c; expanded = false }) }
                    }
                }
                OutlinedTextField(date, { date = it }, label = { Text("Data gg/mm/aaaa") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(amount, { amount = normalizeNumber(it) }, label = { Text("Importo €") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(km, { km = it.filter(Char::isDigit) }, label = { Text("Contachilometri") }, modifier = Modifier.fillMaxWidth())
                if (category == "Carburante" || category == "Ricarica") {
                    OutlinedTextField(quantity, { quantity = normalizeNumber(it) }, label = { Text(if (category == "Ricarica") "kWh" else "Litri") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(unitPrice, { unitPrice = normalizeNumber(it) }, label = { Text(if (category == "Ricarica") "€/kWh" else "€/litro") }, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(note, { note = it }, label = { Text("Nota") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = amount.toDoubleOrNull() != null, onClick = { onSave(category, amount.toDoubleOrNull() ?: 0.0, km.toIntOrNull(), quantity.toDoubleOrNull(), unitPrice.toDoubleOrNull(), note, date) }) {
                Text(if (existing == null) "Salva" else "Salva modifiche")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
private fun AddReminderDialog(onDismiss: () -> Unit, onSave: (String, Int?, String?) -> Unit) {
    var title by remember { mutableStateOf("") }; var km by remember { mutableStateOf("") }; var date by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nuova scadenza") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedTextField(title, { title = it }, label = { Text("Es. Revisione") }); OutlinedTextField(date, { date = it }, label = { Text("Data gg/mm/aaaa (opzionale)") }); OutlinedTextField(km, { km = it.filter(Char::isDigit) }, label = { Text("Km scadenza (opzionale)") }) } }, confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onSave(title, km.toIntOrNull(), date.takeIf { it.isNotBlank() }) }) { Text("Salva") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } })
}

private fun normalizeNumber(value: String): String = value.replace(',', '.').filter { it.isDigit() || it == '.' }
private fun euro(value: Double): String = NumberFormat.getCurrencyInstance(Locale.ITALY).apply { currency = Currency.getInstance("EUR") }.format(value)
private fun today(): String = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(Date())
private fun parseDate(value: String): Long = runCatching { (SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).apply { isLenient = false }.parse(value)?.time ?: System.currentTimeMillis()) / 86_400_000L }.getOrDefault(System.currentTimeMillis() / 86_400_000L)
private fun formatDate(epochDay: Long): String = when { epochDay == -1L -> "Data non indicata"; epochDay in -3000L..-1900L -> "${-epochDay} (data parziale)"; else -> SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(Date(epochDay * 86_400_000L)) }
private fun hashPin(pin: String): String = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
