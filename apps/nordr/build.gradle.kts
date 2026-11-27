plugins {
    alias(libs.plugins.aapps.app)
    alias(libs.plugins.roborazzi)
}

dependencies {
    implementation(project(":apps:nordr:shared"))
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.compose.ui.test.junit4)
}
