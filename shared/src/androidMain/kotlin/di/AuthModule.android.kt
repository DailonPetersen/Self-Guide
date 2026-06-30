package com.selfguide.di

import android.content.Context
import com.selfguide.audio.AudioPlayerService
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

val audioModule = module {
    single { AudioPlayerService(get()) }
}


fun initKoin() {
    startKoin {
        modules(authModule, audioModule)
    }
}

object AuthEventNotifier {
    private var googleSignInCallback: ((android.content.Intent?) -> Unit)? = null

    fun setGoogleSignInCallback(callback: (android.content.Intent?) -> Unit) {
        googleSignInCallback = callback
    }

    fun notifyGoogleSignInResult(data: android.content.Intent?) {
        googleSignInCallback?.invoke(data)
    }
}
