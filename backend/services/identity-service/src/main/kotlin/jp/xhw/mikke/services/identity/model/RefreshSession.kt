package jp.xhw.mikke.services.identity.model

import kotlin.time.Instant
import kotlin.uuid.Uuid

data class RefreshSession(
    val id: RefreshSessionId,
    val userId: UserId,
    val refreshTokenHash: String,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val createdAt: Instant,
) {
    fun isActiveAt(at: Instant): Boolean = revokedAt == null && expiresAt > at
}

@JvmInline
value class RefreshSessionId(
    val value: Uuid,
)
