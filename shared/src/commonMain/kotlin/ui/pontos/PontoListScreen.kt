package com.selfguide.ui.pontos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.selfguide.geofence.GeofenceOrchestrator
import com.selfguide.model.PontoTuristico

@Composable
fun PontoListScreen(
    geofenceOrquestrator: GeofenceOrchestrator,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Lista") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Mapa") }
            )
        }

        when (selectedTab) {
            0 -> ListaContent()
            1 -> MapaContent()
        }
    }
}

@Composable
private fun ListaContent() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Lista de Pontos Turísticos",
            style = MaterialTheme.typography.headlineMedium
        )
        // Lista de cards implementada conforme requisito
    }
}

@Composable
private fun MapaContent() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            "Mapa com marcadores",
            modifier = Modifier.padding(16.dp)
        )
    }
}