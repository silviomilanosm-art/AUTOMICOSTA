from pathlib import Path

p = Path('app/src/main/java/com/automicosta/app/ui/AutoMiCostaApp.kt')
s = p.read_text()
old = 'if (m.note.isNotBlank()) Text(m.note, style = MaterialTheme.typography.bodySmall); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { onEdit(m) }) { Text("Modifica") } }'
new = 'if (m.note.isNotBlank()) Text(m.note, style = MaterialTheme.typography.bodySmall); Spacer(Modifier.height(8.dp)); FilledTonalButton(onClick = { onEdit(m) }, modifier = Modifier.fillMaxWidth()) { Text("MODIFICA VOCE") }'
if old not in s:
    raise SystemExit('Target edit button pattern not found')
s = s.replace(old, new, 1)
p.write_text(s)
