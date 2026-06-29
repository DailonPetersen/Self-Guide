plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("app.cash.sqldelight")
}

kotlin {
    androidTarget()

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
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            implementation("io.ktor:ktor-client-core:2.3.12")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
            implementation("io.insert-koin:koin-core:3.5.6")
            implementation("app.cash.sqldelight:runtime:2.0.2")
            implementation("io.github.jan-tieb:supabase-auth-kt:2.0.0")
            implementation("io.github.jan-tieb:supabase-postgrest-kt:2.0.0")
        }

        androidMain.dependencies {
            implementation("io.ktor:ktor-client-android:2.3.12")
            implementation("io.insert-koin:koin-android:3.5.6")
            implementation("app.cash.sqldelight:sqlite-jvm:2.0.2")
            implementation("androidx.security:security-crypto:1.1.0-alpha06")
            implementation("com.google.android.gms:play-services-auth:21.3.0")
        }

        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:2.3.12")
            implementation("app.cash.sqldelight:native-worker:2.0.2")
            implementation("app.cash.sqldelight:sqlite-native-driver-inspector:2.0.2")
        }
    }
}

android {
    namespace = "com.selfguide.shared"
    compileSdk = 35
}

sqldelight {
    databases {
        create("LocalDatabase") {
            packageName.set("com.selfguide.database")
            srcDir.set("src/commonMain/sqldelight")
        }
    }
}