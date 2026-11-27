// dividr: Logik und Oberflaeche gemeinsam fuer Android (:apps:dividr) und iOS (iosApp/dividr); Framework heisst in allen Apps AappsKit
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
        androidMain.dependencies {
            // QR erzeugen/lesen und Kamera nur auf Android; iOS: eigener Encoder + AVFoundation
            implementation(libs.zxing.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.androidx.core.ktx)
        }
        iosMain.dependencies {
            api(project(":core:store"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
        }
    }
}

compose.resources {
    packageOfResClass = "solutions.xapient.dividr.res"
    // Ressourcen-Bibliothek kommt ueber core/ui
    generateResClass = always
}
