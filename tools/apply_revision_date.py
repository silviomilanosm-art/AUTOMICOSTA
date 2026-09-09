from pathlib import Path


def replace(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    if new in text:
        return
    if old not in text:
        raise SystemExit(f"Pattern not found in {path}: {old[:100]}")
    p.write_text(text.replace(old, new, 1))


models = "app/src/main/java/com/automicosta/app/data/Models.kt"
replace(
    models,
    '    val firstRegistrationDate: String = "",\n    val fuelType: String = "Benzina",',
    '    val firstRegistrationDate: String = "",\n    val inspectionDate: String = "",\n    val fuelType: String = "Benzina",',
)

db = "app/src/main/java/com/automicosta/app/data/AutoMiCostaDatabase.kt"
replace(db, "    version = 2,", "    version = 3,")
replace(
    db,
    "        fun get(context: Context): AutoMiCostaDatabase = INSTANCE ?: synchronized(this) {",
    """        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(\"ALTER TABLE vehicles ADD COLUMN inspectionDate TEXT NOT NULL DEFAULT ''\")
            }
        }

        fun get(context: Context): AutoMiCostaDatabase = INSTANCE ?: synchronized(this) {""",
)
replace(
    db,
    "            ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }",
    "            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }",
)

backup = "app/src/main/java/com/automicosta/app/data/AutoMiCostaBackupManager.kt"
replace(
    backup,
    '        put("firstRegistrationDate", v.firstRegistrationDate); put("fuelType", v.fuelType); put("currentKm", v.currentKm)',
    '        put("firstRegistrationDate", v.firstRegistrationDate); put("inspectionDate", v.inspectionDate); put("fuelType", v.fuelType); put("currentKm", v.currentKm)',
)
replace(
    backup,
    '        firstRegistrationDate = o.optString("firstRegistrationDate"), fuelType = o.optString("fuelType", "Benzina"), currentKm = o.optInt("currentKm", 0),',
    '        firstRegistrationDate = o.optString("firstRegistrationDate"), inspectionDate = o.optString("inspectionDate"), fuelType = o.optString("fuelType", "Benzina"), currentKm = o.optInt("currentKm", 0),',
)

ui = "app/src/main/java/com/automicosta/app/ui/AutoMiCostaApp.kt"
replace(
    ui,
    '    var registrationDate by remember(existing) { mutableStateOf(existing?.firstRegistrationDate.orEmpty()) }\n    var fuel by remember(existing)',
    '    var registrationDate by remember(existing) { mutableStateOf(existing?.firstRegistrationDate.orEmpty()) }\n    var inspectionDate by remember(existing) { mutableStateOf(existing?.inspectionDate.orEmpty()) }\n    var fuel by remember(existing)',
)
replace(
    ui,
    '                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, label = { Text("Anno") }, modifier = Modifier.weight(1f)); OutlinedTextField(registrationDate, { registrationDate = it }, label = { Text("1ª immatr. gg/mm/aaaa") }, modifier = Modifier.weight(1f)) } }\n                    item { Text("Motore e utilizzo", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }',
    '                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, label = { Text("Anno") }, modifier = Modifier.weight(1f)); OutlinedTextField(registrationDate, { registrationDate = it }, label = { Text("1ª immatr. gg/mm/aaaa") }, modifier = Modifier.weight(1f)) } }\n                    item { Text("Revisione", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }\n                    item { OutlinedTextField(inspectionDate, { inspectionDate = it }, label = { Text("Data revisione gg/mm/aaaa") }, supportingText = { Text("Inserisci la data dell’ultima revisione effettuata") }, modifier = Modifier.fillMaxWidth()) }\n                    item { Text("Motore e utilizzo", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }',
)
replace(
    ui,
    "firstRegistrationDate = registrationDate, fuelType = fuel,",
    "firstRegistrationDate = registrationDate, inspectionDate = inspectionDate, fuelType = fuel,",
)
replace(
    ui,
    'Text("${v.currentKm} km   •   ${v.fuelType}   •   Pneumatici: ${v.tireCount}", style = MaterialTheme.typography.bodySmall)\n                    Row(Modifier.fillMaxWidth()',
    'Text("${v.currentKm} km   •   ${v.fuelType}   •   Pneumatici: ${v.tireCount}", style = MaterialTheme.typography.bodySmall); if (v.inspectionDate.isNotBlank()) Text("Revisione: ${v.inspectionDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)\n                    Row(Modifier.fillMaxWidth()',
)
