package com.automicosta.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.security.MessageDigest

private enum class RootSection { APP, AI, LIBRETTO, RICAMBI }

@Composable
fun AutoMiCostaRoot(vm: AutoMiCostaViewModel) {
    val context = LocalContext.current
    var section by remember { mutableStateOf(RootSection.APP) }
    var partsUnlocked by remember { mutableStateOf(false) }
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val selected = vehicles.firstOrNull()

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (section) {
                RootSection.APP -> AutoMiCostaApp(vm)
                RootSection.AI -> AiAssistantScreen(vm, vehicles)
                RootSection.LIBRETTO -> CartaCircolazioneScreen()
                RootSection.RICAMBI -> {
                    if (partsUnlocked || hasNoPin(context)) PartsScreen(selected)
                    else PartsPinGate(context) { partsUnlocked = true }
                }
            }
        }
        Surface(tonalElevation = 6.dp) {
            NavigationBar(modifier = Modifier.fillMaxWidth()) {
                NavigationBarItem(
                    selected = section == RootSection.APP,
                    onClick = { section = RootSection.APP },
                    icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                    label = { Text("AUTO") }
                )
                NavigationBarItem(
                    selected = section == RootSection.AI,
                    onClick = { section = RootSection.AI },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                    label = { Text("AI", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = section == RootSection.LIBRETTO,
                    onClick = { section = RootSection.LIBRETTO },
                    icon = { Icon(Icons.Default.Description, contentDescription = null) },
                    label = { Text("LIBRETTO", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = section == RootSection.RICAMBI,
                    onClick = { section = RootSection.RICAMBI },
                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                    label = { Text("RICAMBI", fontWeight = FontWeight.Bold) }
                )
            }
        }
    }
}

@Composable
private fun CartaCircolazioneScreen() {
    var lastData by remember { mutableStateOf<LibrettoData?>(null) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Carta di circolazione", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Inserisci la carta di circolazione come PDF oppure foto. AUTOMICOSTA legge il documento sul telefono ed estrae i dati riconosciuti.",
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )
        LibrettoScanButton { lastData = it }
        lastData?.let { data ->
            Text("Dati riconosciuti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            val summary = listOfNotNull(
                data.plate.takeIf { it.isNotBlank() }?.let { "Targa: $it" },
                data.vin.takeIf { it.isNotBlank() }?.let { "VIN: $it" },
                data.brand.takeIf { it.isNotBlank() }?.let { "Marca: $it" },
                data.model.takeIf { it.isNotBlank() }?.let { "Modello: $it" },
                data.firstRegistrationDate.takeIf { it.isNotBlank() }?.let { "Prima immatricolazione: $it" },
                data.fuelType.takeIf { it.isNotBlank() }?.let { "Alimentazione: $it" }
            ).joinToString("\n")
            Text(if (summary.isBlank()) "Documento acquisito: controlla il testo riconosciuto." else summary, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

private fun hasNoPin(context: Context): Boolean =
    context.getSharedPreferences("automicosta_security", Context.MODE_PRIVATE).getString("pin_hash", null) == null

@Composable
private fun PartsPinGate(context: Context, onUnlocked: () -> Unit) {
    val prefs = remember { context.getSharedPreferences("automicosta_security", Context.MODE_PRIVATE) }
    val savedHash = remember { prefs.getString("pin_hash", null) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(28.dp)) {
        Text("RICAMBI", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Text("Catalogo ricambi del veicolo selezionato", style = MaterialTheme.typography.titleMedium)
        Text("Inserisci lo stesso PIN di AUTOMICOSTA per accedere a telaio/VIN, categorie ricambi, codici OE/OEM e ricerca prezzi.", modifier = Modifier.padding(top = 10.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(8); error = "" },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        TextButton(
            onClick = { if (savedHash != null && hashPinForParts(pin) == savedHash) onUnlocked() else error = "PIN non corretto." },
            enabled = pin.length in 4..8,
            modifier = Modifier.padding(top = 8.dp)
        ) { Text("Apri RICAMBI", fontWeight = FontWeight.Bold) }
    }
}

private fun hashPinForParts(pin: String): String =
    MessageDigest.getInstance("SHA-256").digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
