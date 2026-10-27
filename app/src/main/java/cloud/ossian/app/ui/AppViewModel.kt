package cloud.ossian.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cloud.ossian.app.data.Alert
import cloud.ossian.app.data.Call
import cloud.ossian.app.data.EDUCATION
import cloud.ossian.app.data.Feed
import cloud.ossian.app.data.Filter
import cloud.ossian.app.data.ROME
import cloud.ossian.app.data.Repository
import cloud.ossian.app.data.UserData
import cloud.ossian.app.data.haystack
import cloud.ossian.app.data.isOpen
import cloud.ossian.app.data.matches
import cloud.ossian.app.data.openSorted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

enum class Sort(val label: String) { Deadline("Scadenza più vicina"), Recent("Più recenti") }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)

    var feed by mutableStateOf<Feed?>(null); private set
    var user by mutableStateOf(UserData()); private set
    var ready by mutableStateOf(false); private set     // user data loaded
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var filter by mutableStateOf(Filter()); private set
    var sort by mutableStateOf(Sort.Recent); private set
    var results by mutableStateOf<List<Call>>(emptyList()); private set
    var selected by mutableStateOf<Call?>(null)
    /** Set after the first-run screen: the search shows an offer to turn the chosen filters into an alert. */
    var offerAlert by mutableStateOf(false)
    /** Calls modified after this UTC instant are marked "Nuovo"; "" on the very first open (nothing marked). */
    var newSince by mutableStateOf(""); private set

    private var open: List<Pair<Call, String>> = emptyList()  // open calls with their folded text, deadline order

    val firstRun get() = ready && !user.onboarded && user.saved.isEmpty() && user.alerts.isEmpty()

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val u = repo.userData()
                newSince = u.lastOpen
                user = repo.updateUser { it.copy(lastOpen = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()) }
                ready = true
                repo.cached()?.let(::useFeed)
            }
            refresh()
        }
    }

    fun now(): LocalDateTime = LocalDateTime.now(ROME)

    fun refresh() {
        if (loading) return
        loading = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { repo.refresh() } }
            result.onSuccess { fresh -> fresh?.let { withContext(Dispatchers.Default) { useFeed(it) } }; error = null }
                .onFailure { error = if (feed == null) "Non riesco a scaricare i concorsi. Controlla la connessione e riprova." else null }
            loading = false
        }
    }

    private fun useFeed(f: Feed) {
        open = openSorted(f.calls, now()).map { it to it.haystack() }
        feed = f
        search(filter)
    }

    fun search(f: Filter) {
        filter = f
        val today = now().toLocalDate()
        val hits = open.filter { (c, hay) -> f.matches(c, hay, today) }.map { it.first }
        results = if (sort == Sort.Recent) hits.sortedWith(compareByDescending<Call> { it.o }.thenByDescending { it.m }) else hits
    }

    fun sortBy(s: Sort) { sort = s; search(filter) }

    fun countOpen(f: Filter): Int {
        val today = now().toLocalDate()
        return open.count { (c, hay) -> f.matches(c, hay, today) }
    }

    /** Provinces with open local calls in the chosen region (all of them if none chosen). */
    fun provinces(region: String): List<Pair<String, String>> {
        val f = feed ?: return emptyList()
        val keys = open.map { it.first }.filter { region.isEmpty() || region in it.r }.map { it.p }.filter { it.isNotEmpty() }.toSet()
        return f.provinces.filterKeys { it in keys }.toList().sortedBy { it.second }
    }

    /** Professions: the broad groups first (feed order), then the medical specialties alphabetically. */
    fun professions(): List<Pair<String, String>> {
        val all = (feed?.profiles ?: emptyMap()).filterKeys { it !in EDUCATION }.toList()
        val (special, broad) = all.partition { it.second.startsWith("Medici: ") }
        return broad + special.sortedBy { it.second }
    }

    fun education(): List<Pair<String, String>> = EDUCATION.mapNotNull { k -> feed?.profiles?.get(k)?.let { k to it } }

    fun isSaved(c: Call) = c.id in user.saved

    fun isNew(c: Call) = newSince.isNotEmpty() && c.m > newSince

    fun toggleSaved(c: Call) = update { u ->
        if (c.id in u.saved) u.copy(saved = u.saved - c.id, status = u.status - c.id) else u.copy(saved = u.saved + (c.id to c))
    }

    fun setStatus(c: Call, status: String) = update { u ->
        u.copy(status = if (status.isEmpty() || u.status[c.id] == status) u.status - c.id else u.status + (c.id to status))
    }

    fun addAlert(f: Filter) = update { u ->
        val since = feed?.calls?.maxOfOrNull { it.m } ?: ""
        if (u.alerts.any { it.filter == f }) u else u.copy(alerts = u.alerts + Alert(System.currentTimeMillis(), f, since))
    }

    fun hasAlert(f: Filter) = user.alerts.any { it.filter == f }

    fun setAlertEnabled(a: Alert, on: Boolean) =
        update { u -> u.copy(alerts = u.alerts.map { if (it.id == a.id) it.copy(enabled = on) else it }) }

    fun removeAlert(a: Alert) = update { u -> u.copy(alerts = u.alerts.filterNot { it.id == a.id }) }

    fun finishFirstRun(f: Filter?) {
        update { it.copy(onboarded = true) }
        if (f != null && !f.isEmpty) { search(f); offerAlert = true }
    }

    /** Saved calls, soonest deadline first; ones that left the feed (closed or withdrawn) still show from the copy. */
    fun savedCalls(): Pair<List<Call>, List<Call>> {
        val now = now()
        val all = user.saved.values.map { saved -> open.firstOrNull { it.first.id == saved.id }?.first ?: saved }
        val (live, closed) = all.partition { isOpen(it, now) }
        return live.sortedWith(compareBy({ it.s.isEmpty() }, { it.s })) to closed.sortedByDescending { it.s }
    }

    private fun update(change: (UserData) -> UserData) {
        viewModelScope.launch { user = withContext(Dispatchers.IO) { repo.updateUser(change) } }
    }
}
