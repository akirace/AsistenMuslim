package com.aghatis.asmal.data.model

data class GraphqlRequest(
    val query: String
)

data class BackgroundResponse(
    val data: BackgroundData?
)

data class BackgroundData(
    val shouldUpdateBackgroundAsmals: List<BackgroundConfig>?
)

data class BackgroundConfig(
    val isNightMode: Boolean,
    val background: BackgroundImage?
)

data class BackgroundImage(
    val url: String?
)
