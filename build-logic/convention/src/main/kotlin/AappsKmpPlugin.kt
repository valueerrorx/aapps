import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// Gemeinsamer Code fuer Android und iOS: KMP-Bibliothek mit Compose Multiplatform
class AappsKmpPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("org.jetbrains.compose")

        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
        kotlin.targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
            // :core:ui -> solutions.xapient.aapps.core.ui, :apps:levlr:shared -> solutions.xapient.levlr.shared
            namespace = if (path.startsWith(":apps:")) "solutions.xapient." + path.removePrefix(":apps:").replace(':', '.')
            else "solutions.xapient.aapps" + path.replace(':', '.')
            compileSdk { version = release(Sdk.COMPILE) { minorApiLevel = Sdk.COMPILE_MINOR } }
            minSdk = Sdk.MIN
            // Android-R (core/ui) und Compose-Ressourcen im APK
            androidResources.enable = true
            compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
            // commonTest laeuft auf der JVM mit (androidHostTest)
            withHostTest { isIncludeAndroidResources = true }
        }
        // expect/actual-Klassen (Haptics) ohne Beta-Warnung
        kotlin.compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
        // Kotlin/Native fuer iOS; gebaut wird nur am Mac, am NAS nur Metadaten/klibs
        kotlin.iosArm64()
        kotlin.iosSimulatorArm64()
        // Android: androidx-Compose-Versionen wie in den Apps (BOM)
        val bom = libs.findLibrary("compose-bom").get()
        kotlin.sourceSets.getByName("androidMain").dependencies { implementation(dependencies.platform(bom)) }

        tasks.withType<Test>().configureEach {
            maxParallelForks = 1
            maxHeapSize = "1g"
            jvmArgs("-XX:+UseSerialGC")
        }
    }
}
