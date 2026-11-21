plugins {
    `kotlin-dsl`
}

dependencies {
    // AGP liegt zur Laufzeit ueber das Root-Projekt am Classpath
    compileOnly(libs.android.gradle.api)
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
    }
}
