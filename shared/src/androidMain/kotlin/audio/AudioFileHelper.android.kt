package com.selfguide.audio

import android.content.Context
import java.io.File

private var appContext: Context? = null

fun initAudioHelper(context: Context) {
    appContext = context.applicationContext
}

actual fun getAudioFilePath(pontoId: String): String {
    val context = appContext ?: throw IllegalStateException("AudioPlayerService not initialized. Call initAudioHelper() first.")
    val downloadsDir = File(context.getExternalFilesDir(null), "audio")
    return File(downloadsDir, "$pontoId.mp3").absolutePath
}