from pathlib import Path

vm_path = Path('app/src/main/java/com/automicosta/app/ui/AutoMiCostaViewModel.kt')
vm = vm_path.read_text()
needle = '''    fun addMaintenance(item: MaintenanceEntity) {\n        viewModelScope.launch { dao.insertMaintenance(item) }\n    }\n'''
replacement = needle + '''\n    fun importKiaVengaEcoGplHistory(vehicleId: Long, onResult: (Int) -> Unit = {}) {\n        viewModelScope.launch {\n            val existing = dao.getAllMaintenance().filter { it.vehicleId == vehicleId }\n            var inserted = 0\n            kiaVengaEcoGplHistory(vehicleId).forEach { item ->\n                val duplicate = existing.any { current ->\n                    current.dateEpochDay == item.dateEpochDay &&\n                        current.component == item.component &&\n                        current.odometerKm == item.odometerKm &&\n                        current.workshop == item.workshop\n                }\n                if (!duplicate) {\n                    dao.insertMaintenance(item)\n                    inserted++\n                }\n            }\n            onResult(inserted)\n        }\n    }\n'''
if needle not in vm:
    raise SystemExit('VM insertion point not found')
vm = vm.replace(needle, replacement, 1)
vm_path.write_text(vm)

ui_path = Path('app/src/main/java/com/automicosta/app/ui/AutoMiCostaApp.kt')
ui = ui_path.read_text()
ui = ui.replace(
    '    var vehicleToDelete by remember { mutableStateOf<VehicleEntity?>(null) }\n',
    '    var vehicleToDelete by remember { mutableStateOf<VehicleEntity?>(null) }\n    var showKiaHistoryImport by remember { mutableStateOf(false) }\n    var kiaImportResult by remember { mutableStateOf<Int?>(null) }\n',
    1
)
ui = ui.replace(
    '                tab == Tab.MANUTENZIONE -> MaintenanceList(maintenance)\n',
    '                tab == Tab.MANUTENZIONE -> MaintenanceList(maintenance) { showKiaHistoryImport = true }\n',
    1
)
anchor = '    if (showMaintenance && selected != null) AddMaintenanceDialog(selected.id, { showMaintenance = false }) { vm.addMaintenance(it); showMaintenance = false }\n'
insert = '''    if (showKiaHistoryImport && selected != null) {\n        AlertDialog(\n            onDismissRequest = { showKiaHistoryImport = false },\n            title = { Text("Importare storico Kia Venga EcoGPL?") },\n            text = { Text("Verranno aggiunte al veicolo ${selected.nickname} le manutenzioni documentate dal 2015 al 2026. Le voci già presenti con stessa data, intervento, km e officina saranno saltate.") },\n            confirmButton = {\n                Button(onClick = {\n                    vm.importKiaVengaEcoGplHistory(selected.id) { count -> kiaImportResult = count }\n                    showKiaHistoryImport = false\n                }) { Text("Importa storico") }\n            },\n            dismissButton = { TextButton(onClick = { showKiaHistoryImport = false }) { Text("Annulla") } }\n        )\n    }\n\n    kiaImportResult?.let { count ->\n        AlertDialog(\n            onDismissRequest = { kiaImportResult = null },\n            title = { Text("Importazione completata") },\n            text = { Text(if (count > 0) "$count interventi aggiunti allo storico." else "Nessun nuovo intervento da aggiungere: lo storico risulta già importato.") },\n            confirmButton = { TextButton(onClick = { kiaImportResult = null }) { Text("OK") } }\n        )\n    }\n\n''' + anchor
if anchor not in ui:
    raise SystemExit('UI dialog anchor not found')
ui = ui.replace(anchor, insert, 1)
old_list = '''@Composable\nprivate fun MaintenanceList(items: List<MaintenanceEntity>) {\n    if (items.isEmpty()) { EmptyState("Nessuna manutenzione", "Tocca + per registrare ricambi, revisioni, lampadine e qualsiasi intervento."); return }\n    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {\n        item { Text("Storico manutenzione", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }\n'''
new_list = '''@Composable\nprivate fun MaintenanceList(items: List<MaintenanceEntity>, onImportKiaHistory: () -> Unit) {\n    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {\n        item {\n            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {\n                Text("Storico manutenzione", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)\n                FilledTonalButton(onClick = onImportKiaHistory, modifier = Modifier.fillMaxWidth()) {\n                    Text("Importa storico Kia Venga EcoGPL 2015–2026")\n                }\n                if (items.isEmpty()) Text("Nessuna manutenzione registrata. Puoi importare lo storico Kia oppure usare + per aggiungere un intervento.", style = MaterialTheme.typography.bodyMedium)\n            }\n        }\n'''
if old_list not in ui:
    raise SystemExit('MaintenanceList block not found')
ui = ui.replace(old_list, new_list, 1)
ui = ui.replace(
    'private fun formatDate(epochDay: Long): String = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(Date(epochDay * 86_400_000L))',
    'private fun formatDate(epochDay: Long): String = when { epochDay == -1L -> "Data non indicata"; epochDay in -3000L..-1900L -> "${-epochDay} (data parziale)"; else -> SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(Date(epochDay * 86_400_000L)) }',
    1
)
ui_path.write_text(ui)
print('Kia history import integrated')
