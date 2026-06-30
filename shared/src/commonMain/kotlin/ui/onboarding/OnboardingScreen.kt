package com.selfguide.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class OnboardingPage(
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        title = "Guia de Bolso Automatizado",
        description = "Explore a cidade com um guia no seu ouvido. Áudios históricos que disparam automaticamente quando você se aproxima de pontos históricos."
    ),
    OnboardingPage(
        title = "Caminhe e Ouça",
        description = "Caminhe normalmente. Ao se aproximar de um ponto histórico, você recebe uma notificação com a história daquele lugar."
    ),
    OnboardingPage(
        title = "Acesso Livre e Premium",
        description = "Acesse 10 pontos históricos gratuitamente em qualquer cidade. Desbloqueie o acesso total com um único pagamento."
    )
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        PageIndicator(
            totalPages = onboardingPages.size,
            currentPage = currentPage,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        PagerContent(
            page = onboardingPages[currentPage],
            modifier = Modifier.weight(1f)
        )
        
        Button(
            onClick = {
                if (currentPage < onboardingPages.size - 1) {
                    currentPage++
                } else {
                    onFinished()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (currentPage < onboardingPages.size - 1) "Próximo" else "Começar"
            )
        }
    }
}

@Composable
private fun PageIndicator(
    totalPages: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(totalPages) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .padding(2.dp)
            ) {
                if (index == currentPage) {
                    Text("●", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("○", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun PagerContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}