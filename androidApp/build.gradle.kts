plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
