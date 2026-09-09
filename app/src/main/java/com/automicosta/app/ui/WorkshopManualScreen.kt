package com.automicosta.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class ManualEntry(
    val keywords: List<String>,
    val title: String,
    val page: Int,
    val italianText: String
)

private val vengaManualIndex = listOf(
    ManualEntry(listOf("vin", "telaio", "identificazione"), "Identificazione veicolo e VIN", 3, "Il manuale illustra dove trovare VIN, etichetta di certificazione, numero motore e numero del cambio. Nelle pagine successive spiega anche la struttura dei codici identificativi."),
    ManualEntry(listOf("motore", "1.4", "1.6", "diesel", "specifiche"), "Specifiche motore diesel", 23, "Per i diesel 1.4 D4FC e 1.6 D4FB il manuale riporta cilindrata, alesaggio, corsa, rapporto di compressione, fasatura valvole e tolleranze dei principali componenti."),
    ManualEntry(listOf("olio", "quantità", "lubrificante"), "Olio motore diesel", 25, "Il manuale indica 5,7 L come quantità totale; per scarico e riempimento con filtro indica 5,3 L. Classificazione riportata: ACEA C3 con CPF oppure ACEA B4 senza CPF."),
    ManualEntry(listOf("puleggia", "albero motore", "coppia", "serraggio"), "Coppia puleggia albero motore", 27, "Coppia di serraggio indicata per il bullone della puleggia albero motore: 225,6–245,2 N·m (23,0–25,0 kgf·m; 166,4–180,8 lb-ft)."),
    ManualEntry(listOf("batteria", "sicurezza"), "Batteria: sicurezza e stoccaggio", 8, "Il manuale prescrive protezione degli occhi, ventilazione, assenza di fiamme o scintille e particolare cautela con l'acido solforico. Descrive inoltre lo stoccaggio della batteria e del veicolo."),
    ManualEntry(listOf("fusibile", "fusibili", "elettrico"), "Controllo fusibili", 18, "I fusibili a lama possono essere verificati dai punti di test senza rimuoverli dalla scatola, usando una lampada di prova e mettendo il circuito nelle condizioni operative indicate."),
    ManualEntry(listOf("traino", "trainare"), "Traino del veicolo", 10, "Il manuale raccomanda un servizio professionale e descrive i metodi di trasporto/traino. Per alcune configurazioni specifica procedure e limiti; consultare sempre la pagina originale prima di procedere."),
    ManualEntry(listOf("sollevamento", "ponte", "supporto"), "Punti di sollevamento e supporto", 9, "Il manuale mostra i punti di appoggio del ponte e prescrive di verificare la stabilità del veicolo prima di portarlo alla massima altezza.")
)

@Composable
fun WorkshopManualScreen() {
    var query by remember { mutableStateOf("") }
    val normalized = query.trim().lowercase()
    val results = remember(normalized) {
        if (normalized.isBlank()) vengaManualIndex
        else vengaManualIndex.filter { e -> e.keywords.any { normalized.contains(it) || it.contains(normalized) } || e.title.lowercase().contains(normalized) || e.italianText.lowercase().contains(normalized) }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Manuale officina", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("Kia Venga · Assistente tecnico in italiano", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(query, { query = it }, label = { Text("Chiedi o cerca un componente") }, placeholder = { Text("Es. coppia puleggia albero motore") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text("Prima versione dell'indice italiano del manuale. I valori tecnici restano quelli riportati nel documento originale; verifica sempre la pagina indicata prima di un intervento.", style = MaterialTheme.typography.bodySmall)
        }
        if (results.isEmpty()) item { Text("Nessun risultato nell'indice italiano disponibile. Prova un termine più generale.") }
        results.forEach { entry ->
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(entry.title, fontWeight = FontWeight.Bold)
                        Text("Manuale originale · pagina ${entry.page}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        Text(entry.italianText)
                    }
                }
            }
        }
    }
}
