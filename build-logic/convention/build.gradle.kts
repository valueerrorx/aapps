plugins {
    `kotlin-dsl`
}

dependencies {
    // AGP liegt zur Laufzeit ueber das Root-Projekt am Classpath
    compileOnly(libs.android.gradle.api)
    // KMP + Compose Multiplatform fuer aapps.kmp (iOS-Port)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("aappsApp") {
            id = "aapps.app"
            implementationClass = "AappsAppPlugin"
        }
        register("aappsLib") {
            id = "aapps.lib"
            implementationClass = "AappsLibPlugin"
        }
        register("aappsKmp") {
            id = "aapps.kmp"
            implementationClass = "AappsKmpPlugin"
        }
    }
}
