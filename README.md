# AUTOMICOSTA

MVP Android per registrare i costi dell'auto e capire quanto costa davvero possederla.

## Funzioni incluse

- Configurazione iniziale del veicolo
- Dashboard con totale spese e costo/km
- Inserimento spese per categoria
- Rifornimenti e ricariche con litri/kWh e prezzo unitario
- Chilometraggio associato alle spese
- Scadenze/manutenzione a chilometraggio
- Persistenza locale con Room
- UI nativa Kotlin + Jetpack Compose / Material 3
- Nessun account e nessun server richiesto

## Aprire il progetto

1. Apri la cartella `AUTOMICOSTA` con Android Studio.
2. Installa Android SDK API 37 se richiesto.
3. Lascia completare Gradle Sync.
4. Avvia su emulatore o dispositivo Android API 23+.

## Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room 2.8.4
- Compose BOM 2026.08.00
- Android Gradle Plugin 9.4.0
- Gradle 9.6.0

## Stato MVP

La versione 0.1.0 è pensata come fondazione. I prossimi moduli consigliati sono: scadenze a data con notifiche, gestione multi-auto nella UI, modifica/cancellazione movimenti, statistiche mensili, documenti/fatture, esportazione CSV/PDF, backup e calcolo svalutazione.

### Nota Gradle

Il pacchetto contiene i sorgenti del progetto ma non il file binario `gradle-wrapper.jar`. Android Studio può configurare Gradle all'importazione; in alternativa genera un wrapper Gradle 9.6.x (`gradle wrapper --gradle-version 9.6.0`).

## APK
Per generare un APK installabile, consulta `BUILD_APK.md`. Il progetto include anche `.github/workflows/build-apk.yml` per compilare automaticamente l'APK con GitHub Actions.
