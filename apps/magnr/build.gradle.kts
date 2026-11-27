plugins {
    alias(libs.plugins.aapps.app)
    alias(libs.plugins.roborazzi)
}

// Keine eigenen Berechtigungen: VIBRATE kommt aus core/ui, Ton ueber AudioTrack (SPEC.md)

dependencies {
    implementation(project(":apps:magnr:shared"))
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.compose.ui.test.junit4)
}
