package com.selfguide.audio

import kotlin.native.concurrent.ThreadLocal
import kotlinx.coroutines.flow.Flow

enum class AudioState {
    Idle,
    Downloading,
    Ready,
    Playing,
    Paused,
    Error
}

@ThreadLocal
expect class AudioPlayerService {
    val state: Flow<AudioState>
    val progress: Flow<Long>
    
    suspend fun downloadAudio(url: String, fileName: String): Result<Unit>
    
    suspend fun play(filePath: String): Result<Unit>
    
    fun pause()
    
    fun stop()
    
    fun release()
    
    suspend fun getCurrentPosition(): Long
    
    suspend fun getDuration(): Long
}