// hertzr: Logik und Oberflaeche gemeinsam fuer Android (:apps:hertzr) und iOS (iosApp/hertzr); Framework heisst in allen Apps AappsKit
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
        commonMain.dependencies {
            api(project(":core:ui"))
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
    packageOfResClass = "solutions.xapient.hertzr.res"
    // Ressourcen-Bibliothek kommt ueber core/ui
    generateResClass = always
}
