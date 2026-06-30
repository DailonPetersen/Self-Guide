package com.selfguide.audio

import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSSearchPathForDirectoriesInDomains

actual fun getAudioFilePath(pontoId: String): String {
    val directories = NSSearchPathForDirectoriesInDomains(
        directory = NSDocumentDirectory,
        domainMask = NSUserDomainMask,
        expandTilde = true
    )
    val documentsDir = directories.first() as NSString
    return "${documentsDir.stringByAppendingPathComponent("audio")}/${pontoId}.mp3"
}