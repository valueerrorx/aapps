plugins {
    alias(libs.plugins.aapps.app)
    alias(libs.plugins.roborazzi)
}

// Begruendung je Berechtigung: SPEC.md, Abschnitt Berechtigungen
aapps {
    allowedPermissions.addAll(
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.WAKE_LOCK",
        "android.permission.VIBRATE",
    )
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
