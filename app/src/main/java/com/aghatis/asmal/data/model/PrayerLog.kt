package com.aghatis.asmal.data.model

data class PrayerLog(
    val date: String = "",
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false
)
