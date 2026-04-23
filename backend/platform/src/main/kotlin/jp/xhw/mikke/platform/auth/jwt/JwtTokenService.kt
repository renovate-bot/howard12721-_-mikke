package jp.xhw.mikke.platform.auth.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import jp.xhw.mikke.platform.auth.AuthenticatedPrincipal
import jp.xhw.mikke.platform.auth.IssuedToken
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class JwtTokenService(
    secret: String,
    private val clock: Clock = Clock.System,
    private val accessTokenTtl: Duration = 15.minutes,
) {
    private val accessAlgorithm = Algorithm.HMAC256("$secret.access")
    private val accessVerifier = JWT.require(accessAlgorithm).build()

    fun issueAccessToken(
        principal: AuthenticatedPrincipal,
        issuedAt: Instant = clock.now(),
    ): IssuedToken {
        val accessExpiresAt = issuedAt + accessTokenTtl

        return IssuedToken(
            value = issueToken(principal, issuedAt, accessExpiresAt, accessAlgorithm),
            expiresAt = accessExpiresAt,
        )
    }

    fun authenticateAccessToken(token: String): AuthenticatedPrincipal = authenticate(token, accessVerifier)

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
            .withIssuedAt(Date(issuedAt.toEpochMilliseconds()))
            .withExpiresAt(Date(expiresAt.toEpochMilliseconds()))
            .withJWTId(UUID.randomUUID().toString())
            .sign(algorithm)
}
