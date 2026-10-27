package cloud.ossian.app.data

import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Lower case, no accents, one kind of apostrophe: the same folding the site's search uses. */
fun fold(s: String): String =
    Normalizer.normalize(s.lowercase().replace('’', '\'').replace('‘', '\''), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

/** Words of 5+ letters lose a final vowel, so "infermiere" also finds "infermieri" (as on the site). */
fun stem(w: String): String = if (w.length >= 5 && w.last() in "aeiou") w.dropLast(1) else w

fun terms(q: String): List<String> = fold(q).split(Regex("[^a-z0-9']+")).filter { it.length >= 2 }.map(::stem)

fun Call.haystack(): String = fold("$t $tt $w $e $c")

/** [today] is the Rome date, used only by the deadline filter. */
fun Filter.matches(call: Call, hay: String = call.haystack(), today: LocalDate = romeToday()): Boolean =
    (region.isEmpty() || region in call.r) &&
        (province.isEmpty() || call.p == province) &&
        (profile.isEmpty() || profile in call.f) &&
        (education.isEmpty() || education in call.f) &&
        (within == 0 || daysLeft(call.s, today)?.let { it in 0..within } == true) &&
        terms(words).all { it in hay }

private val ROME_IN = DateTimeFormatter.ISO_LOCAL_DATE_TIME
private val DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun parseRome(s: String): LocalDateTime? =
    runCatching { LocalDateTime.parse(if (s.length == 16) "$s:00" else s, ROME_IN) }.getOrNull()

fun day(s: String): String = parseRome(s)?.format(DAY) ?: ""

/** Calls still open at [now] (Rome wall clock), soonest deadline first; unknown deadlines last. */
fun openSorted(calls: List<Call>, now: LocalDateTime): List<Call> =
    calls.filter { c -> parseRome(c.s)?.isAfter(now) ?: true }
        .sortedWith(compareBy({ it.s.isEmpty() }, { it.s }, { it.id }))

fun Feed.place(c: Call): String = listOfNotNull(
    c.c.ifEmpty { null },
    provinces[c.p]?.let { if (c.c.isNotEmpty() && fold(it) == fold(c.c)) null else "prov. $it" },
    c.r.mapNotNull { regions[it] }.joinToString(", ").ifEmpty { null },
).joinToString(" · ")

fun Filter.describe(feed: Feed): String = listOfNotNull(
    words.trim().ifEmpty { null }?.let { "«$it»" },
    feed.profiles[profile],
    feed.profiles[education],
    feed.provinces[province]?.let { "prov. $it" },
    feed.regions[region],
    if (within > 0) "scadenza entro $within giorni" else null,
).joinToString(" · ").ifEmpty { "Tutti i concorsi" }

val ROME: ZoneId = ZoneId.of("Europe/Rome")
fun romeToday(): LocalDate = LocalDate.now(ROME)

/** Whole days from [today] to the deadline day (0 = today, negative = past); null when unknown. */
fun daysLeft(s: String, today: LocalDate): Int? =
    parseRome(s)?.toLocalDate()?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it).toInt() }

enum class Urgency { Unknown, Expired, Urgent, Soon, Normal }

/** What the deadline badge says and how loud it is: urgent within 3 days, soon within 7. */
data class Deadline(val text: String, val urgency: Urgency)

fun deadline(s: String, now: LocalDateTime): Deadline {
    val end = parseRome(s) ?: return Deadline("Scadenza non indicata", Urgency.Unknown)
    if (!end.isAfter(now)) return Deadline("Scaduto il ${day(s)}", Urgency.Expired)
    val d = daysLeft(s, now.toLocalDate())!!
    val text = when (d) { 0 -> "Scade oggi"; 1 -> "Scade domani"; else -> "Scade tra $d giorni" }
    return Deadline(text, when { d <= 3 -> Urgency.Urgent; d <= 7 -> Urgency.Soon; else -> Urgency.Normal })
}

fun isOpen(c: Call, now: LocalDateTime) = parseRome(c.s)?.isAfter(now) ?: true

/** Card and detail headline: the job alone when known, else the short title without a "Kind:" prefix that the kind line repeats. */
fun Call.headline(): String {
    if (w.isNotBlank()) return w
    val m = Regex("^([\\p{L}.]+):\\s+(.+)$").find(t) ?: return t
    val head = m.groupValues[1]
    return if (k.isNotEmpty() && fold(k).startsWith(fold(head).trimEnd('.'))) m.groupValues[2].replaceFirstChar { it.uppercase() } else t
}

/** "BANDO DI CONCORSO PER..." -> "Bando di concorso per..." (abbreviations make later sentence starts unreliable,
 *  so only the first letter is raised). Mixed-case text is left alone. */
fun sentenceCase(s: String, names: List<String> = emptyList()): String {
    val letters = s.filter { it.isLetter() }
    if (letters.isEmpty() || letters.count { it.isUpperCase() } < letters.length * 0.6) return s
    var out = s.lowercase().replaceFirstChar { it.uppercase() }
    // known names (ente, comune, province, region) get their capitals back: "Comune di Rescaldina"
    for (n in names.filter { it.length >= 3 }.sortedByDescending { it.length }) {
        out = out.replace(Regex("(?<![\\p{L}])" + Regex.escape(n.lowercase()) + "(?![\\p{L}])"), Regex.escapeReplacement(n))
    }
    return out
}

/** "inPA" or "Gazzetta Ufficiale", from the official URL. */
fun sourceName(url: String): String = when {
    "gazzettaufficiale" in url -> "Gazzetta Ufficiale"
    "inpa.gov" in url -> "inPA"
    else -> "fonte ufficiale"
}
