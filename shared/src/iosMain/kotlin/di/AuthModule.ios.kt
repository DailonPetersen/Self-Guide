package com.selfguide.di

import com.selfguide.auth.AuthRepository
import com.selfguide.network.SupabaseAuthRepository
import com.selfguide.network.SupabaseProvider
import com.selfguide.network.SupabaseConfig
import com.selfguide.security.SecureStorage
import org.koin.core.context.startKoin
import org.koin.dsl.module

val authModule = module {
    single { SupabaseConfig() }
    single { SecureStorage() }
    single { SupabaseProvider(get(), get()) }
    single<AuthRepository> { SupabaseAuthRepository(get()) }
}

@ObjCName("InitKoin")
fun initKoin() {
    startKoin {
        modules(authModule)
    }
}