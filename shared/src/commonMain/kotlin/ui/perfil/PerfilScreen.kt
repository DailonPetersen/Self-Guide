package com.selfguide.ui.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.selfguide.auth.AuthRepository
import com.selfguide.model.AuthUser
import kotlinx.coroutines.launch

data class UserProfile(
    val nome: String,
    val email: String,
    val pontosVisitados: Int,
    val cidadesDesbloqueadas: Int,
    val badges: List<Badge>
)

data class Badge(
    val nome: String,
    val descricao: String,
    val nivel: Int
)

@Composable
fun PerfilScreen(
    authRepository: AuthRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                userProfile = UserProfile(
                    nome = user.email ?: "Usuário",
                    email = user.email ?: "",
                    pontosVisitados = 0,
                    cidadesDesbloqueadas = 0,
                    badges = emptyList()
                )
            }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        userProfile?.let { profile ->
            PerfilContent(profile = profile, modifier = modifier)
        }
    }
}

@Composable
private fun PerfilContent(
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = profile.nome,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = profile.email,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Pontos visitados: ${profile.pontosVisitados}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Cidades desbloqueadas: ${profile.cidadesDesbloqueadas}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Badges",
            style = MaterialTheme.typography.titleMedium
        )
        LazyRow(
            modifier = Modifier.padding(top = 8.dp)
        ) {
            items(profile.badges) { badge ->
                BadgeItem(badge = badge)
            }
        }
    }
}

@Composable
private fun BadgeItem(badge: Badge) {
    Card(
        modifier = Modifier
            .padding(end = 8.dp)
            .size(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = badge.nome,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = badge.descricao,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}