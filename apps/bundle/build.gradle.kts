plugins {
    alias(libs.plugins.aapps.app)
    alias(libs.plugins.roborazzi)
}

android {
    // hasSigningCertificate der Einzel-Apps braucht Android 9, darunter gilt das Bundle nie
    defaultConfig.minSdk = 28
}

dependencies {
    implementation(project(":core:ui"))
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.compose.ui.test.junit4)
}
