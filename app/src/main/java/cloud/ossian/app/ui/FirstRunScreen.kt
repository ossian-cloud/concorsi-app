package cloud.ossian.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cloud.ossian.app.data.Feed
import cloud.ossian.app.data.Filter

/** One skippable screen: where and which job; the answers become the first search (and, if wanted, an alert). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FirstRunScreen(vm: AppViewModel, feed: Feed?, onDone: (Filter?) -> Unit) {
    var region by rememberSaveable { mutableStateOf("") }
    var province by rememberSaveable { mutableStateOf("") }
    var profile by rememberSaveable { mutableStateOf("") }
    var provinceSheet by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { onDone(null) }) { Text("Salta") }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { onDone(Filter(region = region, province = province, profile = profile)) }, enabled = feed != null) {
                        Text("Mostra i concorsi")
                    }
                }
            }
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Concorsi pubblici aperti", style = MaterialTheme.typography.headlineMedium)
            Text("Due domande per partire: puoi cambiare tutto dopo, oppure saltare.",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (feed == null) {
                Text(vm.error ?: "Scarico i concorsi aperti…", Modifier.padding(top = 16.dp))
                return@Column
            }
            Text("Dove cerchi?", Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                feed.regions.toList().sortedBy { it.second }.forEach { (k, v) ->
                    Choice(v, region == k) { region = if (region == k) "" else k; province = "" }
                }
            }
            if (region.isNotEmpty()) {
                OutlinedButton(onClick = { provinceSheet = true }) {
                    Icon(Icons.Filled.LocationOn, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(feed.provinces[province]?.let { "Provincia di $it" } ?: "Provincia (facoltativa)")
                    Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(18.dp))
                }
            }
            Text("Che lavoro?", Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.professions().filterNot { it.second.startsWith("Medici: ") }.forEach { (k, v) ->
                    Choice(v, profile == k) { profile = if (profile == k) "" else k }
                }
            }
            Text("Nessun dato esce dal telefono: le scelte restano qui.", Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (provinceSheet && feed != null) OptionSheet(
        "Provincia", vm.provinces(region), province, "Tutte le province",
        note = "Solo per i concorsi di Comuni, Province e Città metropolitane",
        onPick = { province = it; provinceSheet = false }, onDismiss = { provinceSheet = false },
    )
}

@Composable
private fun Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick, label = { Text(text) },
        leadingIcon = if (selected) ({ Icon(Icons.Filled.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }) else null,
    )
}
