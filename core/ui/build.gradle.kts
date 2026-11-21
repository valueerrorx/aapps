plugins {
    alias(libs.plugins.aapps.lib)
}

dependencies {
    api(libs.compose.ui)
    api(libs.compose.foundation)
    api(libs.androidx.activity.compose)
    api(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
}
