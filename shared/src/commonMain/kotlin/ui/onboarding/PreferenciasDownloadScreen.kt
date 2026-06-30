package com.selfguide.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class DownloadMode {
    ECONOMICO, OFFLINE
}

@Composable
fun PreferenciasDownloadScreen(
    onModeSelected: (DownloadMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(DownloadMode.ECONOMICO) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Preferências de Download",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            onClick = { selectedMode = DownloadMode.ECONOMICO },
            enabled = selectedMode == DownloadMode.ECONOMICO,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Modo Econômico", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Consumir sob demanda. Baixa o áudio de cada ponto turístico somente quando você abrir a notificação correspondente. Recomendado para economizar dados móveis.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            onClick = { selectedMode = DownloadMode.OFFLINE },
            enabled = selectedMode == DownloadMode.OFFLINE,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Modo Offline", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Pré-download antecipado. Baixa previamente o áudio dos 20 pontos ativos da região para que você possa ouvir sem internet na rua. Consome mais armazenamento local.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { onModeSelected(selectedMode) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Começar")
        }
    }
}