package com.selfguide.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val pontoId = intent.getStringExtra("ponto_id")
        val transition = com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER

        when (action) {
            "com.selfguide.geofence.ENTER" -> {
                Log.d("Geofence", "Entered geofence for point: $pontoId")
                // Trigger dwell time timer in orchestrator
            }
            "com.selfguide.geofence.EXIT" -> {
                Log.d("Geofence", "Exited geofence for point: $pontoId")
                // Cancel dwell time timer in orchestrator
            }
            "open_details" -> {
                Log.d("Geofence", "Opening details for point: $pontoId")
                // Open PontoDetalheScreen
            }
        }
    }
}