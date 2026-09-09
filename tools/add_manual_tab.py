from pathlib import Path
p=Path('app/src/main/java/com/automicosta/app/ui/AutoMiCostaApp.kt')
s=p.read_text()
s=s.replace('private enum class Tab { HOME, VEICOLI, SPESE, MANUTENZIONE, SCADENZE }','private enum class Tab { HOME, VEICOLI, SPESE, MANUTENZIONE, MANUALE, SCADENZE }')
s=s.replace('NavigationBarItem(tab == Tab.MANUTENZIONE, { tab = Tab.MANUTENZIONE }, { Icon(Icons.Default.Build, null) }, label = { Text("Manut.") })','NavigationBarItem(tab == Tab.MANUTENZIONE, { tab = Tab.MANUTENZIONE }, { Icon(Icons.Default.Build, null) }, label = { Text("Manut.") })\n                    NavigationBarItem(tab == Tab.MANUALE, { tab = Tab.MANUALE }, { Icon(Icons.Default.Search, null) }, label = { Text("Manuale") })')
s=s.replace('Tab.MANUTENZIONE -> showMaintenance = true\n                        Tab.SCADENZE', 'Tab.MANUTENZIONE -> showMaintenance = true\n                        Tab.MANUALE -> Unit\n                        Tab.SCADENZE')
s=s.replace('tab == Tab.MANUTENZIONE -> MaintenanceList(maintenance)\n                tab == Tab.SCADENZE', 'tab == Tab.MANUTENZIONE -> MaintenanceList(maintenance)\n                tab == Tab.MANUALE -> WorkshopManualScreen()\n                tab == Tab.SCADENZE')
p.write_text(s)
