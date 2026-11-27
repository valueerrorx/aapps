// sleepr fuer iOS: Compose-Oberflaeche wie die Android-Views, Signalverarbeitung (domain/Dsp.kt) aus der Android-App
// Die Android-App bleibt bei Views ohne Compose (SPEC.md) und haengt nicht an diesem Modul.
plugins {
    alias(libs.plugins.aapps.kmp)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "AappsKit"
            isStatic = true
            // StoreBridge/StoreListener ohne Praefix fuer Swift
            export(project(":core:store"))
        }
    }
    sourceSets {
        commonMain {
            // eine Quelle fuer die Rauschfarben: Dsp.kt hat keine Android-Imports
            kotlin.srcDir("../src/main/kotlin/solutions/xapient/sleepr/domain")
            dependencies {
                api(project(":core:ui"))
            }
        }
        iosMain.dependencies {
            api(project(":core:store"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.resources {
    packageOfResClass = "solutions.xapient.sleepr.res"
    // Ressourcen-Bibliothek kommt ueber core/ui
    generateResClass = always
}
