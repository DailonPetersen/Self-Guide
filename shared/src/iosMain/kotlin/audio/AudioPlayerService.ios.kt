package com.selfguide.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import kotlin.coroutines.resume

actual class AudioPlayerService {
    private val _state = MutableStateFlow(AudioState.Idle)
    actual val state: Flow<AudioState> = _state
    
    private val _progress = MutableStateFlow(0L)
    actual val progress: Flow<Long> = _progress
    
    private var audioPlayer: AVAudioPlayer? = null
    
    actual suspend fun downloadAudio(url: String, fileName: String): Result<Unit> {
        return suspendCoroutine { continuation ->
            _state.update { AudioState.Downloading }
            
            val session = NSURLSession.sessionWithConfiguration(
                configuration = NSURLSessionConfiguration.defaultSessionConfiguration,
                delegate = null,
                delegateQueue = null
            )
            
            val downloadTask = session.downloadTaskWithURL(NSURL.URLWithString(url)!!) { location, response, error ->
                if (error != null) {
                    _state.update { AudioState.Error }
                    continuation.resume(Result.failure(Exception(error.localizedDescription)))
                    return@downloadTaskWithURL
                }
                
                location?.let { downloadedFile ->
                    val directories = NSSearchPathForDirectoriesInDomains(
                        directory = NSDocumentDirectory,
                        domainMask = NSUserDomainMask,
                        expandTilde = true
                    )
                    val documentsDir = directories.first() as NSString
                    val audioDirStr = documentsDir.stringByAppendingPathComponent("audio")
                    val audioDir = NSURL.fileURLWithPath(audioDirStr)
                    val fileManager = NSFileManager.defaultManager
                    
                    if (!fileManager.fileExistsAtPath(audioDir.path)) {
                        fileManager.createDirectoryAtURL(
                            url = audioDir,
                            withIntermediateDirectories = true,
                            attributes = null,
                            error = null
                        )
                    }
                    
                    val destURL = audioDir.URLByAppendingPathComponent("$fileName.mp3")
                    fileManager.moveItemAtURL(
                        srcURL = downloadedFile,
                        toURL = destURL,
                        error = null
                    )
                    
                    _state.update { AudioState.Ready }
                    continuation.resume(Result.success(Unit))
                } ?: run {
                    _state.update { AudioState.Error }
                    continuation.resume(Result.failure(Exception("Download failed")))
                }
            }
            
            downloadTask.resume()
        }
    }
    
    actual suspend fun play(filePath: String): Result<Unit> {
        return suspendCoroutine { continuation ->
            try {
                val fileURL = NSURL.fileURLWithPath(filePath)
                val player = AVAudioPlayer(contentsOfURL = fileURL, error = null)
                
                audioPlayer = player
                player.prepareToPlay()
                player.play()
                _state.update { AudioState.Playing }
                continuation.resume(Result.success(Unit))
            } catch (e: Exception) {
                _state.update { AudioState.Error }
                continuation.resume(Result.failure(e))
            }
        }
    }
    
    actual fun pause() {
        audioPlayer?.takeIf { it.playing }?.let {
            it.pause()
            _state.update { AudioState.Paused }
        }
    }
    
    actual fun stop() {
        audioPlayer?.takeIf { it.playing || it.currentTime > 0 }?.let {
            it.stop()
            it.setCurrentTime(0.0)
        }
        _state.update { AudioState.Idle }
    }
    
    actual fun release() {
        audioPlayer?.stop()
        audioPlayer = null
        _state.update { AudioState.Idle }
    }
    
    actual suspend fun getCurrentPosition(): Long {
        return (audioPlayer?.currentTime?.times(1000)?.toLong() ?: 0L)
    }
    
    actual suspend fun getDuration(): Long {
        return (audioPlayer?.duration?.times(1000)?.toLong() ?: 0L)
    }
}