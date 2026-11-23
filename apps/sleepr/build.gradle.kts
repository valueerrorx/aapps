plugins {
    alias(libs.plugins.aapps.app)
    alias(libs.plugins.roborazzi)
}

// Begruendung je Berechtigung: SPEC.md, Abschnitt Berechtigungen
aapps {
    allowedPermissions.addAll(
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.WAKE_LOCK",
    )
}

dependencies {
    // Views/Canvas statt Compose; Runtime nur fuer den Compose-Compiler des Convention-Plugins, nicht in der APK
    compileOnly(libs.compose.runtime)
    testCompileOnly(libs.compose.runtime)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
}
