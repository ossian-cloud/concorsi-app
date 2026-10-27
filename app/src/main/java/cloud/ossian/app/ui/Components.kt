package cloud.ossian.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cloud.ossian.app.R
import cloud.ossian.app.data.Call
import cloud.ossian.app.data.Deadline
import cloud.ossian.app.data.Feed
import cloud.ossian.app.data.Urgency
import cloud.ossian.app.data.fold
import cloud.ossian.app.data.headline
import cloud.ossian.app.data.place

const val DISCLAIMER = "Estratto non ufficiale preparato da Ossian, un agente AI: i dati possono essere in ritardo o " +
    "sbagliati. Fa fede il bando ufficiale."

/** Short labels for the education keys, as they appear on cards. */
val EDUCATION_SHORT = mapOf("laureati" to "Laurea", "diplomati" to "Diploma", "scuola-obbligo" to "Scuola dell'obbligo")

@Composable fun icon(@DrawableRes id: Int): Painter = painterResource(id)
@Composable fun icon(v: ImageVector): Painter = rememberVectorPainter(v)

fun posts(n: Int) = if (n == 1) "1 posto" else "$n posti"

/** "Concorso · Comune di Orsenigo": the kind (if known) and the ente. */
fun Call.kindAndEnte() = listOf(k, e).filter { it.isNotBlank() }.joinToString(" · ")

@Composable
fun DeadlineBadge(d: Deadline, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val (bg, fg) = when (d.urgency) {
        Urgency.Urgent -> cs.errorContainer to cs.onErrorContainer
        Urgency.Soon -> cs.tertiaryContainer to cs.onTertiaryContainer
        Urgency.Normal -> cs.secondaryContainer to cs.onSecondaryContainer
        Urgency.Unknown, Urgency.Expired -> cs.surfaceContainerHighest to cs.onSurfaceVariant
    }
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon(R.drawable.ic_schedule), null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(d.text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** A small non-interactive label: outlined, or filled when [fill] is given. */
@Composable
fun Tag(text: String, fill: Color? = null, content: Color = MaterialTheme.colorScheme.onSurfaceVariant, @DrawableRes iconId: Int? = null) {
    Surface(
        color = fill ?: Color.Transparent, contentColor = content, shape = RoundedCornerShape(8.dp),
        border = if (fill == null) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (iconId != null) { Icon(icon(iconId), null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun BookmarkButton(saved: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            icon(if (saved) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border),
            contentDescription = if (saved) "Togli dai salvati" else "Salva",
            tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CallCard(
    feed: Feed, c: Call, deadline: Deadline, saved: Boolean, isNew: Boolean,
    onClick: () -> Unit, onToggleSave: () -> Unit, extra: @Composable () -> Unit = {},
) {
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 16.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isNew) Tag("Nuovo", fill = MaterialTheme.colorScheme.primary, content = MaterialTheme.colorScheme.onPrimary)
                    Text(c.headline(), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(c.kindAndEnte(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                BookmarkButton(saved, onToggleSave)
            }
            val place = feed.place(c)
            if (place.isNotEmpty()) {
                Row(Modifier.padding(top = 4.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(place, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            FlowRow(
                Modifier.padding(top = 12.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DeadlineBadge(deadline)
                c.n?.let { Tag(posts(it), iconId = R.drawable.ic_groups) }
                c.f.mapNotNull { EDUCATION_SHORT[it] }.forEach { Tag(it, iconId = R.drawable.ic_school) }
                extra()
            }
        }
    }
}

@Composable
fun EmptyState(painter: Painter, title: String, text: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
            Icon(painter, null, Modifier.padding(16.dp).size(32.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) FilledTonalButton(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) { Text(action) }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(top = 8.dp, bottom = 0.dp), style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
}

/** A single-choice list in a bottom sheet; the first row ([anyLabel]) clears the choice. Searchable when long. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionSheet(
    title: String, options: List<Pair<String, String>>, selected: String, anyLabel: String,
    onPick: (String) -> Unit, onDismiss: () -> Unit, note: String? = null,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = options.size > 8)
    var q by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth()) {
            Text(title, Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.titleLarge)
            if (note != null) Text(note, Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (options.size > 8) {
                OutlinedTextField(
                    value = q, onValueChange = { q = it }, singleLine = true, placeholder = { Text("Cerca") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = { q = "" }) { Icon(Icons.Filled.Clear, "Cancella") } },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            val shown = if (q.isBlank()) options else options.filter { fold(it.second).contains(fold(q.trim())) }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 560.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (q.isBlank()) item { OptionRow(anyLabel, selected.isEmpty()) { onPick("") } }
                items(shown, key = { it.first }) { (k, v) -> OptionRow(v, k == selected) { onPick(k) } }
                if (shown.isEmpty()) item {
                    Text("Nessun risultato", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun OptionRow(text: String, checked: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text, fontWeight = if (checked) FontWeight.SemiBold else null) },
        trailingContent = { if (checked) Icon(Icons.Filled.Check, "Selezionato", tint = MaterialTheme.colorScheme.primary) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp),
    )
}

@Composable
fun Centered(content: @Composable () -> Unit) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }

@Composable
fun Divider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
