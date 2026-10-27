package cloud.ossian.app.ui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Assume.assumeTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.runners.MethodSorters
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Screenshots of each screen on the JVM, to see the UI without a device:
 *  ./gradlew testDebugUnitTest -Dfeed=<v1.json> --tests '*ScreenshotTest*'  ->  app/build/screenshots/ */
@RunWith(RobolectricTestRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)  // the first-run test must see a fresh install
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h822dp-xxhdpi")
@OptIn(ExperimentalRoborazziApi::class)
class ScreenshotTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun shot(name: String) { compose.waitForIdle(); captureScreenRoboImage("build/screenshots/$name.png") }

    private fun ComposeTestRule.waitText(text: String) = try {
        waitUntil(30_000) { onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    } catch (e: Throwable) {
        val dir = ApplicationProvider.getApplicationContext<android.content.Context>().filesDir
        println("DEBUG roots=${onAllNodes(androidx.compose.ui.test.isRoot()).fetchSemanticsNodes().size} files=${dir.list()?.toList()} user=${runCatching { File(dir, "user.json").readText() }.getOrNull()}")
        captureScreenRoboImage("build/screenshots/debug-${text.take(12)}.png"); throw e
    }

    /** Installs the feed as the app's cache, and a user.json as an earlier version would have left it. */
    private fun install(user: String) {
        val feed = System.getProperty("feed").orEmpty()
        assumeTrue("needs -Dfeed=<v1.json>", feed.isNotEmpty())
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        File(feed).copyTo(File(ctx.filesDir, "concorsi-v1.json"), overwrite = true)
        File(ctx.filesDir, "user.json").writeText(user)
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx)
        System.setProperty("ossian.offline", "true")
    }

    @Test
    fun a_screens() {
        install("""{"lastOpen":"2026-10-26T00:00:00Z"}""")  // calls changed after this show "Nuovo"
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitText("Che lavoro?")
            shot("01-primo-avvio")
            compose.onNodeWithText("Salta").performClick()
            compose.waitText("concorsi aperti")
            shot("02-cerca")
            compose.onNodeWithText("Avvisi").performClick(); shot("14-avvisi-vuoto")
            compose.onNodeWithText("Cerca").performClick()
            compose.waitText("concorsi aperti")
            compose.onNodeWithText("Regione").performClick()
            compose.waitText("Tutta Italia")
            shot("03-filtro-regione")
            compose.onAllNodesWithText("Lombardia").onLast().performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Tutta Italia").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithText("Professione").performClick()
            compose.waitText("Tutte le professioni")
            compose.onAllNodesWithText("Amministrativi e contabili").onLast().performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Tutte le professioni").fetchSemanticsNodes().isEmpty() }
            shot("04-cerca-filtri")
            compose.onAllNodesWithContentDescription("Salva").onFirst().performClick()
            compose.waitText("Non ora")
            shot("05-permesso-notifiche")
            compose.onNodeWithText("Non ora").performClick()
            compose.onNodeWithText("Avvisami per questa ricerca").performClick()
            compose.waitText("Avviso attivo")
            compose.onAllNodesWithText("Scade ", substring = true).onFirst().performClick()
            compose.waitText("Apri il bando ufficiale")
            shot("06-dettaglio")
            compose.onNodeWithText("Testo completo del titolo").performClick()
            shot("07-dettaglio-titolo")
            compose.onNodeWithContentDescription("Indietro").performClick()
            compose.waitText("concorsi aperti")
            compose.onNodeWithText("Salvati").performClick()
            compose.onNodeWithContentDescription("Cambia stato").performClick()
            compose.waitText("Domanda inviata")
            compose.onNodeWithText("Da preparare").performClick()
            shot("08-salvati")
            compose.onNodeWithText("Avvisi").performClick(); shot("09-avvisi")
            compose.onNodeWithText("Info").performClick(); shot("10-info")
        }
    }

    @Test
    @Config(qualifiers = "+night")
    fun b_darkScreens() {
        install("""{"lastOpen":"2026-10-26T00:00:00Z","onboarded":true}""")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitText("concorsi aperti")
            shot("11-cerca-scuro")
            compose.onAllNodesWithText("Scade ", substring = true).onFirst().performClick()
            compose.waitText("Apri il bando ufficiale")
            shot("12-dettaglio-scuro")
            compose.onNodeWithContentDescription("Indietro").performClick()
            compose.waitText("concorsi aperti")
            compose.onNodeWithText("Regione").performClick()
            compose.waitText("Tutta Italia")
            shot("13-filtro-scuro")
        }
    }
}
