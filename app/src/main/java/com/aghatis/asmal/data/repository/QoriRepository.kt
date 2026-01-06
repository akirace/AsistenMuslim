package com.aghatis.asmal.data.repository

import com.aghatis.asmal.data.local.dao.QoriDao
import com.aghatis.asmal.data.model.GraphqlRequest
import com.aghatis.asmal.data.model.QoriEntity
import com.aghatis.asmal.data.model.QoriResponse
import com.aghatis.asmal.data.model.toEntity
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface QoriApi {
    @POST("graphql")
    suspend fun fetchQoris(@Body request: GraphqlRequest): QoriResponse
}

class QoriRepository(private val qoriDao: QoriDao) {
    private val api: QoriApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.aghatis.id/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(QoriApi::class.java)
    }

    suspend fun getQoris(): Flow<List<QoriEntity>> {
        val count = qoriDao.getCount()
        if (count == 0) {
            try {
                val query = """
                    query Qoris {
                      qoris {
                        idReciter
                        reciter
                        photoUrl {
                          url
                        }
                      }
                    }
                """.trimIndent()

                val response = api.fetchQoris(GraphqlRequest(query))
                val qoriDtos = response.data?.qoris

                if (!qoriDtos.isNullOrEmpty()) {
                    val qoriEntities = qoriDtos.map { it.toEntity() }
                    qoriDao.insertAll(qoriEntities)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // In case of error, we just return what's in DB (empty) or could expose error
            }
        }
        return qoriDao.getAllQoris()
    }
}
