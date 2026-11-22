import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HasHostTestsBuilder
import com.android.build.api.variant.HostTestBuilder
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

// Erlaubte Berechtigungen pro App (Allowlist), Standard: keine
abstract class AappsExtension {
    abstract val allowedPermissions: SetProperty<String>
}

class AappsAppPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val appName = name
        val aapps = extensions.create<AappsExtension>("aapps")
        aapps.allowedPermissions.convention(emptySet())

        val version = file("version.txt").readText().trim()
        val parts = version.split(".").map { it.toInt() }
        require(parts.size == 3) { "version.txt muss X.Y.Z enthalten" }

        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(this)
            namespace = "solutions.xapient.$appName"
            defaultConfig {
                applicationId = "solutions.xapient.$appName"
                targetSdk = Sdk.TARGET
                versionName = version
                versionCode = parts[0] * 10000 + parts[1] * 100 + parts[2]
            }
            buildFeatures.buildConfig = true
            lint.checkDependencies = true

            // Signierung nur aus Umgebungsvariablen, sonst Debug-Schluessel
            val ks = providers.environmentVariable("KEYSTORE_FILE").orNull
            val ksPass = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
            val keyPass = providers.environmentVariable("KEY_PASSWORD_${appName.uppercase()}").orNull
            val haveKey = !ks.isNullOrBlank() && !ksPass.isNullOrBlank() && !keyPass.isNullOrBlank()
            // CI auf main/Tags: ohne echten Schluessel abbrechen, sonst wechselt die Signatur je Runner
            if (providers.gradleProperty("aapps.requireReleaseKey").orNull == "true" && !haveKey) {
                fail("Release-Schluessel fehlt: KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_PASSWORD_${appName.uppercase()}")
            }
            if (haveKey) {
                signingConfigs.create("release") {
                    storeFile = file(ks!!)
                    storePassword = ksPass
                    keyAlias = appName
                    keyPassword = keyPass
                }
            }
            buildTypes.getByName("release") {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                signingConfig = signingConfigs.getByName(if (haveKey) "release" else "debug")
            }
        }

        extensions.configure<ApplicationAndroidComponentsExtension> {
            // Unit-/Screenshot-Tests nur gegen Debug, spart die doppelte Laufzeit
            beforeVariants(selector().withBuildType("release")) { v ->
                (v as? HasHostTestsBuilder)?.hostTests?.get(HostTestBuilder.UNIT_TEST_TYPE)?.enable = false
            }
            onVariants { variant ->
                val cap = variant.name.replaceFirstChar { it.uppercase() }
                val checkTask = tasks.register<CheckManifestPermissionsTask>("check${cap}ManifestPermissions") {
                    group = "verification"
                    description = "Bricht ab bei INTERNET oder nicht freigegebenen Berechtigungen"
                    mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                    applicationId.set(variant.applicationId)
                    allowed.set(aapps.allowedPermissions)
                    report.set(layout.buildDirectory.file("aapps/${variant.name}-permissions.txt"))
                }
                tasks.matching { it.name == "assemble$cap" || it.name == "check" }.configureEach {
                    dependsOn(checkTask)
                }
            }
        }
    }
}

internal fun fail(msg: String): Nothing = throw GradleException(msg)
