package com.selfguide.audio

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

actual class AudioPlayerService(private val context: Context) {
    private val _state = MutableStateFlow(AudioState.Idle)
    actual val state: Flow<AudioState> = _state
    
    private val _progress = MutableStateFlow(0L)
    actual val progress: Flow<Long> = _progress
    
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    
    init {
        createMediaPlayer()
    }
    
    private fun createMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener {
                _state.update { AudioState.Idle }
                stopProgressUpdates()
            }
            setOnErrorListener { _, _, _ ->
                _state.update { AudioState.Error }
                stopProgressUpdates()
                true
            }
        }
    }
    
    actual suspend fun downloadAudio(url: String, fileName: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            _state.update { AudioState.Downloading }
            
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            val downloadsDir = File(context.getExternalFilesDir(null), "audio")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Download em progresso...")
                .setDestinationUri(android.net.Uri.fromFile(File(downloadsDir, "$fileName.mp3")))
                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_HIDDEN)
            
            val downloadId = downloadManager.enqueue(request)
            
            val query = android.app.DownloadManager.Query().setFilterById(downloadId)
            
            continuation.invokeOnCancellation {
                downloadManager.remove(downloadId)
            }
            
            CoroutineScope(Dispatchers.IO).launch {
                var cursor: android.database.Cursor? = null
                var lastProgress = -1L
                
                while (continuation.isActive) {
                    cursor?.close()
                    cursor = downloadManager.query(query)
                    
                    if (cursor?.moveToFirst() == true) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS))
                        
                        if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                            cursor.close()
                            _state.update { AudioState.Ready }
                            continuation.resume(Result.success(Unit))
                            return@launch
                        } else if (status == android.app.DownloadManager.STATUS_FAILED) {
                            cursor.close()
                            _state.update { AudioState.Error }
                            continuation.resume(Result.failure(Exception("Download falhou")))
                            return@launch
                        }
                        
                        val downloadedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        
                        if (downloadedBytes != lastProgress) {
                            lastProgress = downloadedBytes
                            _progress.update { downloadedBytes }
                        }
                    }
                    
                    kotlinx.coroutines.delay(100)
                }
            }
        }
    }
    
    actual suspend fun play(filePath: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    continuation.resume(Result.failure(Exception("Arquivo não encontrado: $filePath")))
                    return@suspendCancellableCoroutine
                }
                
                mediaPlayer?.reset()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    setOnPreparedListener {
                        start()
                        _state.update { AudioState.Playing }
                        startProgressUpdates()
                        continuation.resume(Result.success(Unit))
                    }
                    setOnCompletionListener {
                        _state.update { AudioState.Idle }
                        stopProgressUpdates()
                    }
                    setOnErrorListener { _, _, _ ->
                        _state.update { AudioState.Error }
                        stopProgressUpdates()
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                _state.update { AudioState.Error }
                continuation.resume(Result.failure(e))
            }
        }
    }
    
    actual fun pause() {
        mediaPlayer?.takeIf { it.isPlaying }?.let {
            it.pause()
            _state.update { AudioState.Paused }
            stopProgressUpdates()
        }
    }
    
    actual fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying || it.currentPosition > 0) {
                it.stop()
                it.reset()
                createMediaPlayer()
            }
        }
        _state.update { AudioState.Idle }
        stopProgressUpdates()
    }
    
    actual fun release() {
        stopProgressUpdates()
        mediaPlayer?.release()
        mediaPlayer = null
        _state.update { AudioState.Idle }
    }
    
    actual suspend fun getCurrentPosition(): Long {
        return mediaPlayer?.currentPosition?.toLong() ?: 0L
    }
    
    actual suspend fun getDuration(): Long {
        return mediaPlayer?.duration?.toLong() ?: 0L
    }
    
    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressJob = CoroutineScope(Dispatchers.IO).launch {
            while (mediaPlayer?.isPlaying == true) {
                _progress.update { getCurrentPosition() }
                kotlinx.coroutines.delay(100)
            }
        }
    }
    
    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }
}