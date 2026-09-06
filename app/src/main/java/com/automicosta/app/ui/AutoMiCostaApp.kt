@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.automicosta.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.automicosta.app.data.ExpenseEntity
import com.automicosta.app.data.VehicleEntity
import com.automicosta.app.data.defaultCategories
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private enum class Tab { HOME, SPESE, SCADENZE }

@Composable
fun AutoMiCostaApp(vm: AutoMiCostaViewModel) {
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val selected = vehicles.firstOrNull { it.id == selectedId } ?: vehicles.firstOrNull()

    LaunchedEffect(vehicles, selectedId) {
        if (selectedId == null && vehicles.isNotEmpty()) selectedId = vehicles.first().id
        vm.selectVehicle(selectedId ?: vehicles.firstOrNull()?.id)
    }

    if (vehicles.isEmpty()) {
        VehicleSetupScreen(onSave = vm::addVehicle)
        return
    }

    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(Tab.HOME) }
    var showExpense by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("AUTOMICOSTA", fontWeight = FontWeight.Bold); Text(selected?.nickname.orEmpty(), style = MaterialTheme.typography.labelMedium) } }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == Tab.HOME, onClick = { tab = Tab.HOME }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = tab == Tab.SPESE, onClick = { tab = Tab.SPESE }, icon = { Icon(Icons.Default.ReceiptLong, null) }, label = { Text("Spese") })
                NavigationBarItem(selected = tab == Tab.SCADENZE, onClick = { tab = Tab.SCADENZE }, icon = { Icon(Icons.Default.Build, null) }, label = { Text("Scadenze") })
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { if (tab == Tab.SCADENZE) showReminder = true else showExpense = true }) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi")
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.HOME -> Dashboard(selected!!, expenses, reminders.size)
                Tab.SPESE -> ExpenseList(expenses)
                Tab.SCADENZE -> ReminderList(reminders.map { it.title to it.dueKm })
            }
        }
    }

    if (showExpense) {
        AddExpenseDialog(
            onDismiss = { showExpense = false },
            onSave = { category, amount, km, qty, unitPrice, note ->
                vm.addExpense(selected!!.id, category, amount, km, qty, unitPrice, note)
                showExpense = false
            }
        )
    }

    if (showReminder) {
        AddReminderDialog(
            onDismiss = { showReminder = false },
            onSave = { title, km -> vm.addReminder(selected!!.id, title, km); showReminder = false }
        )
    }
}

@Composable
private fun VehicleSetupScreen(onSave: (String, String, String, String, Int) -> Unit) {
    var nickname by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var fuel by remember { mutableStateOf("Benzina") }
    var km by remember { mutableStateOf("") }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(28.dp))
            Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Benvenuto in AUTOMICOSTA", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Aggiungi la tua auto. Potrai iniziare subito a capire quanto ti costa davvero.")
            OutlinedTextField(nickname, { nickname = it }, label = { Text("Nome auto (es. Golf)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(brand, { brand = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(model, { model = it }, label = { Text("Modello") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(fuel, { fuel = it }, label = { Text("Alimentazione") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(km, { km = it.filter(Char::isDigit) }, label = { Text("Km attuali") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { onSave(nickname.ifBlank { "La mia auto" }, brand, model, fuel, km.toIntOrNull() ?: 0) },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Inizia") }
        }
    }
}

@Composable
private fun Dashboard(vehicle: VehicleEntity, expenses: List<ExpenseEntity>, reminderCount: Int) {
    val total = expenses.sumOf { it.amount }
    val maxKm = expenses.mapNotNull { it.odometerKm }.maxOrNull() ?: vehicle.currentKm
    val minKm = expenses.mapNotNull { it.odometerKm }.minOrNull() ?: vehicle.currentKm
    val driven = (maxKm - minKm).coerceAtLeast(0)
    val costPerKm = if (driven > 0) total / driven else 0.0
    val fuel = expenses.filter { it.category == "Carburante" || it.category == "Ricarica" }.sumOf { it.amount }
    val maintenance = expenses.filter { it.category == "Manutenzione" }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Quanto ti costa davvero?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${vehicle.brand} ${vehicle.model}".trim().ifBlank { vehicle.nickname }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { MetricCard("Spese registrate", euro(total), "${expenses.size} movimenti") }
        item { MetricCard("Costo per km", if (driven > 0) euro(costPerKm) else "—", if (driven > 0) "$driven km analizzati" else "Aggiungi km alle spese per calcolarlo") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallMetric("Carburante", euro(fuel), Modifier.weight(1f))
                SmallMetric("Manutenzione", euro(maintenance), Modifier.weight(1f))
            }
        }
        item { MetricCard("Prossime scadenze", reminderCount.toString(), if (reminderCount == 0) "Nessuna scadenza inserita" else "Da tenere sotto controllo") }
    }
}

@Composable
private fun MetricCard(title: String, value: String, subtitle: String) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SmallMetric(title: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) { Column(Modifier.padding(16.dp)) { Text(title, style = MaterialTheme.typography.labelMedium); Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) } }
}

@Composable
private fun ExpenseList(expenses: List<ExpenseEntity>) {
    if (expenses.isEmpty()) { EmptyState("Nessuna spesa", "Tocca + per registrare il primo costo della tua auto."); return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(expenses, key = { it.id }) { e ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.category, fontWeight = FontWeight.SemiBold)
                        Text(listOfNotNull(e.odometerKm?.let { "$it km" }, e.note.takeIf { it.isNotBlank() }).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(euro(e.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReminderList(items: List<Pair<String, Int?>>) {
    if (items.isEmpty()) { EmptyState("Nessuna scadenza", "Aggiungi tagliando, revisione, assicurazione o cambio gomme."); return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { (title, km) ->
            ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(km?.let { "Entro $it km" } ?: "Scadenza registrata") } }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(subtitle) }
    }
}

@Composable
private fun AddExpenseDialog(onDismiss: () -> Unit, onSave: (String, Double, Int?, Double?, Double?, String) -> Unit) {
    var category by remember { mutableStateOf("Carburante") }
    var amount by remember { mutableStateOf("") }
    var km by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unitPrice by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi spesa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(category, {}, readOnly = true, label = { Text("Categoria") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        defaultCategories.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { category = c; expanded = false }) }
                    }
                }
                OutlinedTextField(amount, { amount = normalizeNumber(it) }, label = { Text("Importo €") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(km, { km = it.filter(Char::isDigit) }, label = { Text("Contachilometri (opzionale)") }, modifier = Modifier.fillMaxWidth())
                if (category == "Carburante" || category == "Ricarica") {
                    OutlinedTextField(quantity, { quantity = normalizeNumber(it) }, label = { Text(if (category == "Ricarica") "kWh" else "Litri") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(unitPrice, { unitPrice = normalizeNumber(it) }, label = { Text(if (category == "Ricarica") "€/kWh" else "€/litro") }, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(note, { note = it }, label = { Text("Nota") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = amount.toDoubleOrNull() != null, onClick = { onSave(category, amount.toDoubleOrNull() ?: 0.0, km.toIntOrNull(), quantity.toDoubleOrNull(), unitPrice.toDoubleOrNull(), note) }) { Text("Salva") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
private fun AddReminderDialog(onDismiss: () -> Unit, onSave: (String, Int?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var km by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova scadenza") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedTextField(title, { title = it }, label = { Text("Es. Tagliando") }); OutlinedTextField(km, { km = it.filter(Char::isDigit) }, label = { Text("Km scadenza (opzionale)") }) } },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onSave(title, km.toIntOrNull()) }) { Text("Salva") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

private fun normalizeNumber(value: String): String = value.replace(',', '.').filter { it.isDigit() || it == '.' }
private fun euro(value: Double): String = NumberFormat.getCurrencyInstance(Locale.ITALY).apply { currency = Currency.getInstance("EUR") }.format(value)
