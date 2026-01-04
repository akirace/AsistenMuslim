package com.aghatis.asmal.utils

import com.aghatis.asmal.data.model.PrayerTimes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object PrayerTimeUtils {

    fun getNextPrayer(prayerTimes: PrayerTimes): Pair<String, String>? {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val nowV = Calendar.getInstance()
        val currentTime = sdf.format(nowV.time) // e.g. "14:30"

        // Map of name -> time string
        val timesMap = mapOf(
            "Fajr" to prayerTimes.fajr,
            "Dhuhr" to prayerTimes.dhuhr,
            "Asr" to prayerTimes.asr,
            "Maghrib" to prayerTimes.maghrib,
            "Isha" to prayerTimes.isha
        )

        // Find the first time that is strictly greater than current time
        // Note: This logic assumes simple string comparison works for HH:mm format, which it does ("14:30" > "12:00")
        // But we must handle day wrap-around (if now is after Isha, next is Fajr tomorrow).
        
        var nextPrayer: Pair<String, String>? = null
        var minDiff = Int.MAX_VALUE

        // Convert current to minutes for easier comparison
        val currentMinutes = timeToMinutes(currentTime)

        for ((name, timeStr) in timesMap) {
            val prayerMinutes = timeToMinutes(timeStr)
            var diff = prayerMinutes - currentMinutes
            
            if (diff <= 0) {
                 // It's already passed or now, so next instance is tomorrow (add 24 hours minutes)
                 // But strictly speaking, if we want "next FUTURE prayer today", we skip.
                 // If we consider tomorrow's Fajr as next, we treat it with lowest positive diff effectively.
                 continue
            }

            if (diff < minDiff) {
                minDiff = diff
                nextPrayer = name to timeStr
            }
        }

        // If no prayer found for today (i.e. after Isha), detailed logic would say Fajr tomorrow.
        // For simpler scope, if return null, UI handles it (e.g. shows Fajr for tomorrow or just highlighted Isha finished).
        // Let's explicitly fallback to Fajr if all passed, marking it as next.
        if (nextPrayer == null) {
            nextPrayer = "Fajr" to prayerTimes.fajr
        }

        return nextPrayer
    }
    
    fun hasTimePassed(timeStr: String): Boolean {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Calendar.getInstance()
        val currentTimeStr = sdf.format(now.time)
        return timeToMinutes(currentTimeStr) >= timeToMinutes(timeStr)
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        if (parts.size != 2) return 0
        return parts[0].toInt() * 60 + parts[1].toInt()
    }
}
