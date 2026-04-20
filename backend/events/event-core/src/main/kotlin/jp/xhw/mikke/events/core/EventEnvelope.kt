package jp.xhw.mikke.events.core

import kotlinx.serialization.Serializable

@Serializable
data class EventEnvelope<T>(
    val eventId: String,
    val eventType: String,
    val eventVersion: Int,
    val occurredAt: String,
    val producer: String,
    val aggregateType: String,
    val aggregateId: String,
    val correlationId: String? = null,
    val causationId: String? = null,
    val payload: T,
)
