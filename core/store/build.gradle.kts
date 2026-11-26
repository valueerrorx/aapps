// App-Store-Testphase + Kauf (StoreKit 2 per Swift-Bridge); Logik gemeinsam, Tests auf der JVM
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.withType<Test>().configureEach {
    maxHeapSize = "512m"
    jvmArgs("-XX:+UseSerialGC")
}
