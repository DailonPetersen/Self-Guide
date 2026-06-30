package com.selfguide.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SelecaoLocalizacaoScreen(
    onUseCurrentLocation: () -> Unit,
    onSelectCity: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPermissionDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Como deseja explorar?",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            onClick = { showPermissionDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Usar minha localização atual", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Usamos sua localização para disparar áudios automáticos quando você se aproxima de pontos históricos, mesmo com o app fechado.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            onClick = onSelectCity,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Escolher uma cidade", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Digite e selecione uma cidade manualmente usando nossa busca.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("Permissão de Localização") },
                text = { Text("Usamos sua localização para disparar áudios automáticos quando você se aproxima de pontos históricos, mesmo com o app fechado.") },
                confirmButton = {
                    TextButton(onClick = {
                        showPermissionDialog = false
                        onUseCurrentLocation()
                    }) {
                        Text("Entendi")
                    }
                }
            )
        }
    }
}