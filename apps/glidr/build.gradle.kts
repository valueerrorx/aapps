plugins {
    alias(libs.plugins.aapps.app)
    alias(libs.plugins.roborazzi)
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.webkit)
    // kein Compose-UI; Runtime nur, weil das Convention-Plugin den Compose-Compiler aktiviert
    implementation(libs.compose.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
}
