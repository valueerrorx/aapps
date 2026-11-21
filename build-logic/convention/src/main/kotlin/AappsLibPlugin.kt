import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AappsLibPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        extensions.configure<LibraryExtension> {
            configureAndroidCommon(this)
            // :core:ui -> com.valueerrorx.aapps.core.ui
            namespace = "com.valueerrorx.aapps" + path.replace(':', '.')
        }
    }
}
