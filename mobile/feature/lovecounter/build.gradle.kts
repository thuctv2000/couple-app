plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
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
        commonMain.dependencies {
            implementation(libs.datetime)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            api(libs.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.auth)
            implementation(libs.coroutines.play.services)
        }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
        jvmMain.dependencies { implementation(libs.ktor.client.cio) }
        jvmTest.dependencies { implementation(libs.ktor.client.mock) }
    }
}

room { schemaDirectory("$projectDir/schemas") }
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
