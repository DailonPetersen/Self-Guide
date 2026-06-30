package com.selfguide.util

import platform.UIKit.UIApplication
import platform.Foundation.NSURL
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.Foundation.NSUUID

actual fun openAppSettings() {
    val settingsUrl = NSURL.URLWithString("UIApplication.openSettingsURLString")
    if (settingsUrl != null) {
        UIApplication.sharedApplication.openURL(settingsUrl)
    }
}

actual fun mostrarNotificacao(context: Any, pontoId: String, titulo: String, mensagem: String) {
    val center = UNUserNotificationCenter.currentNotificationCenter
    val content = UNMutableNotificationContent()
    content.title = titulo
    content.body = mensagem
    content.sound = null

    val request = UNNotificationRequest.requestWithIdentifier(
        identifier = NSUUID.UUID().UUIDString,
        content = content,
        trigger = null
    )
    center.addNotificationRequest(request)
}