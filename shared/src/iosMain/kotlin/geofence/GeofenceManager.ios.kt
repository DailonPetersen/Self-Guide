package com.selfguide.geofence

import com.selfguide.model.PontoTuristico

actual class GeofenceManager {
    actual fun startTracking(userId: String, pontos: List<PontoTuristico>) {
        // iOS CoreLocation geofencing implementation
    }
    
    actual fun stopTracking() {
        // Remove geofences
    }
    
    actual fun dispose() {
        stopTracking()
    }
}