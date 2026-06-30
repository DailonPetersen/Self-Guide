package com.selfguide.di

import com.selfguide.auth.AuthRepository
import com.selfguide.audio.AudioPlayerService
import com.selfguide.network.SupabaseAuthRepository
import com.selfguide.network.SupabaseProvider
import com.selfguide.network.SupabaseConfig
import com.selfguide.security.SecureStorage
import org.koin.core.context.startKoin
import org.koin.dsl.module

expect fun initKoin()

val authModule = module {
    single { SupabaseConfig() }
    single { SecureStorage() }
    single { SupabaseProvider(get(), get()) }
    single<AuthRepository> { SupabaseAuthRepository(get()) }
}

val audioModule = module {
    single { AudioPlayerService() }
}