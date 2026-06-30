package com.selfguide.ui.pontos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.selfguide.model.PontoTuristico
import com.selfguide.geofence.GeofenceOrchestrator

@Composable
fun PontoDetalheScreen(
    ponto: PontoTuristico,
    geofenceOrchestrator: GeofenceOrchestrator,
    onMarkConsumed: () -> Unit,
    onReportIssue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = ponto.tituloLocal ?: ponto.nome,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Distância: ${ponto.distanciaMetros}m",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = ponto.roteiroRapido ?: ponto.roteiroCompleto ?: "",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Horário: ${ponto.horariosFuncionamento}",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Preço: ${ponto.custoIngresso}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onMarkConsumed) {
            Text("Já consumi este ponto")
        }

        TextButton(onClick = onReportIssue) {
            Text("Reportar dado incorreto")
        }
    }
}