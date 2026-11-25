plugins {
    alias(libs.plugins.aapps.app)
    alias(libs.plugins.roborazzi)
}

aapps {
    allowedPermissions.add("android.permission.CAMERA")
}

dependencies {
    implementation(project(":core:ui"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.compose.ui.test.junit4)
}
