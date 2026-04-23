package jp.xhw.mikke.services.identity.application

import jp.xhw.mikke.platform.auth.AuthenticatedPrincipal
import jp.xhw.mikke.platform.auth.IssuedAuthSession
import jp.xhw.mikke.platform.auth.IssuedToken
import jp.xhw.mikke.platform.auth.PasswordPolicy
import jp.xhw.mikke.platform.auth.jwt.JwtTokenService
import jp.xhw.mikke.platform.database.TransactionRunner
import jp.xhw.mikke.services.identity.model.*
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class IdentityService(
    private val userRepository: IdentityUserRepository,
    private val refreshSessionRepository: RefreshSessionRepository,
    private val transactionRunner: TransactionRunner,
    private val passwordHasher: PasswordHasher,
    private val tokenService: JwtTokenService,
    private val refreshSessionTokenService: RefreshSessionTokenService = RefreshSessionTokenService(),
    private val clock: Clock = Clock.System,
) {
    fun register(command: RegisterIdentityUserCommand): AuthenticatedIdentityUser =
        transactionRunner.runInTransaction {
            try {
                val now = clock.now()
                PasswordPolicy.validate(command.password)
                val user =
                    IdentityUser(
                        id = UserId(Uuid.random()),
                        email = Email(command.email.normalizeEmail()),
                        username = Username(command.username.trim()),
                        displayName = DisplayName(command.displayName.trim()),
                        passwordHash = passwordHasher.hash(command.password),
                        createdAt = now,
                        updatedAt = now,
                    )
                userRepository.saveUser(user)

                AuthenticatedIdentityUser(
                    user = user,
                    session = issueAuthSession(user.id, now),
                )
            } catch (e: IllegalArgumentException) {
                throw InvalidIdentityInputException(message = e.message ?: "invalid input", cause = e)
            }
        }

    fun login(command: LoginIdentityUserCommand): AuthenticatedIdentityUser {
        val user =
            transactionRunner.runInTransaction {
                userRepository.findByLogin(command.loginId)
                    ?: throw InvalidCredentialsException()
            }

        val passwordMatches =
            runCatching { passwordHasher.verify(command.password, user.passwordHash) }
                .getOrDefault(false)

        if (!passwordMatches) {
            throw InvalidCredentialsException()
        }

        val session = transactionRunner.runInTransaction { issueAuthSession(user.id, clock.now()) }

        return AuthenticatedIdentityUser(user = user, session = session)
    }

    fun refreshSession(refreshToken: String): IssuedAuthSession {
        val now = clock.now()
        val refreshTokenHash = refreshSessionTokenService.hash(refreshToken)

        return transactionRunner.runInTransaction {
            val currentSession =
                refreshSessionRepository
                    .findByRefreshTokenHash(refreshTokenHash)
                    ?.takeIf { it.isActiveAt(now) }
                    ?: throw InvalidRefreshTokenException()

            val user =
                userRepository.findByIds(listOf(currentSession.userId)).firstOrNull()
                    ?: throw InvalidRefreshTokenException()

            val revoked = refreshSessionRepository.revoke(currentSession.id, now)
            if (!revoked) {
                throw InvalidRefreshTokenException()
            }

            issueAuthSession(user.id, now)
        }
    }

    fun logout(refreshToken: String) {
        val now = clock.now()
        val refreshTokenHash = refreshSessionTokenService.hash(refreshToken)

        transactionRunner.runInTransaction {
            refreshSessionRepository.revokeByRefreshTokenHash(refreshTokenHash, now)
        }
    }

    fun getMe(subject: String): IdentityUser {
        val userId =
            subject.toUserIdOrNull()
                ?: throw UserNotFoundException()

        return transactionRunner.runInTransaction {
            userRepository.findByIds(listOf(userId)).firstOrNull()
                ?: throw UserNotFoundException()
        }
    }

    private fun issueAuthSession(
        userId: UserId,
        issuedAt: Instant,
    ): IssuedAuthSession {
        val principal = AuthenticatedPrincipal(subject = userId.value.toString())
        val accessToken = tokenService.issueAccessToken(principal = principal, issuedAt = issuedAt)
        val refreshToken = refreshSessionTokenService.issueRefreshToken(issuedAt = issuedAt)

        refreshSessionRepository.save(
            RefreshSession(
                id = RefreshSessionId(Uuid.random()),
                userId = userId,
                refreshTokenHash = refreshSessionTokenService.hash(refreshToken.value),
                expiresAt = refreshToken.expiresAt,
                revokedAt = null,
                createdAt = issuedAt,
            ),
        )

        return IssuedAuthSession(
            accessToken = accessToken,
            refreshToken = IssuedToken(value = refreshToken.value, expiresAt = refreshToken.expiresAt),
        )
    }
}

data class RegisterIdentityUserCommand(
    val email: String,
    val username: String,
    val displayName: String,
    val password: String,
)

data class LoginIdentityUserCommand(
    val loginId: String,
    val password: String,
)

data class AuthenticatedIdentityUser(
    val user: IdentityUser,
    val session: IssuedAuthSession,
)

sealed class IdentityApplicationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class InvalidIdentityInputException(
    message: String,
    cause: Throwable? = null,
) : IdentityApplicationException(message, cause)

class InvalidCredentialsException(
    message: String = "Invalid credentials",
    cause: Throwable? = null,
) : IdentityApplicationException(message, cause)

class InvalidRefreshTokenException(
    message: String = "Invalid refresh token",
    cause: Throwable? = null,
) : IdentityApplicationException(message, cause)

class UserNotFoundException(
    message: String = "User not found",
    cause: Throwable? = null,
) : IdentityApplicationException(message, cause)

private fun String.normalizeEmail(): String = trim().lowercase()

private fun String.toUserIdOrNull(): UserId? = runCatching { UserId(Uuid.parse(trim())) }.getOrNull()
