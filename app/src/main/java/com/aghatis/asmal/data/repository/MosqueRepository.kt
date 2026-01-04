package com.aghatis.asmal.data.repository

import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.aghatis.asmal.data.model.OverpassElement
import com.aghatis.asmal.data.remote.OverpassApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

data class Mosque(
    val id: Long,
    val name: String,
    val lat: Double,
    val lon: Double,
    val address: String?,
    val distance: Float // in meters
)

class MosqueRepository(private val context: Context) {
    private val api: OverpassApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://overpass-api.de/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(OverpassApi::class.java)
    }

    suspend fun getNearestMosques(lat: Double, lon: Double, radiusMeters: Int = 5000): Result<List<Mosque>> {
        return try {
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
                 Result.success(mosques)
            }
        } catch (e: Exception) {
            Result.failure(e)
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
