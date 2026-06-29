package com.selfguide.network

expect class SupabaseConfig {
    val supabaseUrl: String
    val supabaseAnonKey: String
    val googleWebClientId: String
}