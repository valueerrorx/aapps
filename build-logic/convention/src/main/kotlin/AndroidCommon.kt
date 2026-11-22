import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

internal object Sdk {
    // Compose 1.13 verlangt compileSdk >= 37.1
    const val COMPILE = 37
    const val COMPILE_MINOR = 2
    const val TARGET = 36
    const val MIN = 26
}

internal val Project.libs get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

// Gemeinsame Einstellungen fuer App- und Library-Module
internal fun Project.configureAndroidCommon(android: CommonExtension) {
    android.apply {
        compileSdk = Sdk.COMPILE
        compileSdkMinor = Sdk.COMPILE_MINOR
        defaultConfig.minSdk = Sdk.MIN
        compileOptions.sourceCompatibility = JavaVersion.VERSION_17
        compileOptions.targetCompatibility = JavaVersion.VERSION_17
        buildFeatures.compose = true
        lint.apply {
            abortOnError = true
            warningsAsErrors = false
            checkReleaseBuilds = false
            // Versions-Hinweise sind keine Fehler, Updates laufen bewusst
            disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
        }
        testOptions.unitTests.isIncludeAndroidResources = true
    }
    tasks.withType<Test>().configureEach {
        // ein Fork, kleiner Heap, Serial-GC: am NAS ist RAM knapp, Swap bremst sonst massiv
        maxParallelForks = 1
        maxHeapSize = "1g"
        jvmArgs("-XX:+UseSerialGC")
        systemProperty("robolectric.pixelCopyRenderMode", "hardware")
    }
    val bom = libs.findLibrary("compose-bom").get()
    dependencies.add("implementation", dependencies.platform(bom))
    dependencies.add("testImplementation", dependencies.platform(bom))
}
