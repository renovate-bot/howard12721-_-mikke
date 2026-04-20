package jp.xhw.mikke.platform.auth.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import jp.xhw.mikke.platform.auth.AuthenticatedPrincipal
import jp.xhw.mikke.platform.auth.IssuedAuthSession
import jp.xhw.mikke.platform.auth.IssuedToken
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*

class JwtTokenService(
    secret: String,
    private val clock: Clock = Clock.systemUTC(),
    private val accessTokenTtl: Duration = Duration.ofHours(1),
    private val refreshTokenTtl: Duration = Duration.ofDays(30),
) {
    private val accessAlgorithm = Algorithm.HMAC256("$secret.access")
    private val refreshAlgorithm = Algorithm.HMAC256("$secret.refresh")
    private val accessVerifier = JWT.require(accessAlgorithm).build()
    private val refreshVerifier = JWT.require(refreshAlgorithm).build()

    fun issueSession(principal: AuthenticatedPrincipal): IssuedAuthSession {
        val now = clock.instant()
        val accessExpiresAt = now.plus(accessTokenTtl)
        val refreshExpiresAt = now.plus(refreshTokenTtl)

        return IssuedAuthSession(
            accessToken =
                IssuedToken(
                    value = issueToken(principal, now, accessExpiresAt, accessAlgorithm),
                    expiresAt = accessExpiresAt,
                ),
            refreshToken =
                IssuedToken(
                    value = issueToken(principal, now, refreshExpiresAt, refreshAlgorithm),
                    expiresAt = refreshExpiresAt,
                ),
        )
    }

    fun authenticateAccessToken(token: String): AuthenticatedPrincipal = authenticate(token, accessVerifier)

    fun authenticateRefreshToken(token: String): AuthenticatedPrincipal = authenticate(token, refreshVerifier)

    private fun authenticate(
        token: String,
        verifier: com.auth0.jwt.JWTVerifier,
    ): AuthenticatedPrincipal {
        val decoded = verifier.verify(token)
        val subject = decoded.subject?.takeIf { it.isNotBlank() } ?: throw JWTVerificationException("Missing sub claim")

        return AuthenticatedPrincipal(subject = subject)
    }

    private fun issueToken(
        principal: AuthenticatedPrincipal,
        issuedAt: Instant,
        expiresAt: Instant,
        algorithm: Algorithm,
    ): String =
        JWT
            .create()
            .withSubject(principal.subject)
            .withIssuedAt(Date.from(issuedAt))
            .withExpiresAt(Date.from(expiresAt))
            .withJWTId(UUID.randomUUID().toString())
            .sign(algorithm)
}
