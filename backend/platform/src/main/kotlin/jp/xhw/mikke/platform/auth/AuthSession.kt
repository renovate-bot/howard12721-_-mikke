package jp.xhw.mikke.platform.auth

import java.time.Instant

data class IssuedToken(
    val value: String,
    val expiresAt: Instant,
)

data class IssuedAuthSession(
    val accessToken: IssuedToken,
    val refreshToken: IssuedToken,
)
