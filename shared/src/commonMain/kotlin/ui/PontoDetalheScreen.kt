package com.selfguide.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.selfguide.audio.AudioPlayerService
import com.selfguide.audio.AudioState
import com.selfguide.audio.getAudioFilePath
import com.selfguide.model.PontoTuristico
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PontoDetalheScreen(
    ponto: PontoTuristico,
    preferenciaRapida: Boolean = true,
    audioPlayer: AudioPlayerService,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val audioUrl by derivedStateOf {
        if (preferenciaRapida) ponto.audioUrlRapido else ponto.audioUrlCompleto
    }
    
    var audioState by remember { mutableStateOf(AudioState.Idle) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var sliderPosition by remember { mutableStateOf(0f) }
    
    LaunchedEffect(audioPlayer) {
        audioPlayer.state.collectLatest { state ->
            audioState = state
        }
    }
    
    LaunchedEffect(audioPlayer) {
        audioPlayer.progress.collectLatest { progress ->
            currentPosition = progress
            if (duration > 0) {
                sliderPosition = (progress.toFloat() / duration.toFloat())
            }
        }
    }
    
    LaunchedEffect(audioState) {
        if (audioState == AudioState.Playing) {
            duration = audioPlayer.getDuration()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = ponto.nome,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        ponto.tituloLocal?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        ponto.roteiroRapido?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AudioPlayerSection(
            audioUrl = audioUrl,
            audioState = audioState,
            currentPosition = currentPosition,
            duration = duration,
            sliderPosition = sliderPosition,
            onDownloadClick = {
                coroutineScope.launch {
                    audioUrl?.let { url ->
                        audioPlayer.downloadAudio(url, ponto.id)
                    }
                }
            },
            onPlayClick = {
                coroutineScope.launch {
                    audioUrl?.let { url ->
                        val fileName = "${ponto.id}.mp3"
                        when (audioState) {
                            AudioState.Idle, AudioState.Ready, AudioState.Paused -> {
                                audioPlayer.play(getAudioFilePath(ponto.id))
                            }
                            AudioState.Playing -> {
                                audioPlayer.pause()
                            }
                            AudioState.Downloading -> { }
                            AudioState.Error -> { }
                        }
                    }
                }
            },
            onPauseClick = {
                audioPlayer.pause()
            },
            onStopClick = {
                audioPlayer.stop()
            }
        )
    }
}

@Composable
private fun AudioPlayerSection(
    audioUrl: String?,
    audioState: AudioState,
    currentPosition: Long,
    duration: Long,
    sliderPosition: Float,
    onDownloadClick: () -> Unit,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onStopClick: () -> Unit
) {
    if (audioUrl == null) return
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Áudio do Ponto Turístico",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            when (audioState) {
                AudioState.Idle -> {
                    Button(
                        onClick = onDownloadClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Baixar e Reproduzir Áudio")
                    }
                }
                AudioState.Downloading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Baixando áudio...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                AudioState.Ready -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onPlayClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reproduzir")
                        }
                    }
                }
                AudioState.Playing -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = sliderPosition,
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            enabled = false
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = formatTime(duration),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onPauseClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Pausar")
                            }
                            OutlinedButton(
                                onClick = onStopClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Parar")
                            }
                        }
                    }
                }
                AudioState.Paused -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onPlayClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Continuar")
                        }
                        OutlinedButton(
                            onClick = onStopClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Parar")
                        }
                    }
                }
                AudioState.Error -> {
                    Text(
                        text = "Erro ao carregar áudio",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Button(
                        onClick = onDownloadClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tentar Novamente")
                    }
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000).toInt()
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}