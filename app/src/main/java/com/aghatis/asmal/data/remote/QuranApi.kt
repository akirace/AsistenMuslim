package com.aghatis.asmal.data.remote

import com.aghatis.asmal.data.model.AyahResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface QuranApi {
    @GET("{surahNo}/{ayahNo}.json")
    suspend fun getAyah(
        @Path("surahNo") surahNo: Int,
        @Path("ayahNo") ayahNo: Int
    ): AyahResponse
}
