package com.selfguide.di

import com.selfguide.auth.AuthRepository
import com.selfguide.network.SupabaseAuthRepository
import com.selfguide.network.SupabaseProvider
import com.selfguide.network.SupabaseConfig
import com.selfguide.security.SecureStorage
import org.koin.core.context.startKoin
import org.koin.dsl.module

expect fun initKoin()