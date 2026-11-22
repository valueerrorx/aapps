plugins {
    alias(libs.plugins.aapps.app)
    alias(libs.plugins.roborazzi)
}

// Begruendung: SPEC.md, Abschnitt Berechtigungen
aapps {
    allowedPermissions.add("android.permission.ACCESS_FINE_LOCATION")
    allowedPermissions.add("android.permission.ACCESS_COARSE_LOCATION")
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
