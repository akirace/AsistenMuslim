package com.aghatis.asmal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_cache")
data class PrayerCacheEntity(
    @PrimaryKey
    val date: String, // format: yyyy-MM-dd
    val latitude: Double,
    val longitude: Double,
    val timesJson: String, // JSON string of PrayerTimes
    val locationName: String
)
