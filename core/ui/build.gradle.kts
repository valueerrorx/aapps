plugins {
    alias(libs.plugins.aapps.kmp)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.cmp.runtime)
            api(libs.cmp.ui)
            api(libs.cmp.foundation)
            api(libs.cmp.resources)
            api(libs.jb.lifecycle.runtime.compose)
        }
        androidMain.dependencies {
            api(libs.compose.ui)
            api(libs.compose.foundation)
            api(libs.androidx.activity.compose)
            api(libs.androidx.lifecycle.runtime.compose)
            // Testphase + Kauf (TrialGated); Apps ohne Billing (Spiele) nutzen core/ui nicht
            api(project(":core:billing"))
            implementation(libs.androidx.core.ktx)
        }
        iosMain.dependencies {
            // App-Store-Testphase (StoreGated)
            api(project(":core:store"))
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
        }
    }
}

// Eine Quelle fuer Texte und Schriften: Android-R fuer die Apps, Compose-Res fuer gemeinsamen Code
compose.resources {
    publicResClass = true
    packageOfResClass = "solutions.xapient.aapps.core.ui"
    customDirectory("commonMain", provider { layout.projectDirectory.dir("src/androidMain/res") })
}
