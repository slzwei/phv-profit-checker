package com.example.tadaprofitchecker

import android.os.Bundle
import android.view.View
import android.widget.Spinner
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout


class TripHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TripRecordAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_history)

        recyclerView = findViewById(R.id.recyclerView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
val sortSpinner = findViewById<Spinner>(R.id.sortSpinner)
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val sortedTrips = when (position) {
                    1 -> TripRecordStore.getAllTrips().sortedByDescending { it.fare }
                    2 -> TripRecordStore.getAllTrips().sortedByDescending { it.earningsPerMinute }
                    else -> TripRecordStore.getAllTrips().sortedByDescending { it.timestamp }
                }
                adapter = TripRecordAdapter(sortedTrips)
                recyclerView.adapter = adapter
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TripRecordAdapter(TripRecordStore.getAllTrips())
        recyclerView.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            adapter = TripRecordAdapter(TripRecordStore.getAllTrips())
            recyclerView.adapter = adapter
            swipeRefreshLayout.isRefreshing = false
        }
    }
}
