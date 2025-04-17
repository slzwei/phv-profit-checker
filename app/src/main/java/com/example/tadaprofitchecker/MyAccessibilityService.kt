package com.example.tadaprofitchecker

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import android.location.Location
import android.location.LocationManager
import android.location.LocationListener
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan

class MyAccessibilityService : AccessibilityService() {

    private var overlayView: View? = null
    private lateinit var windowManager: WindowManager
    private var lastShownText = ""
    private var currentDisplayedText = ""

    private val apiKey = "YOUR_API_KEY"
    private val tripHistory = mutableListOf<TripRecord>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val rootNode = rootInActiveWindow ?: return
        val screenText = getAllText(rootNode).trim()
        val lines = screenText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        // Collect full addresses using lines before (S) postal codes
        val addressList = mutableListOf<String>()
        val postalCodeRegex = Regex("""\(S\)\d{5,6}""")

        for (i in 1 until lines.size) {
            if (postalCodeRegex.containsMatchIn(lines[i])) {
                val fullAddress = "${lines[i - 1]}, ${lines[i]}"
                addressList.add(fullAddress)
            }
        }

        val isJobScreen = screenText.contains("ACCEPT", ignoreCase = true) &&
                screenText.contains("SGD", ignoreCase = true) &&
                addressList.size >= 2

        if (isJobScreen && screenText != lastShownText) {
            lastShownText = screenText
            showOverlay("\u23F3 Calculating trip details...")

            val pickup = addressList[0]
            val dropoffs = addressList.drop(1)

            if (pickup.isNotEmpty() && dropoffs.isNotEmpty()) {
                val fareRegex = Regex("""\d+\.\d{2}""")
                val fareMatch = fareRegex.find(screenText)
                val fare = fareMatch?.value?.toDoubleOrNull() ?: 0.0

                getMultiStopTripDurations(pickup, dropoffs) { pickupSecs, tripSecs, totalDistance ->
                    val pickupMins = pickupSecs / 60
                    val tripMins = tripSecs / 60
                    val totalMins = pickupMins + tripMins
                    val earningsPerMin = if (totalMins > 0) fare / totalMins else 0.0
                    val totalDistanceKm = totalDistance / 1000.0

                    TripRecordStore.addTrip(
                        TripRecord(
                            fare = fare,
                            pickupMinutes = pickupMins,
                            tripMinutes = tripMins,
                            totalMinutes = totalMins,
                            earningsPerMinute = earningsPerMin,
                            totalDistanceKm = totalDistanceKm,
                            pickupAddress = pickup,
                            dropoffAddresses = dropoffs
                        )
                    )

                    val message = """
                    💰 S$%.2f
                    📍 %d mins to pickup
                    🚕 %d mins trip
                    🕒 %d mins in total 
                    🤑 S$%.2f/min earnings
                """.trimIndent().format(
                        fare, pickupMins, tripMins, totalMins, earningsPerMin
                    )

                    updateOverlayStyled(message, totalMins, earningsPerMin)
                }
            }
        } else if (!isJobScreen && overlayView != null) {
            lastShownText = ""
            removeOverlay()
        }
    }


    private fun updateOverlayStyled(message: String, totalMins: Int, earningsPerMin: Double) {
        if (message == currentDisplayedText) return
        currentDisplayedText = message
        removeOverlay()

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_layout, null)

        val textView = overlayView!!.findViewById<TextView>(R.id.overlay_text)
        val dismissButton = overlayView!!.findViewById<Button>(R.id.dismiss_button)

        val spannable = SpannableString(message)
        val earningsLine = "🤑 S$%.2f/min earnings".format(earningsPerMin)
        val totalLine = "🕒 $totalMins mins in total"
        val earningsStart = message.indexOf(earningsLine)
        val earningsEnd = earningsStart + earningsLine.length

        if (earningsStart >= 0 && earningsEnd <= message.length) {
            spannable.setSpan(StyleSpan(Typeface.BOLD), earningsStart, earningsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(RelativeSizeSpan(1.4f), earningsStart, earningsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(Color.parseColor("#FFA500")), earningsStart, earningsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        textView.text = spannable

        dismissButton.setOnClickListener {
            removeOverlay()
            lastShownText = ""
            currentDisplayedText = ""
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.y = 400

        windowManager.addView(overlayView, params)
    }

    private fun getMultiStopTripDurations(
        pickupAddress: String,
        dropoffs: List<String>,
        callback: (Int, Int, Int) -> Unit
    ) {
        getCurrentLocation { currentLocation ->
            if (currentLocation == null) {
                callback(0, 0, 0)
                return@getCurrentLocation
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val originLatLng = "${currentLocation.latitude},${currentLocation.longitude}"
                    val pickup = URLEncoder.encode(pickupAddress, "UTF-8")
                    val pickupUrl = URL("https://maps.googleapis.com/maps/api/directions/json?origin=$originLatLng&destination=$pickup&key=$apiKey")
                    val pickupDuration = fetchDurationFromUrl(pickupUrl)

                    var tripDuration = 0
                    var totalDistance = 0
                    var last = pickup
                    for (addr in dropoffs) {
                        val next = URLEncoder.encode(addr, "UTF-8")
                        val url = URL("https://maps.googleapis.com/maps/api/directions/json?origin=$last&destination=$next&key=$apiKey")
                        val (dura, dist) = fetchDurationAndDistance(url)
                        tripDuration += dura
                        totalDistance += dist
                        last = next
                    }

                    withContext(Dispatchers.Main) {
                        callback(pickupDuration, tripDuration, totalDistance)
                    }
                } catch (e: Exception) {
                    Log.e("TADA_ERROR", "Multi-stop error: ${e.message}")
                    withContext(Dispatchers.Main) {
                        callback(0, 0, 0)
                    }
                }
            }
        }
    }


    private fun getAllText(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val sb = StringBuilder()
        if (!node.text.isNullOrEmpty()) sb.append(node.text).append("\n")
        for (i in 0 until node.childCount) {
            sb.append(getAllText(node.getChild(i)))
        }
        return sb.toString()
    }

    private fun getCurrentLocation(callback: (Location?) -> Unit) {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            if (isGpsEnabled) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        callback(location)
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {
                        callback(null)
                    }
                }, null)
            } else {
                callback(null)
            }
        } catch (e: SecurityException) {
            Log.e("TADA_LOC", "Location permission not granted.")
            callback(null)
        }
    }

    private fun fetchDurationFromUrl(url: URL): Int {
        val conn = url.openConnection() as HttpsURLConnection
        conn.connect()
        val json = conn.inputStream.bufferedReader().readText()
        conn.disconnect()

        return try {
            val root = JSONObject(json)
            root.getJSONArray("routes")
                .optJSONObject(0)
                ?.getJSONArray("legs")
                ?.optJSONObject(0)
                ?.getJSONObject("duration")
                ?.getInt("value") ?: 0
        } catch (e: Exception) {
            Log.e("TADA_ERROR", "Parsing error: ${e.message}")
            0
        }
    }

    private fun fetchDurationAndDistance(url: URL): Pair<Int, Int> {
        val conn = url.openConnection() as HttpsURLConnection
        conn.connect()
        val json = conn.inputStream.bufferedReader().readText()
        conn.disconnect()

        return try {
            val root = JSONObject(json)
            val leg = root.getJSONArray("routes").optJSONObject(0)?.getJSONArray("legs")?.optJSONObject(0)
            val duration = leg?.getJSONObject("duration")?.getInt("value") ?: 0
            val distance = leg?.getJSONObject("distance")?.getInt("value") ?: 0
            Pair(duration, distance)
        } catch (e: Exception) {
            Log.e("TADA_ERROR", "Distance parsing error: ${e.message}")
            Pair(0, 0)
        }
    }

    private fun showOverlay(message: String) {
        removeOverlay()

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_layout, null)

        val textView = overlayView!!.findViewById<TextView>(R.id.overlay_text)
        val dismissButton = overlayView!!.findViewById<Button>(R.id.dismiss_button)
        textView.text = message
        dismissButton.setOnClickListener {
            removeOverlay()
            lastShownText = ""
            currentDisplayedText = ""
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.y = 400

        windowManager.addView(overlayView, params)
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            overlayView = null
            currentDisplayedText = ""
        }
    }

    override fun onInterrupt() {}
}