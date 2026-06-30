plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.koin.core)
            implementation(libs.sqldelight.runtime)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.koin.android)
            implementation(libs.sqldelight.sqlite.jvm)
            implementation(libs.androidx.security.crypto)
            implementation(libs.google.play.services.auth)
            implementation(libs.koin.androidx.compose)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
            implementation("app.cash.sqldelight:sqlite-native-driver-inspector:2.0.2")
        }
    }
}

android {
    namespace = "com.selfguide.shared"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

sqldelight {
    databases {
        create("LocalDatabase") {
            packageName.set("com.selfguide.database")
            srcDirs.setFrom("src/commonMain/sqldelight")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.2")
        }
    }
}
