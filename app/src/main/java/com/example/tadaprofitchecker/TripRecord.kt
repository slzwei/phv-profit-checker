package com.example.tadaprofitchecker

data class TripRecord(
    val fare: Double,
    val pickupMinutes: Int,
    val tripMinutes: Int,
    val totalMinutes: Int,
    val earningsPerMinute: Double,
    val totalDistanceKm: Double,
    val pickupAddress: String,
    val dropoffAddresses: List<String>,
    val timestamp: Long = System.currentTimeMillis() // ✅ Add this
)
