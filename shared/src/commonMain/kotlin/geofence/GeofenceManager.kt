package com.selfguide.geofence

import com.selfguide.model.PontoTuristico

expect class GeofenceManager {
    fun startTracking(userId: String, pontos: List<PontoTuristico>)
    
    fun stopTracking()
    
    fun dispose()
}