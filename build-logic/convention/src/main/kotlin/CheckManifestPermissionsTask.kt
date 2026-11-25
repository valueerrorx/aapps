import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.xml.parsers.DocumentBuilderFactory

// Prueft das gemergte Manifest gegen die Allowlist der App
@CacheableTask
abstract class CheckManifestPermissionsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:Input
    abstract val applicationId: Property<String>

    @get:Input
    abstract val allowed: SetProperty<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun run() {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val doc = factory.newDocumentBuilder().parse(mergedManifest.get().asFile)
        val ns = "http://schemas.android.com/apk/res/android"
        val found = sortedSetOf<String>()
        for (tag in listOf("uses-permission", "uses-permission-sdk-23")) {
            val nodes = doc.getElementsByTagName(tag)
            for (i in 0 until nodes.length) {
                val el = nodes.item(i) as org.w3c.dom.Element
                found += el.getAttributeNS(ns, "name")
            }
        }
        // androidx.core legt diese app-interne Signatur-Berechtigung an
        val internal = "${applicationId.get()}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        val bad = found.filter { it != internal && it !in CORE && it !in allowed.get() }
        report.get().asFile.writeText(found.joinToString("\n", postfix = "\n"))
        if ("android.permission.INTERNET" in found) fail("INTERNET im Manifest von ${applicationId.get()} – verboten")
        if (bad.isNotEmpty()) fail("Nicht freigegebene Berechtigungen in ${applicationId.get()}: ${bad.joinToString()}")
    }

    private companion object {
        // per Manifest-Merge: VIBRATE aus core/ui (Haptics), BILLING aus core/billing (Play Billing)
        val CORE = setOf("android.permission.VIBRATE", "com.android.vending.BILLING")
    }
}
