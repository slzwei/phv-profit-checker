package com.example.tadaprofitchecker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TripRecordAdapter(private val tripList: List<TripRecord>) :
    RecyclerView.Adapter<TripRecordAdapter.TripViewHolder>() {

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fareView: TextView = itemView.findViewById(R.id.fareText)
        val minsView: TextView = itemView.findViewById(R.id.minutesText)
        val earningsView: TextView = itemView.findViewById(R.id.earningsText)
        val distanceView: TextView = itemView.findViewById(R.id.distanceText)
        val pickupText: TextView = itemView.findViewById(R.id.pickupText)
        val dropoffText: TextView = itemView.findViewById(R.id.dropoffText)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_record, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = tripList[position]

        holder.fareView.text = "Fare: S$%.2f".format(trip.fare)
        holder.minsView.text = "Total Time: ${trip.totalMinutes} mins"
        holder.earningsView.text = "Earnings per Minute: S$%.2f".format(trip.earningsPerMinute)
        holder.distanceView.text = "Distance: %.1f km".format(trip.totalDistanceKm)
        holder.pickupText.text = "Pickup: ${trip.pickupAddress}"

        val formattedDropoffs = trip.dropoffAddresses.joinToString(
            separator = "\n• ",
            prefix = "• "
        )
        holder.dropoffText.text = "Drop-off(s):\n$formattedDropoffs"

        val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(trip.timestamp))
        holder.timestampText.text = "Recorded: $dateTime"
    }

    override fun getItemCount() = tripList.size
}
