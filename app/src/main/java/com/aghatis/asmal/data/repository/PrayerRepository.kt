package com.aghatis.asmal.data.repository

import com.aghatis.asmal.data.model.PrayerData
import com.aghatis.asmal.data.remote.PrayerTimeApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PrayerRepository(private val prayerDao: com.aghatis.asmal.data.local.dao.PrayerDao) {
    private val api: PrayerTimeApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://islamicapi.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(PrayerTimeApi::class.java)
    }

    suspend fun getPrayerTimes(lat: Double, long: Double, date: String, address: String? = null, forceRefresh: Boolean = false): Result<PrayerData> {
        return try {
            // Check cache
            if (!forceRefresh) {
                val cached = prayerDao.getPrayerByDate(date)
                if (cached != null) {
                    // Check if location is "close enough" (e.g. within 1km)
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(lat, long, cached.latitude, cached.longitude, results)
                    if (results[0] < 1000) { // 1 km tolerance
                        val gson = com.google.gson.Gson()
                        val times = gson.fromJson(cached.timesJson, com.aghatis.asmal.data.model.PrayerTimes::class.java)
                         // Reconstruct PrayerData. Date is constructed.
                         // Note: Original PrayerDate response structure might be needed.
                         // For now simplified reconstruction or store full json. 
                         // To be robust: Let's assume we store minimal needs or reconstruct.
                         // Actually, storing "times" only might miss "date" field from API.
                         // Let's create a partial PrayerData.
                        val cachedData = PrayerData(
                            times = times,
                            date = com.aghatis.asmal.data.model.PrayerDate(readable = date, timestamp = "") // Fake timestamp
                        )
                        return Result.success(cachedData)
                    }
                }
            }
            
            // Network Call
            val response = api.getPrayerTimes(
                latitude = lat,
                longitude = long,
                method = 20,
                school = 1,
                apiKey = "E1Au0cBZHoQcnYvN5XhNHpQDUATmBFrB1tuxAnwBmv04PPHz",
                date = date
            )
            
            if (response.code == 200 && response.data != null) {
                // Save to Cache
                val gson = com.google.gson.Gson()
                val entity = com.aghatis.asmal.data.local.entity.PrayerCacheEntity(
                    date = date,
                    latitude = lat,
                    longitude = long,
                    timesJson = gson.toJson(response.data.times),
                    locationName = address ?: "Unknown"
                )
                prayerDao.insertOrUpdate(entity)
                
                Result.success(response.data)
            } else {
                Result.failure(Exception("No prayer time data found or API error"))
            }
        } catch (e: Exception) {
             // Fallback to cache if network fails even if forceRefresh was true? 
             // Maybe. But for now strictly follow flow.
             // If network fails, try cache as last resort:
             val cached = prayerDao.getPrayerByDate(date)
             if (cached != null) {
                 val gson = com.google.gson.Gson()
                 val times = gson.fromJson(cached.timesJson, com.aghatis.asmal.data.model.PrayerTimes::class.java)
                 val cachedData = PrayerData(
                    times = times,
                    date = com.aghatis.asmal.data.model.PrayerDate(readable = date, timestamp = "")
                )
                Result.success(cachedData)
             } else {
                 Result.failure(e)
             }
        }
    }
}
