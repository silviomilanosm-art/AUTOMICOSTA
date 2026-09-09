package com.automicosta.app.ui

import com.automicosta.app.data.MaintenanceEntity
import java.text.SimpleDateFormat
import java.util.Locale

private fun historyDay(value: String): Long =
    (SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).apply { isLenient = false }.parse(value)?.time ?: 0L) / 86_400_000L

private fun historyItem(
    vehicleId: Long,
    date: String?,
    component: String,
    km: Int? = null,
    cost: Double = 0.0,
    workshop: String = "In proprio",
    workType: String = "Manutenzione",
    note: String = "",
    yearOnly: Int? = null
): MaintenanceEntity = MaintenanceEntity(
    vehicleId = vehicleId,
    dateEpochDay = when {
        date != null -> historyDay(date)
        yearOnly != null -> -yearOnly.toLong()
        else -> -1L
    },
    component = component,
    workType = workType,
    cost = cost,
    odometerKm = km,
    workshop = workshop,
    note = note
)

fun kiaVengaEcoGplHistory(vehicleId: Long): List<MaintenanceEntity> = listOf(
    historyItem(vehicleId, "01/12/2015", "Olio + filtro olio + filtro GPL", 20228, workshop = "Automobili Massimino S.n.c."),
    historyItem(vehicleId, "05/09/2016", "Olio + filtro olio + filtro aria", 41592, workshop = "Automobili Massimino S.n.c."),
    historyItem(vehicleId, "03/04/2017", "Olio + filtro olio + filtro GPL + candele", 60176, workshop = "Automobili Massimino S.n.c."),
    historyItem(vehicleId, "26/09/2017", "Tutti i filtri + controlli GPL", 79883, workshop = "Norauto Italia S.p.A. - Moncalieri", note = "Manutenzione/controlli GPL annotati nel documento."),
    historyItem(vehicleId, "26/03/2018", "Olio + filtro olio", 99864, workshop = "Automobili Massimino S.n.c."),
    historyItem(vehicleId, "06/11/2018", "Olio + filtro olio + filtro GPL + filtro antipolline + candele", 121170, workshop = "Automobili Massimino S.n.c."),
    historyItem(vehicleId, null, "Olio + filtro olio + filtro aria + filtro abitacolo", 155600, workshop = "In proprio", note = "Il documento indica solo l'anno 2019; giorno e mese non sono indicati.", yearOnly = 2019),
    historyItem(vehicleId, "28/07/2020", "Filtro GPL + olio", 182500, 20.0, note = "Annotazione originale parzialmente leggibile."),
    historyItem(vehicleId, "18/02/2021", "Olio motore + filtro", 184177),
    historyItem(vehicleId, "21/04/2021", "Cinghia servizi", 187000, workType = "Sostituzione"),
    historyItem(vehicleId, "03/05/2021", "Cambio pneumatici", 189000, 282.0, workType = "Sostituzione"),
    historyItem(vehicleId, "17/08/2021", "Dischi freni anteriori/posteriori + pastiglie + candele", 198007, 320.0, workType = "Sostituzione", note = "Importi documentati: EUR 240,00 + EUR 80,00."),
    historyItem(vehicleId, "25/10/2021", "Olio + filtro olio + filtro aria + filtro abitacolo + inversione pneumatici", 203676),
    historyItem(vehicleId, "17/05/2022", "Olio + filtro olio + filtro aria + filtro antipolline", 220500),
    historyItem(vehicleId, "02/08/2022", "Biellette SX/DX barra stabilizzatrice - 216 mm", 229000, workType = "Sostituzione"),
    historyItem(vehicleId, "30/10/2022", "Filtro olio + filtro aria + filtro GPL + filtro abitacolo", 236440),
    historyItem(vehicleId, "22/11/2022", "Pneumatici Taurus + annotazione cambio olio", 238550, 400.0, workType = "Sostituzione"),
    historyItem(vehicleId, "17/03/2023", "Inversione pneumatici", 248164, 40.0),
    historyItem(vehicleId, "21/04/2023", "4 ammortizzatori", 252279, workType = "Sostituzione"),
    historyItem(vehicleId, "01/06/2023", "Olio + filtro olio + filtro aria + filtro abitacolo", 252961),
    historyItem(vehicleId, "18/07/2023", "Verniciatura musetto", null, 40.0, workType = "Ripristino"),
    historyItem(vehicleId, "12/09/2023", "Inversione pneumatici", 258150),
    historyItem(vehicleId, "28/01/2024", "Olio + filtro olio + filtro aria + filtro abitacolo + inversione pneumatici", 268448, note = "Chilometraggio indicato come approssimativo (~268.448 km)."),
    historyItem(vehicleId, "02/05/2024", "Sensore albero motore", 279550, 133.0, workType = "Sostituzione"),
    historyItem(vehicleId, "21/07/2024", "Inversione pneumatici", 277500, 15.0, note = "Chilometraggio mantenuto come riportato nel documento, anche se non perfettamente progressivo."),
    historyItem(vehicleId, "10/09/2024", "Ammortizzatori posteriori + supporti ammortizzatori posteriori", 283000, 83.90, workType = "Sostituzione"),
    historyItem(vehicleId, "10/09/2024", "Pneumatici Hankook", 284800, 69.90, workType = "Sostituzione", note = "Prezzo documentato: EUR 69,90 per pneumatico; totale non indicato, quindi non moltiplicato."),
    historyItem(vehicleId, "23/10/2024", "Cerchi in ferro + pneumatici", 286350, 150.0, workType = "Montaggio"),
    historyItem(vehicleId, "05/11/2024", "4 iniettori GPL MY9 BLU + diagnosi e controllo", 287323, 340.0, workshop = "Elettrauto Borio di Marengo Alessandro e C. s.n.c.", workType = "Sostituzione", note = "EUR 320,00 ricambi + EUR 20,00 manodopera."),
    historyItem(vehicleId, null, "Alternatore", null, 310.0, workshop = "Elettrauto Borio", workType = "Sostituzione", note = "Data e chilometraggio non indicati. EUR 249,00 ricambio + EUR 61,00 manodopera."),
    historyItem(vehicleId, "06/01/2025", "Filtro olio + filtro aria + filtro antipolline + olio + controllo livelli + filtro benzina", 293700),
    historyItem(vehicleId, "07/01/2025", "4 stagioni + sensori pressione", 300000, 55.0, workType = "Montaggio"),
    historyItem(vehicleId, "09/04/2025", "Bombolone GPL + filtro gas + cinghia servizi", 300040, 670.0, workType = "Sostituzione"),
    historyItem(vehicleId, "06/05/2025", "Bielletta/barra anteriore DX + olio freni", 301850, 70.0, workType = "Sostituzione", note = "Chilometraggio e importo indicati come approssimativi/da verificare (~301.850 km, ~EUR 70,00)."),
    historyItem(vehicleId, "13/06/2025", "Intervento Elettrauto Borio - 2 articoli/ricambi", 303700, 229.0, workshop = "Elettrauto Borio SNC", note = "Descrizione specifica non leggibile nel documento; chilometraggio indicato come approssimativo (~303.700 km)."),
    historyItem(vehicleId, "10/10/2025", "Olio + filtro olio + filtro antipolline", 311350),
    historyItem(vehicleId, "04/11/2025", "2 pneumatici anteriori + 2 posteriori", 315700, workType = "Sostituzione", note = "Data/km e importi annotati da verificare nel documento; nessun importo inserito per evitare stime."),
    historyItem(vehicleId, "29/11/2025", "Inversione pneumatici", 315600),
    historyItem(vehicleId, "30/04/2026", "Inversione pneumatici + cambio olio + tutti i filtri", 325518),
    historyItem(vehicleId, "15/06/2026", "Fascia/collare scarico", 331000, 18.90, workType = "Sostituzione", note = "Chilometraggio indicato come approssimativo (~331.000 km).")
)
