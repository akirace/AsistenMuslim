package com.aghatis.asmal.data.model

import com.google.gson.annotations.SerializedName

data class SurahDto(
    @SerializedName("surahName") val surahName: String,
    @SerializedName("surahNameArabic") val surahNameArabic: String,
    @SerializedName("surahNameArabicLong") val surahNameArabicLong: String,
    @SerializedName("surahNameTranslation") val surahNameTranslation: String,
    @SerializedName("revelationPlace") val revelationPlace: String,
    @SerializedName("totalAyah") val totalAyah: Int
)

fun SurahDto.toEntity(id: Int): SurahEntity {
    return SurahEntity(
        surahNo = id,
        surahName = surahName,
        surahNameArabic = surahNameArabic,
        surahNameArabicLong = surahNameArabicLong,
        surahNameTranslation = surahNameTranslation,
        revelationPlace = revelationPlace,
        totalAyah = totalAyah
    )
}
