package com.selfguide.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.app.PendingIntent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.selfguide.security.AndroidPreferences

actual fun openAppSettings() {
    val context = AndroidPreferences.appContext ?: return
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

actual fun mostrarNotificacao(context: Any, pontoId: String, titulo: String, mensagem: String) {
    val ctx = context as Context
    val channelId = "ponto_turistico_cerca"
    val notificationId = pontoId.hashCode()

    val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (notificationManager.getNotificationChannel(channelId) == null) {
        val channel = NotificationChannel(
            channelId,
            "Ponto Turístico Próximo",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableLights(true)
            lightColor = Color.BLUE
            enableVibration(true)
            description = "Notificações de pontos turísticos próximos"
        }
        notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(ctx, GeofenceBroadcastReceiver::class.java).apply {
        putExtra("ponto_id", pontoId)
        putExtra("action", "open_details")
    }
    val pendingIntent = PendingIntent.getBroadcast(
        ctx,
        notificationId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(ctx, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(titulo)
        .setContentText(mensagem)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(notificationId, notification)
}