package com.aghatis.asmal.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "qori")
data class QoriEntity(
    @PrimaryKey
    val idReciter: String,
    val reciterName: String,
    val photoUrl: String?
)

// GraphQL Response Classes
data class QoriResponse(
    val data: QoriData?
)

data class QoriData(
    val qoris: List<QoriDto>?
)

data class QoriDto(
    val idReciter: String,
    val reciter: String,
    val photoUrl: QoriPhoto?
)

data class QoriPhoto(
    val url: String?
)

fun QoriDto.toEntity(): QoriEntity {
    return QoriEntity(
        idReciter = this.idReciter,
        reciterName = this.reciter,
        photoUrl = this.photoUrl?.url?.let { 
            if (it.startsWith("http")) it else "https://api.aghatis.id$it"
        }
    )
}
