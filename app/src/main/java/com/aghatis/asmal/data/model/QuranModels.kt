package com.aghatis.asmal.data.model

import com.google.gson.annotations.SerializedName

data class AyahResponse(
    @SerializedName("surahName") val surahName: String,
    @SerializedName("surahNameArabic") val surahNameArabic: String,
    @SerializedName("surahNameTranslation") val surahNameTranslation: String,
    @SerializedName("surahNo") val surahNo: Int,
    @SerializedName("ayahNo") val ayahNo: Int,
    @SerializedName("audio") val audio: Map<String, AudioData>,
    @SerializedName("english") val english: String,
    @SerializedName("arabic1") val arabic1: String
)

data class AudioData(
    @SerializedName("reciter") val reciter: String,
    @SerializedName("url") val url: String,
    @SerializedName("originalUrl") val originalUrl: String
)
