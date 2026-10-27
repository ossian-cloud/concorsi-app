package cloud.ossian.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DeadlineTest {
    private val now = LocalDateTime.of(2026, 10, 27, 10, 0)

    @Test fun badgeText() {
        assertEquals(Deadline("Scade oggi", Urgency.Urgent), deadline("2026-10-27T23:59", now))
        assertEquals(Deadline("Scade domani", Urgency.Urgent), deadline("2026-10-28T12:00", now))
        assertEquals(Deadline("Scade tra 3 giorni", Urgency.Urgent), deadline("2026-10-30T23:59", now))
        assertEquals(Deadline("Scade tra 4 giorni", Urgency.Soon), deadline("2026-10-31T00:01", now))
        assertEquals(Deadline("Scade tra 7 giorni", Urgency.Soon), deadline("2026-11-03T23:59", now))
        assertEquals(Deadline("Scade tra 8 giorni", Urgency.Normal), deadline("2026-11-04T23:59", now))
        assertEquals(Deadline("Scadenza non indicata", Urgency.Unknown), deadline("", now))
        assertEquals(Urgency.Expired, deadline("2026-10-27T09:00", now).urgency)
        assertEquals("Scaduto il 20/10/2026", deadline("2026-10-20T23:59", now).text)
    }

    @Test fun daysLeftCountsCalendarDays() {
        val today = LocalDate.of(2026, 10, 27)
        assertEquals(0, daysLeft("2026-10-27T00:01", today))
        assertEquals(5, daysLeft("2026-11-01T00:01", today))  // across the DST change
        assertEquals(null, daysLeft("", today))
    }

    @Test fun filterMatching() {
        val today = LocalDate.of(2026, 10, 27)
        val c = Call(id = "1", t = "Concorso: istruttore", e = "Comune di Orsenigo", r = listOf("lombardia"), p = "como",
            c = "Orsenigo", f = listOf("amministrativi", "diplomati"), s = "2026-11-02T23:59", w = "Istruttore amministrativo")
        assertTrue(Filter(region = "lombardia", province = "como", profile = "amministrativi", education = "diplomati").matches(c, today = today))
        assertFalse(Filter(province = "milano").matches(c, today = today))
        assertFalse(Filter(education = "laureati").matches(c, today = today))
        assertTrue(Filter(within = 7).matches(c, today = today))
        assertFalse(Filter(within = 7).matches(c.copy(s = "2026-11-30T23:59"), today = today))
        assertFalse(Filter(within = 30).matches(c.copy(s = ""), today = today))  // unknown deadline: not "within"
        assertTrue(Filter(words = "istruttori amministrativi").matches(c, today = today))  // w is searchable, stemmed
        assertTrue(Filter().isEmpty); assertFalse(Filter(within = 7).isEmpty)
    }

    @Test fun headlinesAndCase() {
        val c = Call(id = "1", t = "Concorso: n. 2 posti di funzionario", k = "Concorso")
        assertEquals("N. 2 posti di funzionario", c.headline())
        assertEquals("Istruttore", c.copy(w = "Istruttore").headline())
        assertEquals("Stabilizzazione: convertito", Call(id = "2", t = "Stabilizzazione: convertito").headline())  // no kind: keep
        assertEquals("Bando di concorso per n. 1 posto", sentenceCase("BANDO DI CONCORSO PER N. 1 POSTO"))
        assertEquals("Già in Minuscolo e Maiuscolo", sentenceCase("Già in Minuscolo e Maiuscolo"))
        assertEquals("inPA", sourceName("https://www.inpa.gov.it/x"))
        assertEquals("Gazzetta Ufficiale", sourceName("https://www.gazzettaufficiale.it/x"))
    }

    @Test fun oldUserDataStillReads() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; explicitNulls = false }
        val v01 = """{"saved":{"a":{"id":"a","t":"x"}},"alerts":[{"id":1,"filter":{"region":"lazio"},"since":"2026-10-01"}],"notified":[]}"""
        val u = json.decodeFromString<UserData>(v01)
        assertTrue(u.alerts[0].enabled)
        assertEquals("", u.saved["a"]!!.k)
        assertFalse(u.onboarded)
    }
}
