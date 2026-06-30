package com.selfguide.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ModoDegradadoScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        BannerLocalizacao(onOpenSettings = onOpenSettings)
        PesquisaManualContent()
    }
}

@Composable
private fun BannerLocalizacao(onOpenSettings: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Ative a localização para receber notificações de áudio automáticas enquanto caminha.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onOpenSettings) {
                Text("Ativar localização")
            }
        }
    }
}

@Composable
private fun PesquisaManualContent() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Modo Degradado",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "Você pode pesquisar cidades manualmente para explorar pontos turísticos sem geofences automáticos.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}