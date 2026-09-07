package com.automicosta.app.ui

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private enum class RootSection { APP, AI, LIBRETTO, RICAMBI }

@Composable
fun AutoMiCostaRoot(vm: AutoMiCostaViewModel) {
    var section by remember { mutableStateOf(RootSection.APP) }
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val selected = vehicles.firstOrNull()

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (section) {
                RootSection.APP -> AutoMiCostaApp(vm)
                RootSection.AI -> AiAssistantScreen(vm, vehicles)
                RootSection.LIBRETTO -> CartaCircolazioneScreen()
                RootSection.RICAMBI -> PartsScreen(selected)
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
            "Inserisci la carta di circolazione come PDF oppure foto. AUTOMICOSTA salva il documento sul telefono, lo legge e ti permette di riaprirlo con il pulsante dedicato.",
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
