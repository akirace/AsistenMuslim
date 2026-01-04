package com.aghatis.asmal.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "surah")
data class SurahEntity(
    @PrimaryKey
    @SerializedName("surahNo") val surahNo: Int,
    @SerializedName("surahName") val surahName: String,
    @SerializedName("surahNameArabic") val surahNameArabic: String,
    @SerializedName("surahNameArabicLong") val surahNameArabicLong: String,
    @SerializedName("surahNameTranslation") val surahNameTranslation: String,
    @SerializedName("revelationPlace") val revelationPlace: String,
    @SerializedName("totalAyah") val totalAyah: Int
)
