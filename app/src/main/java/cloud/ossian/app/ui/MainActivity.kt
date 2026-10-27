package cloud.ossian.app.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import cloud.ossian.app.R
import cloud.ossian.app.data.Call
import cloud.ossian.app.data.Feed
import cloud.ossian.app.data.ROME
import cloud.ossian.app.data.headline
import cloud.ossian.app.data.parseRome
import cloud.ossian.app.work.CheckWorker

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        CheckWorker.channels(this)
        CheckWorker.schedule(this)
        setContent { OssianTheme { App(vm) } }
    }
}

private enum class Tab(val label: String, val title: String) {
    Cerca("Cerca", "Concorsi pubblici"), Salvati("Salvati", "Salvati"), Avvisi("Avvisi", "Avvisi"), Info("Info", "Informazioni")
}

@Composable
private fun tabIcon(t: Tab, selected: Boolean): Painter = when (t) {
    Tab.Cerca -> icon(Icons.Filled.Search)
    Tab.Salvati -> icon(if (selected) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border)
    Tab.Avvisi -> if (selected) icon(Icons.Filled.Notifications) else icon(R.drawable.ic_notifications_none)
    Tab.Info -> icon(if (selected) Icons.Filled.Info else Icons.Outlined.Info)
}

/** Android 13+: before the system dialog, one line saying why; asked only when the user saves a call or creates an alert. */
@Composable
private fun rememberNotificationAsk(): () -> Unit {
    val ctx = LocalContext.current
    var explain by remember { mutableStateOf(false) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    if (explain) AlertDialog(
        onDismissRequest = { explain = false },
        icon = { Icon(icon(Icons.Filled.Notifications), null) },
        title = { Text("Notifiche") },
        text = { Text("Per ricordarti le scadenze e avvisarti dei nuovi concorsi la app deve poter mostrare notifiche. Nessun dato esce dal telefono.") },
        confirmButton = {
            TextButton(onClick = { explain = false; if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                Text("Continua")
            }
        },
        dismissButton = { TextButton(onClick = { explain = false }) { Text("Non ora") } },
    )
    return {
        if (Build.VERSION.SDK_INT >= 33 && !asked &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) { asked = true; explain = true }
    }
}

private fun Context.openUrl(url: String) {
    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "Nessuna app per aprire il link", Toast.LENGTH_SHORT).show()
    }
}

private fun Context.share(feed: Feed, c: Call) {
    val text = listOfNotNull(c.headline(), c.e.ifEmpty { null }, dayAndHour(c.s).ifEmpty { null }?.let { "Scadenza: $it" }, feed.base + c.u)
        .joinToString("\n")
    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
}

/** Opens the calendar app on a new all-day event on the deadline day; no calendar permission needed. */
private fun Context.addToCalendar(c: Call) {
    val end = parseRome(c.s) ?: return
    val start = end.toLocalDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()  // all-day events are UTC midnights
    val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, "Scadenza: ${c.headline()}")
        .putExtra(CalendarContract.Events.DESCRIPTION, "${c.e}\nScade il ${dayAndHour(c.s)} (ora italiana).\nBando ufficiale: ${c.x}")
        .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 86_400_000L)
        .putExtra(CalendarContract.Events.EVENT_TIMEZONE, ROME.id)
    try { startActivity(intent) } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "Nessuna app calendario trovata", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(vm: AppViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.Cerca) }
    val ctx = LocalContext.current
    val askNotifications = rememberNotificationAsk()
    val feed = vm.feed
    val toggleSave = { c: Call -> if (!vm.isSaved(c)) askNotifications(); vm.toggleSaved(c) }
    val createAlert = { f: cloud.ossian.app.data.Filter -> askNotifications(); vm.addAlert(f) }

    if (!vm.ready) return
    if (vm.firstRun) {
        FirstRunScreen(vm, feed) { f -> vm.finishFirstRun(f); tab = Tab.Cerca }
        return
    }
    val selected = vm.selected
    if (selected != null && feed != null) {
        BackHandler { vm.selected = null }
        DetailScreen(
            vm, feed, selected, onBack = { vm.selected = null }, onToggleSave = { toggleSave(selected) },
            open = ctx::openUrl, share = { ctx.share(feed, selected) }, addToCalendar = { ctx.addToCalendar(selected) },
        )
        return
    }
    BackHandler(enabled = tab != Tab.Cerca) { tab = Tab.Cerca }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tab.title) },
                actions = {
                    if (tab == Tab.Cerca && feed != null) androidx.compose.material3.IconButton(onClick = vm::refresh, enabled = !vm.loading) {
                        Icon(Icons.Filled.Refresh, "Aggiorna")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t, onClick = { tab = t },
                        icon = { Icon(tabIcon(t, tab == t), contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (vm.loading && feed != null) LinearProgressIndicator(Modifier.fillMaxWidth())
            when {
                feed == null && vm.error != null -> Centered {
                    EmptyState(icon(Icons.Filled.Refresh), "Nessuna connessione", vm.error.orEmpty(), "Riprova", vm::refresh)
                }
                feed == null -> Centered { androidx.compose.material3.CircularProgressIndicator() }
                tab == Tab.Cerca -> SearchScreen(vm, feed, onOpen = { vm.selected = it }, onToggleSave = toggleSave,
                    onAlert = { createAlert(it) })
                tab == Tab.Salvati -> SavedScreen(vm, feed, onOpen = { vm.selected = it }, onSearch = { tab = Tab.Cerca })
                tab == Tab.Avvisi -> AlertsScreen(vm, feed, onShow = { vm.search(it); tab = Tab.Cerca }, onSearch = { tab = Tab.Cerca })
                else -> InfoScreen(feed)
            }
        }
    }
}
