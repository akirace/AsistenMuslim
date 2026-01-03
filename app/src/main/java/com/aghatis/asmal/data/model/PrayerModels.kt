package com.aghatis.asmal.data.model

import com.google.gson.annotations.SerializedName

data class PrayerTimeResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: PrayerData?
)

data class PrayerData(
    @SerializedName("times") val times: PrayerTimes,
    @SerializedName("date") val date: PrayerDate
)

data class PrayerTimes(
    @SerializedName("Fajr") val fajr: String,
    @SerializedName("Sunrise") val sunrise: String,
    @SerializedName("Dhuhr") val dhuhr: String,
    @SerializedName("Asr") val asr: String,
    @SerializedName("Sunset") val sunset: String,
    @SerializedName("Maghrib") val maghrib: String,
    @SerializedName("Isha") val isha: String,
    @SerializedName("Imsak") val imsak: String,
    @SerializedName("Midnight") val midnight: String,
    @SerializedName("Firstthird") val firstThird: String,
    @SerializedName("Lastthird") val lastThird: String
)

data class PrayerDate(
    @SerializedName("readable") val readable: String,
    @SerializedName("timestamp") val timestamp: String
)
