package com.aghatis.asmal.data.model

import com.google.gson.annotations.SerializedName

data class SurahDetailResponse(
    @SerializedName("surahName") val surahName: String,
    @SerializedName("surahNameArabic") val surahNameArabic: String,
    @SerializedName("surahNameArabicLong") val surahNameArabicLong: String,
    @SerializedName("surahNameTranslation") val surahNameTranslation: String,
    @SerializedName("revelationPlace") val revelationPlace: String,
    @SerializedName("totalAyah") val totalAyah: Int,
    @SerializedName("surahNo") val surahNo: Int,
    @SerializedName("audio") val audio: Map<String, AudioReciter>,
    @SerializedName("english") val english: List<String>,
    @SerializedName("arabic1") val arabic1: List<String>,
    @SerializedName("arabic2") val arabic2: List<String>
)

data class AudioReciter(
    @SerializedName("reciter") val reciter: String,
    @SerializedName("url") val url: String,
    @SerializedName("originalUrl") val originalUrl: String
)
