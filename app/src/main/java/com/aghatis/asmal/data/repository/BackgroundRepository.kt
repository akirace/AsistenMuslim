package com.aghatis.asmal.data.repository

import com.aghatis.asmal.data.model.BackgroundResponse
import com.aghatis.asmal.data.model.GraphqlRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface BackgroundApi {
    @POST("graphql")
    suspend fun fetchBackground(@Body request: GraphqlRequest): BackgroundResponse
}

class BackgroundRepository {
    private val api: BackgroundApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.aghatis.id/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(BackgroundApi::class.java)
    }

    /**
     * Fetches both light and dark background URLs in a single API call
     * @return Pair<lightUrl, darkUrl> or null if request fails
     */
    suspend fun getBackgroundUrls(): Pair<String, String>? {
        return try {
            val query = """
                query ShouldUpdateBackgroundAsmals {
                  shouldUpdateBackgroundAsmals {
                    isNightMode
                    background {
                      url
                    }
                  }
                }
            """.trimIndent()

            val response = api.fetchBackground(GraphqlRequest(query))
            val configs = response.data?.shouldUpdateBackgroundAsmals

            if (configs != null && configs.size >= 2) {
                // Find light mode (isNightMode = false) and dark mode (isNightMode = true)
                val lightConfig = configs.find { it.isNightMode == false }
                val darkConfig = configs.find { it.isNightMode == true }

                val lightUrl = lightConfig?.background?.url
                val darkUrl = darkConfig?.background?.url

                if (lightUrl != null && darkUrl != null) {
                    // Convert relative URLs to absolute
                    val absoluteLightUrl = if (lightUrl.startsWith("http")) lightUrl else "https://api.aghatis.id$lightUrl"
                    val absoluteDarkUrl = if (darkUrl.startsWith("http")) darkUrl else "https://api.aghatis.id$darkUrl"
                    
                    Pair(absoluteLightUrl, absoluteDarkUrl)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
