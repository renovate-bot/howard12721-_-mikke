package jp.xhw.mikke.services.identity.application

import jp.xhw.mikke.platform.auth.AuthenticatedPrincipal
import jp.xhw.mikke.platform.auth.IssuedAuthSession
import jp.xhw.mikke.platform.auth.PasswordPolicy
import jp.xhw.mikke.platform.auth.jwt.JwtTokenService
import jp.xhw.mikke.platform.database.TransactionRunner
import jp.xhw.mikke.services.identity.model.*
import java.time.Clock
import kotlin.uuid.Uuid

class IdentityService(
    private val userRepository: IdentityUserRepository,
    private val transactionRunner: TransactionRunner,
    private val passwordHasher: PasswordHasher,
    private val tokenService: JwtTokenService,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun register(command: RegisterIdentityUserCommand): AuthenticatedIdentityUser {
        val user =
            transactionRunner.runInTransaction {
                try {
                    val now = clock.instant()
                    PasswordPolicy.validate(command.password)
                    IdentityUser(
                        id = UserId(Uuid.random()),
                        email = Email(command.email.normalizeEmail()),
                        username = Username(command.username.trim()),
                        displayName = DisplayName(command.displayName.trim()),
                        passwordHash = passwordHasher.hash(command.password),
                        createdAt = now,
                        updatedAt = now,
                    )
                } catch (e: IllegalArgumentException) {
                    throw InvalidIdentityInputException(message = e.message ?: "invalid input", cause = e)
                }.also(userRepository::saveUser)
            }

        return AuthenticatedIdentityUser(
            user = user,
            session = tokenService.issueSession(AuthenticatedPrincipal(subject = user.id.value.toString())),
        )
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

        return AuthenticatedIdentityUser(
            user = user,
            session = tokenService.issueSession(AuthenticatedPrincipal(subject = user.id.value.toString())),
        )
    }

    fun refreshSession(refreshToken: String): IssuedAuthSession {
        val principal =
            runCatching { tokenService.authenticateRefreshToken(refreshToken) }
                .getOrElse { throw InvalidRefreshTokenException(cause = it) }

        val userId =
            principal.subject.toUserIdOrNull()
                ?: throw InvalidRefreshTokenException()

        val user =
            transactionRunner.runInTransaction {
                userRepository.findByIds(listOf(userId)).firstOrNull()
                    ?: throw InvalidRefreshTokenException()
            }

        return tokenService.issueSession(AuthenticatedPrincipal(subject = user.id.value.toString()))
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
