package jp.xhw.mikke.services.identity.model

import kotlin.uuid.Uuid

@JvmInline
value class UserId(
    val value: Uuid,
)
