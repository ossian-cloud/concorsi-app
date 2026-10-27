package cloud.ossian.app.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cloud.ossian.app.R
import cloud.ossian.app.data.Call
import cloud.ossian.app.data.Feed
import cloud.ossian.app.data.Filter
import cloud.ossian.app.data.deadline
import cloud.ossian.app.data.describe

private const val MAX_SHOWN = 300

private enum class Sheet { Region, Province, Profession, Education, Within }

@Composable
fun SearchScreen(
    vm: AppViewModel, feed: Feed, onOpen: (Call) -> Unit, onToggleSave: (Call) -> Unit, onAlert: (Filter) -> Unit,
) {
    val f = vm.filter
    var sheet by rememberSaveable { mutableStateOf<Sheet?>(null) }
    val now = vm.now()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            TextField(
                value = f.words, onValueChange = { vm.search(f.copy(words = it)) }, singleLine = true,
                placeholder = { Text("Professione, ente, comune…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (f.words.isNotEmpty()) IconButton(onClick = { vm.search(f.copy(words = "")) }) { Icon(Icons.Filled.Clear, "Cancella") }
                },
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp),
            )
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterButton("Regione", feed.regions[f.region]) { sheet = Sheet.Region } }
                item { FilterButton("Provincia", feed.provinces[f.province]) { sheet = Sheet.Province } }
                item { FilterButton("Professione", feed.profiles[f.profile]) { sheet = Sheet.Profession } }
                item { FilterButton("Titolo di studio", feed.profiles[f.education]) { sheet = Sheet.Education } }
                item { FilterButton("Scadenza", if (f.within > 0) "Entro ${f.within} giorni" else null) { sheet = Sheet.Within } }
            }
        }
        if (vm.offerAlert && !f.isEmpty && !vm.hasAlert(f)) item {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vuoi un avviso per questa ricerca?", style = MaterialTheme.typography.titleMedium)
                    Text("Ti mando una notifica quando esce un nuovo concorso: ${f.describe(feed)}.", style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { vm.offerAlert = false }) { Text("No, grazie") }
                        Button(onClick = { vm.offerAlert = false; onAlert(f) }) { Text("Crea avviso") }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val n = vm.results.size
                Text(if (n == 1) "1 concorso aperto" else "$n concorsi aperti", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                if (!f.isEmpty) TextButton(onClick = { vm.search(Filter()) }) { Text("Azzera") }
                SortMenu(vm.sort, vm::sortBy)
            }
        }
        if (!f.isEmpty) item {
            Box(Modifier.padding(horizontal = 16.dp)) {
                if (vm.hasAlert(f)) Tag("Avviso attivo per questa ricerca", iconId = R.drawable.ic_notifications_none)
                else OutlinedButton(onClick = { onAlert(f) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Notifications, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("Avvisami per questa ricerca")
                }
            }
        }
        if (vm.results.isEmpty()) item {
            EmptyState(
                icon(Icons.Filled.Search), "Nessun concorso aperto",
                "Prova con meno parole o togli qualche filtro.",
                action = if (f.isEmpty) null else "Azzera la ricerca",
            ) { vm.search(Filter()) }
        }
        items(vm.results.take(MAX_SHOWN), key = { it.id }) { c ->
            Box(Modifier.padding(horizontal = 16.dp)) {
                CallCard(feed, c, deadline(c.s, now), vm.isSaved(c), vm.isNew(c), onClick = { onOpen(c) }, onToggleSave = { onToggleSave(c) })
            }
        }
        if (vm.results.size > MAX_SHOWN) item {
            Text("Mostro i primi $MAX_SHOWN: aggiungi una parola o un filtro.", Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    val close = { sheet = null }
    when (sheet) {
        Sheet.Region -> OptionSheet("Regione", feed.regions.toList().sortedBy { it.second }, f.region, "Tutta Italia",
            onPick = { vm.search(f.copy(region = it, province = if (it == f.region) f.province else "")); close() }, onDismiss = close)
        Sheet.Province -> OptionSheet("Provincia", vm.provinces(f.region), f.province, "Tutte le province",
            note = "Solo per i concorsi di Comuni, Province e Città metropolitane" +
                (feed.regions[f.region]?.let { " · $it" } ?: ""),
            onPick = { vm.search(f.copy(province = it)); close() }, onDismiss = close)
        Sheet.Profession -> OptionSheet("Professione", vm.professions(), f.profile, "Tutte le professioni",
            note = "Stima dal titolo del bando", onPick = { vm.search(f.copy(profile = it)); close() }, onDismiss = close)
        Sheet.Education -> OptionSheet("Titolo di studio", vm.education(), f.education, "Qualsiasi",
            note = "Stima dal titolo del bando", onPick = { vm.search(f.copy(education = it)); close() }, onDismiss = close)
        Sheet.Within -> OptionSheet("Scadenza", listOf("7" to "Entro 7 giorni", "30" to "Entro 30 giorni"),
            if (f.within > 0) f.within.toString() else "", "Qualsiasi",
            onPick = { vm.search(f.copy(within = it.toIntOrNull() ?: 0)); close() }, onDismiss = close)
        null -> {}
    }
}

@Composable
private fun FilterButton(label: String, value: String?, onClick: () -> Unit) {
    FilterChip(
        selected = value != null, onClick = onClick,
        label = { Text(value ?: label, Modifier.widthIn(max = 200.dp), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = if (value != null) ({ Icon(Icons.Filled.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }) else null,
        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(FilterChipDefaults.IconSize)) },
    )
}

@Composable
private fun SortMenu(sort: Sort, onSort: (Sort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Icon(icon(R.drawable.ic_sort), null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(sort.label)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Sort.entries.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.label) }, onClick = { onSort(s); open = false },
                    trailingIcon = { if (s == sort) Icon(Icons.Filled.Check, null) },
                )
            }
        }
    }
}
