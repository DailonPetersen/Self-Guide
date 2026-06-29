package com.selfguide.network

actual class SupabaseConfig {
    actual val supabaseUrl: String = com.selfguide.android.BuildConfig.SUPABASE_URL
    actual val supabaseAnonKey: String = com.selfguide.android.BuildConfig.SUPABASE_ANON_KEY
    actual val googleWebClientId: String = com.selfguide.android.BuildConfig.GOOGLE_WEB_CLIENT_ID
}