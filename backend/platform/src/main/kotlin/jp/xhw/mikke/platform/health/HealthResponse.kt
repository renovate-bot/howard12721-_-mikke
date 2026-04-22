package jp.xhw.mikke.platform.health

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String = "ok",
    val service: String,
)
