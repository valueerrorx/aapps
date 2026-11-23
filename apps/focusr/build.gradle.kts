plugins {
    alias(libs.plugins.aapps.app)
    alias(libs.plugins.roborazzi)
}

// Begruendung in SPEC.md: exaktes Ende im Doze, Benachrichtigung, Vibration bei gesperrtem Bildschirm
aapps {
    allowedPermissions.addAll(
        "android.permission.USE_EXACT_ALARM",
        "android.permission.SCHEDULE_EXACT_ALARM",
        "android.permission.POST_NOTIFICATIONS",
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
