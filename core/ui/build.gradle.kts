plugins {
    alias(libs.plugins.aapps.lib)
}

dependencies {
    api(libs.compose.ui)
    api(libs.compose.foundation)
    api(libs.androidx.activity.compose)
    api(libs.androidx.lifecycle.runtime.compose)
    // Testphase + Kauf (TrialGated); Apps ohne Billing (Spiele) nutzen core/ui nicht
    api(project(":core:billing"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
}
