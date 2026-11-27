package solutions.xapient.bundle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
class CatalogTest {
    // Arbeitsverzeichnis der Tests = apps/bundle
    private val settingsApps = File("../../settings.gradle.kts").readLines()
        .mapNotNull { Regex("""^include\(":apps:([a-z0-9]+)"\)$""").find(it.trim())?.groupValues?.get(1) }
        .filter { it != "bundle" }
        .toSet()

    @Test fun catalogMatchesAllAppsInRepo() {
        assertTrue(settingsApps.size >= 18)
        assertEquals(settingsApps, Catalog.map { it.id }.toSet())
        assertEquals(Catalog.size, Catalog.map { it.id }.toSet().size)
    }

    @Test fun manifestQueriesEveryApp() {
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))
        val nodes = doc.getElementsByTagName("package")
        val queried = (0 until nodes.length).map {
            (nodes.item(it) as org.w3c.dom.Element).getAttributeNS("http://schemas.android.com/apk/res/android", "name")
        }.toSet()
        assertEquals(Catalog.map { it.pkg }.toSet(), queried)
    }

    @Test fun onlyGamesAreFree() = assertEquals(setOf("moovr", "glidr"), Catalog.filter { it.free }.map { it.id }.toSet())

    // Hinweis im Dankes-Screen nennt die Zahl der Bezahl-Apps
    @Test fun thanksHintCountMatchesPaidApps() {
        val paid = Catalog.count { !it.free }.toString()
        val hint = RuntimeEnvironment.getApplication().getString(solutions.xapient.aapps.core.billing.R.string.billing_bundle)
        assertTrue(hint, Regex("\\b$paid\\b").containsMatchIn(hint))
    }

    @Test fun nothingInstalledInTest() = assertEquals(emptySet<String>(), installedApps(RuntimeEnvironment.getApplication()))
}
