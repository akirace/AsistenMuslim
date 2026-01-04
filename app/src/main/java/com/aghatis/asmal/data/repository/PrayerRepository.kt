package com.aghatis.asmal.data.repository

import com.aghatis.asmal.data.model.PrayerData
import com.aghatis.asmal.data.remote.PrayerTimeApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PrayerRepository {
    private val api: PrayerTimeApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://islamicapi.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(PrayerTimeApi::class.java)
    }

    suspend fun getPrayerTimes(lat: Double, long: Double, date: String? = null): Result<PrayerData> {
        return try {
            val response = api.getPrayerTimes(
                latitude = lat,
                longitude = long,
                method = 20, // KEMENAG Indonesia
                school = 1, // Syafi'i
                apiKey = "E1Au0cBZHoQcnYvN5XhNHpQDUATmBFrB1tuxAnwBmv04PPHz",
                date = date
            )
            if (response.code == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("No prayer time data found or API error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
