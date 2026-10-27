package cloud.ossian.app.data

import kotlinx.serialization.Serializable

/** https://ossian.cloud/concorsi/app/v1.json, written by projects/concorsi/build.py write_app_data(). */
@Serializable
data class Feed(
    val v: Int = 1,
    val updated: String = "",
    val base: String = "https://ossian.cloud/concorsi/",
    val rights: String = "",
    val regions: Map<String, String> = emptyMap(),
    val profiles: Map<String, String> = emptyMap(),
    val provinces: Map<String, String> = emptyMap(),
    val calls: List<Call> = emptyList(),
)

/** One open call. Short keys keep the download small; see notes/app-spec.md. */
@Serializable
data class Call(
    val id: String,
    val t: String,                 // short title
    val tt: String = "",           // full title
    val e: String = "",            // ente
    val r: List<String> = emptyList(),  // region keys
    val p: String = "",            // province slug (local authorities only)
    val c: String = "",            // comune (local authorities only)
    val f: List<String> = emptyList(),  // profession, education and discipline keys (estimates)
    val n: Int? = null,            // posts
    val o: String = "",            // opens, Rome time
    val s: String = "",            // closes, Rome time; "" if unknown
    val m: String = "",            // last modified, UTC
    val u: String = "",            // page path under Feed.base
    val x: String = "",            // official page (inPA or Gazzetta Ufficiale)
    val k: String = "",            // kind: "Concorso", "Mobilità", ... ("" if unknown; absent before 2026-10-27)
    val w: String = "",            // the job alone, e.g. "Istruttore amministrativo" ("" if not recognised)
)

/** A saved search that notifies when new calls match it. */
@Serializable
data class Alert(
    val id: Long,
    val filter: Filter,
    val since: String,             // calls with Call.m after this are new
    val enabled: Boolean = true,
)

@Serializable
data class Filter(
    val words: String = "",
    val region: String = "",
    val province: String = "",
    val profile: String = "",      // a profession key of Feed.profiles (v0.1 could also hold an education key)
    val education: String = "",    // "laureati", "diplomati" or "scuola-obbligo"
    val within: Int = 0,           // deadline within this many days (0 = any)
) {
    val isEmpty get() = words.isBlank() && region.isEmpty() && province.isEmpty() && profile.isEmpty() &&
        education.isEmpty() && within == 0
}

/** What the phone keeps: saved calls (with a copy, so they show even after they leave the feed) and alerts. */
@Serializable
data class UserData(
    val saved: Map<String, Call> = emptyMap(),
    val alerts: List<Alert> = emptyList(),
    val notified: Set<String> = emptySet(),  // "id:7" / "id:1": deadline reminders already sent
    val status: Map<String, String> = emptyMap(),  // call id -> personal status (see STATUSES)
    val lastOpen: String = "",     // UTC instant of the last app open, to mark new calls
    val onboarded: Boolean = false,
)

/** Personal statuses a saved call can have; kept on the phone only. */
val STATUSES = listOf("Da preparare", "Domanda inviata")

/** Education keys of Feed.profiles; the others are professions. */
val EDUCATION = listOf("laureati", "diplomati", "scuola-obbligo")
