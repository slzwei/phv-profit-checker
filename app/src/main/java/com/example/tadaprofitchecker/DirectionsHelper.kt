package com.example.tadaprofitchecker

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object DirectionsHelper {
    fun getEstimatedDuration(pickup: String, dropoff: String, callback: (Int) -> Unit) {
        val client = OkHttpClient()

        val url = HttpUrl.Builder()
            .scheme("https")
            .host("maps.googleapis.com")
            .addPathSegments("maps/api/directions/json")
            .addQueryParameter("origin", pickup)
            .addQueryParameter("destination", dropoff)
            .addQueryParameter("key", "AIzaSyA83pFNSJZwztR5YjGsbB5jCJ3KarqSYYA") // 🔁 Replace this
            .build()

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(0)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val json = JSONObject(body ?: "")
                val duration = json
                    .getJSONArray("routes")
                    .optJSONObject(0)
                    ?.getJSONArray("legs")
                    ?.optJSONObject(0)
                    ?.getJSONObject("duration")
                    ?.getInt("value") // duration in seconds

                callback((duration ?: 0) / 60) // convert to minutes
            }
        })
    }
}
