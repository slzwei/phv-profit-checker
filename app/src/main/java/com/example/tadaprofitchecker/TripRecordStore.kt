package com.example.tadaprofitchecker

object TripRecordStore {
    private val tripList = mutableListOf<TripRecord>()

    fun addTrip(record: TripRecord) {
        val isDuplicate = tripList.any {
            it.fare == record.fare &&
                    it.pickupMinutes == record.pickupMinutes &&
                    it.tripMinutes == record.tripMinutes &&
                    it.totalMinutes == record.totalMinutes &&
                    it.earningsPerMinute == record.earningsPerMinute &&
                    it.totalDistanceKm == record.totalDistanceKm &&
                    it.pickupAddress == record.pickupAddress  &&
                    it.dropoffAddresses == record.dropoffAddresses
        }

        if (!isDuplicate) {
            tripList.add(record)
        }
    }

    fun getAllTrips(): List<TripRecord> {
        return tripList.sortedByDescending { it.timestamp } // ✅ Newest at top
    }

    fun clearAllTrips() {
        tripList.clear()
    }

    // ✅ Optional: Group by day if you want to use this in future
    fun getGroupedTripsByDate(): Map<String, List<TripRecord>> {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd")
        return tripList.groupBy { formatter.format(it.timestamp) }
    }
}
