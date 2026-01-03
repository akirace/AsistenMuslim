package com.aghatis.asmal.data.remote

import com.aghatis.asmal.data.model.OverpassResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassApi {
    @GET("interpreter")
    suspend fun getNearestMosques(
        @Query("data") data: String
    ): OverpassResponse
}
