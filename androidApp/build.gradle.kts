plugins {
    kotlin("android")
    id("com.android.application")
}

android {
    namespace = "com.selfguide.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.selfguide.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        
        buildConfigField("String", "SUPABASE_URL", System.getenv("SUPABASE_URL")?.let { "\"$it\"" } ?: "\"https://YOUR_PROJECT_REF.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", System.getenv("SUPABASE_ANON_KEY")?.let { "\"$it\"" } ?: "\"your_anon_key\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", System.getenv("GOOGLE_WEB_CLIENT_ID")?.let { "\"$it\"" } ?: "\"your_web_client_id\"")
    }
    
    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    androidTarget()
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
}
