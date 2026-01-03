package com.aghatis.asmal.data.model

import com.google.gson.annotations.SerializedName

data class OverpassResponse(
    @SerializedName("elements")
    val elements: List<OverpassElement> = emptyList()
)

data class OverpassElement(
    @SerializedName("id")
    val id: Long,
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lon")
    val lon: Double,
    @SerializedName("tags")
    val tags: Map<String, String>?
) {
    val name: String?
        get() = tags?.get("name")
    
    val address: String?
        get() = tags?.get("addr:street") ?: tags?.get("addr:full")
}
