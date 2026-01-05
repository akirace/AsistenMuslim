package com.aghatis.asmal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mosque_cache")
data class MosqueCacheEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val lat: Double,
    val lon: Double,
    val address: String?,
    val distance: Float,
    // Reference location used to fetch this mosque
    val referenceLat: Double,
    val referenceLon: Double
)
