package com.aghatis.asmal.data.repository

import android.util.Log
import com.aghatis.asmal.data.model.PrayerLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PrayerLogRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun getPrayerLog(userId: String, date: String): Flow<PrayerLog> = callbackFlow {
        val docRef = firestore.collection("users")
            .document(userId)
            .collection("daily_prayers")
            .document(date)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("PrayerLogRepository", "Listen failed.", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val prayerLog = snapshot.toObject<PrayerLog>()
                val result = prayerLog?.copy(date = date) ?: PrayerLog(date = date)
                trySend(result)
            } else {
                trySend(PrayerLog(date = date))
            }
        }

        awaitClose { listener.remove() }
    }

    suspend fun updatePrayerStatus(userId: String, date: String, prayer: String, isChecked: Boolean) {
        val docRef = firestore.collection("users")
            .document(userId)
            .collection("daily_prayers")
            .document(date)

        val data = mapOf(prayer to isChecked, "date" to date)
        
        try {
            docRef.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e("PrayerLogRepository", "Error updating prayer status", e)
            throw e
        }
    }
}
