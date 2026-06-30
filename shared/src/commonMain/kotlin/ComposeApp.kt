package com.selfguide.shared

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import android.content.Intent

@Composable
fun ComposeApp(onSignInClick: (Intent) -> Unit) {
    MaterialTheme {
        // Tela principal do app
        // Para usar o player: val audioPlayer = remember { AudioPlayerProvider().audioPlayer }
    }
}
