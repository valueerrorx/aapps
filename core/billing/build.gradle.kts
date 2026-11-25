plugins {
    alias(libs.plugins.aapps.lib)
}

dependencies {
    // ohne Compose, damit sleepr (Views) es nutzen kann; Runtime nur fuer den Compose-Compiler des Convention-Plugins
    compileOnly(libs.compose.runtime)
    testCompileOnly(libs.compose.runtime)
    implementation(libs.billing)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
