package com.aghatis.asmal.data.remote

import com.aghatis.asmal.data.model.PrayerTimeResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PrayerTimeApi {
    @GET("api/v1/prayer-time/")
    suspend fun getPrayerTimes(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("method") method: Int,
        @Query("school") school: Int,
        @Query("api_key") apiKey: String,
        @Query("date") date: String? = null
    ): PrayerTimeResponse
}
