package cloud.ossian.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cloud.ossian.app.BuildConfig
import cloud.ossian.app.R
import cloud.ossian.app.data.Alert
import cloud.ossian.app.data.Call
import cloud.ossian.app.data.Feed
import cloud.ossian.app.data.Filter
import cloud.ossian.app.data.SITE
import cloud.ossian.app.data.STATUSES
import cloud.ossian.app.data.deadline
import cloud.ossian.app.data.describe

@Composable
fun SavedScreen(vm: AppViewModel, feed: Feed, onOpen: (Call) -> Unit, onSearch: () -> Unit) {
    val (live, closed) = vm.savedCalls()
    if (live.isEmpty() && closed.isEmpty()) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            EmptyState(
                icon(R.drawable.ic_bookmark_border), "Nessun concorso salvato",
                "Tocca il segnalibro su un concorso: lo trovi qui e ti avviso 7 giorni e 1 giorno prima della scadenza.",
                "Cerca concorsi", onSearch,
            )
        }
        return
    }
    val now = vm.now()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (live.isNotEmpty()) item { SectionHeader("Aperti · ${live.size}") }
        items(live, key = { it.id }) { c ->
            CallCard(feed, c, deadline(c.s, now), saved = true, isNew = vm.isNew(c), onClick = { onOpen(c) },
                onToggleSave = { vm.toggleSaved(c) }) {
                if (c.s.isNotEmpty()) Tag("Promemoria 7 e 1 giorno prima", iconId = R.drawable.ic_notifications_none)
                StatusChip(vm.user.status[c.id].orEmpty()) { vm.setStatus(c, it) }
            }
        }
        if (closed.isNotEmpty()) item { SectionHeader("Scaduti · ${closed.size}", Modifier.padding(top = 8.dp)) }
        items(closed, key = { it.id }) { c ->
            CallCard(feed, c, deadline(c.s, now), saved = true, isNew = false, onClick = { onOpen(c) },
                onToggleSave = { vm.toggleSaved(c) }) {
                StatusChip(vm.user.status[c.id].orEmpty()) { vm.setStatus(c, it) }
            }
        }
    }
}

/** The personal status of a saved call ("Da preparare", "Domanda inviata"), picked from a small menu. */
@Composable
private fun StatusChip(status: String, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        val set = status.isNotEmpty()
        val cs = MaterialTheme.colorScheme
        // same size as the card's Tag labels, so the row stays aligned
        Surface(
            onClick = { open = true }, shape = RoundedCornerShape(8.dp),
            color = if (set) cs.secondaryContainer else Color.Transparent,
            contentColor = if (set) cs.onSecondaryContainer else cs.onSurfaceVariant,
            border = if (set) null else BorderStroke(1.dp, cs.outline),
        ) {
            Row(Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (set) { Icon(Icons.Filled.Check, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
                Text(status.ifEmpty { "Stato" }, style = MaterialTheme.typography.labelMedium)
                Icon(Icons.Filled.ArrowDropDown, "Cambia stato", Modifier.size(18.dp))
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            (listOf("") + STATUSES).forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.ifEmpty { "Nessuno stato" }) }, onClick = { onPick(s); open = false },
                    trailingIcon = { if (s == status) Icon(Icons.Filled.Check, null) },
                )
            }
        }
    }
}

@Composable
fun AlertsScreen(vm: AppViewModel, feed: Feed, onShow: (Filter) -> Unit, onSearch: () -> Unit) {
    val alerts = vm.user.alerts
    if (alerts.isEmpty()) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            EmptyState(
                icon(R.drawable.ic_notifications_none), "Nessun avviso",
                "Imposta una ricerca in «Cerca» (per esempio regione e professione) e tocca «Avvisami per questa ricerca». " +
                    "Ti mando una notifica quando esce un nuovo concorso che corrisponde. Il telefono controlla da solo ogni qualche ora.",
                "Vai a Cerca", onSearch,
            )
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Ti avviso quando esce un nuovo concorso per queste ricerche. Il controllo avviene sul telefono, ogni qualche ora.",
                Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(alerts, key = { it.id }) { a -> AlertCard(vm, feed, a, onShow) }
    }
}

@Composable
private fun AlertCard(vm: AppViewModel, feed: Feed, a: Alert, onShow: (Filter) -> Unit) {
    val on = a.enabled
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (on) icon(Icons.Filled.Notifications) else icon(R.drawable.ic_notifications_none), null,
                    Modifier.size(20.dp), tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.filter.describe(feed), style = MaterialTheme.typography.titleMedium,
                        color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (on) "Attivo" else "In pausa", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = on, onCheckedChange = { vm.setAlertEnabled(a, it) }, modifier = Modifier.padding(start = 8.dp, end = 8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val n = vm.countOpen(a.filter)
                TextButton(onClick = { onShow(a.filter) }) {
                    Text(if (n == 1) "1 concorso aperto" else "$n concorsi aperti")
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { vm.removeAlert(a) }) {
                    Icon(Icons.Filled.Delete, "Elimina l'avviso", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun InfoScreen(feed: Feed) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InfoCard("Chi la fa", "Questa app è fatta da Ossian, un agente AI, senza legami con le amministrazioni, con inPA " +
            "o con la Gazzetta Ufficiale. È un estratto non ufficiale: fa sempre fede il bando.")
        InfoCard("Da dove vengono i dati", "Portale del Reclutamento inPA (Dipartimento della Funzione Pubblica, CC BY 3.0) e " +
            "Gazzetta Ufficiale, 4ª serie speciale. L'elenco si aggiorna più volte al giorno.")
        InfoCard("Limiti", "Professione e titolo di studio sono stime ricavate dal titolo del bando, non dati ufficiali. " +
            "Scadenze e dettagli possono essere in ritardo o sbagliati: controlla sempre la pagina ufficiale.")
        InfoCard("Privacy", "La app non raccoglie dati. Ricerche, concorsi salvati e avvisi restano sul telefono; la app " +
            "scarica soltanto l'elenco pubblico dei concorsi da ossian.cloud. Niente account, pubblicità o tracciamento.")
        OutlinedButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SITE))) }, modifier = Modifier.fillMaxWidth()) {
            Icon(icon(R.drawable.ic_language), null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text("Il sito: ossian.cloud/concorsi")
        }
        TextButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ossian.cloud/app/privacy.html"))) },
            modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Informativa privacy completa") }
        Text("Versione ${BuildConfig.VERSION_NAME} · dati aggiornati il ${updatedRome(feed.updated)}",
            Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoCard(title: String, text: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
