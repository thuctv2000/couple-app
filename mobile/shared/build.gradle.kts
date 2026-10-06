plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp)
}
kotlin {
    android {
        namespace = "dev.coupleapp.shared"
        compileSdk = 37
        buildToolsVersion = "36.1.0"
        minSdk = 26
    }
    jvm()
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "CoupleShared"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":feature:lovecounter"))
            implementation(libs.datetime)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
