package com.aghatis.asmal.data.repository

import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.room.Entity
import com.aghatis.asmal.data.remote.OverpassApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import com.aghatis.asmal.data.local.dao.MosqueDao
import com.aghatis.asmal.data.local.entity.MosqueCacheEntity


data class Mosque(
    val id: Long,
    val name: String,
    val lat: Double,
    val lon: Double,
    val address: String?,
    val distance: Float // in meters
)

class MosqueRepository(private val context: Context, private val mosqueDao: MosqueDao) {
    private val api: OverpassApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://overpass-api.de/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(OverpassApi::class.java)
    }

    suspend fun getNearestMosques(lat: Double, lon: Double, radiusMeters: Int = 5000, forceRefresh: Boolean = false): Result<List<Mosque>> {
        return try {
            if (!forceRefresh) {
                val cached = mosqueDao.getAllMosques()
                if (cached.isNotEmpty()) {
                    // Check if *any* cached mosque reference location is close to current user location
                    // Or simpler: check the first one's reference (assuming all inserted together)
                    val first = cached[0]
                    val results = FloatArray(1)
                    Location.distanceBetween(lat, lon, first.referenceLat, first.referenceLon, results)
                    if (results[0] < 1000) { // 1km tolerance for "same location search"
                        // Return cached
                        val mapped = cached.map { 
                            Mosque(it.id, it.name, it.lat, it.lon, it.address, it.distance)
                        }
                        return Result.success(mapped)
                    }
                }
            }

            val query = """
                [out:json][timeout:25];
                (
                  node["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,$lat,$lon);
                );
                out body;
            """.trimIndent()

            val response = api.getNearestMosques(query)
            
            val mosques = response.elements.mapNotNull { element ->
                if (element.lat != 0.0 && element.lon != 0.0) {
                     val results = FloatArray(1)
                     Location.distanceBetween(lat, lon, element.lat, element.lon, results)
                     val distance = results[0]
                     
                     // Name Handling Logic
                     var finalName = element.name
                     if (finalName.isNullOrEmpty()) {
                         // Try to use address from tags first
                         val tagAddress = element.address
                         if (!tagAddress.isNullOrEmpty()) {
                             finalName = "Masjid di $tagAddress"
                         } else {
                             // Reverse Geocoding
                             finalName = getGeocodedName(element.lat, element.lon)
                         }
                     }
                     
                     Mosque(
                         id = element.id,
                         name = finalName ?: "Masjid di Lokasi Ini",
                         lat = element.lat,
                         lon = element.lon,
                         address = element.address,
                         distance = distance
                     )
                } else {
                    null
                }
            }.sortedBy { it.distance }

            if (mosques.isEmpty()) {
                 Result.failure(Exception("Tidak ada masjid ditemukan di sekitar lokasi."))
            } else {
                 // Cache logic: Clear old and insert new
                 mosqueDao.clearAll()
                 val entities = mosques.map { 
                     MosqueCacheEntity(
                         id = it.id,
                         name = it.name,
                         lat = it.lat,
                         lon = it.lon,
                         address = it.address,
                         distance = it.distance,
                         referenceLat = lat,
                         referenceLon = lon
                     ) 
                 }
                 mosqueDao.insertAll(entities)
                 
                 Result.success(mosques)
            }
        } catch (e: Exception) {
            // Fallback to cache?
             val cached = mosqueDao.getAllMosques()
             if (cached.isNotEmpty()) {
                  val mapped = cached.map { 
                        Mosque(it.id, it.name, it.lat, it.lon, it.address, it.distance)
                  }
                  Result.success(mapped)
             } else {
                 Result.failure(e)
             }
        }
    }

    private fun getGeocodedName(lat: Double, lon: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                // Try specific to general
                val location = address.thoroughfare 
                    ?: address.subLocality 
                    ?: address.locality 
                    ?: address.subAdminArea 
                    ?: "Area Sekitar"
                "Masjid di $location"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
