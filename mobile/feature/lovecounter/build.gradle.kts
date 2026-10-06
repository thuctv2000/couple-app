plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp)
}
kotlin {
    android {
        namespace = "dev.coupleapp.lovecounter"
        compileSdk = 37
        buildToolsVersion = "36.1.0"
        minSdk = 26
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain.dependencies { implementation(libs.datetime) }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
