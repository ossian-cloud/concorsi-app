package cloud.ossian.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import cloud.ossian.app.R
import cloud.ossian.app.data.Call
import cloud.ossian.app.data.EDUCATION
import cloud.ossian.app.data.Feed
import cloud.ossian.app.data.ROME
import cloud.ossian.app.data.day
import cloud.ossian.app.data.deadline
import cloud.ossian.app.data.headline
import cloud.ossian.app.data.parseRome
import cloud.ossian.app.data.place
import cloud.ossian.app.data.sentenceCase
import cloud.ossian.app.data.sourceName
import java.time.Instant
import java.time.format.DateTimeFormatter

private val HOUR = DateTimeFormatter.ofPattern("HH:mm")
private val STAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy 'alle' HH:mm")

/** "10/11/2026 alle 23:59" from a Rome wall-clock string. */
fun dayAndHour(s: String): String = parseRome(s)?.let { "${day(s)} alle ${it.format(HOUR)}" } ?: ""

/** Feed.updated (UTC) shown in Rome time. */
fun updatedRome(utc: String): String =
    runCatching { Instant.parse(utc).atZone(ROME).format(STAMP) }.getOrDefault(utc.take(16).replace('T', ' '))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    vm: AppViewModel, feed: Feed, c: Call, onBack: () -> Unit, onToggleSave: () -> Unit,
    open: (String) -> Unit, share: () -> Unit, addToCalendar: () -> Unit,
) {
    val d = deadline(c.s, vm.now())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Concorso") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") } },
                actions = {
                    IconButton(onClick = share) { Icon(Icons.Filled.Share, "Condividi") }
                    BookmarkButton(vm.isSaved(c), onToggleSave)
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
                Button(
                    onClick = { open(c.x) }, enabled = c.x.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(icon(R.drawable.ic_open_in_new), null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("Apri il bando ufficiale su ${sourceName(c.x)}")
                }
            }
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (vm.isNew(c)) Tag("Nuovo", fill = MaterialTheme.colorScheme.primary, content = MaterialTheme.colorScheme.onPrimary)
                val head = c.headline()
                Text(head, style = if (head.length <= 60) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge)
                Text(c.kindAndEnte(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                DeadlineBadge(d)
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Fact(icon(R.drawable.ic_schedule), "Scadenza", dayAndHour(c.s).ifEmpty { "Non indicata: controlla il bando" })
                    c.n?.let { Fact(icon(R.drawable.ic_groups), "Posti", posts(it)) }
                    Fact(icon(Icons.Filled.LocationOn), "Luogo", feed.place(c))
                    Fact(icon(Icons.Filled.DateRange), "Apertura", day(c.o))
                    Fact(icon(R.drawable.ic_work), "Professione (stima)",
                        c.f.filter { it !in EDUCATION }.mapNotNull { feed.profiles[it] }.joinToString(", "))
                    Fact(icon(R.drawable.ic_school), "Titolo di studio (stima)",
                        c.f.filter { it in EDUCATION }.mapNotNull { feed.profiles[it] }.joinToString(", "))
                }
            }
            FullTitle(sentenceCase(c.tt.ifEmpty { c.t }, listOfNotNull(c.e, c.c, feed.provinces[c.p]) + c.r.mapNotNull { feed.regions[it] }))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (parseRome(c.s) != null) OutlinedButton(onClick = addToCalendar, modifier = Modifier.fillMaxWidth()) {
                    Icon(icon(R.drawable.ic_event), null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("Aggiungi la scadenza al calendario")
                }
                OutlinedButton(onClick = { open(feed.base + c.u) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(icon(R.drawable.ic_language), null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("Pagina su ossian.cloud")
                }
            }
            Text("Fonte: ${sourceName(c.x)} · dati aggiornati il ${updatedRome(feed.updated)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Notice(DISCLAIMER)
        }
    }
}

@Composable
private fun Fact(painter: Painter, label: String, value: String) {
    if (value.isEmpty()) return
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(painter, null, Modifier.padding(top = 2.dp).size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun FullTitle(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.animateContentSize()) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Testo completo del titolo", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                if (expanded) "Chiudi" else "Apri")
        }
        if (expanded) Text(text, style = MaterialTheme.typography.bodyMedium)
        Divider()
    }
}

/** The "unofficial extract" notice, in a quiet container. */
@Composable
fun Notice(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Info, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
