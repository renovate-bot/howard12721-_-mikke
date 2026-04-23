package jp.xhw.mikke.services.identity.application

import jp.xhw.mikke.platform.auth.jwt.JwtTokenService
import jp.xhw.mikke.platform.database.TransactionRunner
import jp.xhw.mikke.services.identity.model.Email
import jp.xhw.mikke.services.identity.model.IdentityUser
import jp.xhw.mikke.services.identity.model.RefreshSession
import jp.xhw.mikke.services.identity.model.RefreshSessionId
import jp.xhw.mikke.services.identity.model.UserId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Instant

class IdentityServiceTest {
    @Test
    fun `register rejects weak password`() {
        val repository = RecordingIdentityUserRepository()
        val refreshSessionRepository = RecordingRefreshSessionRepository()
        val service =
            IdentityService(
                userRepository = repository,
                refreshSessionRepository = refreshSessionRepository,
                transactionRunner = ImmediateTransactionRunner,
                passwordHasher = PasswordHasher(),
                tokenService = JwtTokenService(secret = "test-secret"),
                refreshSessionTokenService = RefreshSessionTokenService(),
            )

        val exception =
            assertThrows(InvalidIdentityInputException::class.java) {
                service.register(
                    RegisterIdentityUserCommand(
                        email = "alice@example.com",
                        username = "alice",
                        displayName = "Alice",
                        password = "password",
                    ),
                )
            }

        assertEquals(
            "password must be at least 8 characters and include at least one letter and one digit",
            exception.message,
        )
        assertEquals(null, repository.savedUser)
    }

    @Test
    fun `register persists user for valid password`() {
        val repository = RecordingIdentityUserRepository()
        val refreshSessionRepository = RecordingRefreshSessionRepository()
        val service =
            IdentityService(
                userRepository = repository,
                refreshSessionRepository = refreshSessionRepository,
                transactionRunner = ImmediateTransactionRunner,
                passwordHasher = PasswordHasher(),
                tokenService = JwtTokenService(secret = "test-secret"),
                refreshSessionTokenService = RefreshSessionTokenService(),
            )

        val result =
            service.register(
                RegisterIdentityUserCommand(
                    email = "alice@example.com",
                    username = "alice",
                    displayName = "Alice",
                    password = "password123",
                ),
            )

        assertNotNull(repository.savedUser)
        assertEquals("alice@example.com", result.user.email.value)
        assertEquals("alice", result.user.username.value)
        assertTrue(
            result.session.accessToken.value
                .isNotBlank(),
        )
        assertTrue(
            result.session.refreshToken.value
                .isNotBlank(),
        )
        assertEquals(1, refreshSessionRepository.sessions.size)
    }

    @Test
    fun `refresh rotates refresh session and invalidates previous token`() {
        val repository = RecordingIdentityUserRepository()
        val refreshSessionRepository = RecordingRefreshSessionRepository()
        val fixedClock =
            object : Clock {
                override fun now(): Instant = Instant.parse("2026-04-23T00:00:00Z")
            }
        val service =
            IdentityService(
                userRepository = repository,
                refreshSessionRepository = refreshSessionRepository,
                transactionRunner = ImmediateTransactionRunner,
                passwordHasher = PasswordHasher(),
                tokenService = JwtTokenService(secret = "test-secret", clock = fixedClock),
                refreshSessionTokenService = RefreshSessionTokenService(),
                clock = fixedClock,
            )

        val registered =
            service.register(
                RegisterIdentityUserCommand(
                    email = "alice@example.com",
                    username = "alice",
                    displayName = "Alice",
                    password = "password123",
                ),
            )

        val refreshed = service.refreshSession(registered.session.refreshToken.value)

        assertNotEquals(registered.session.refreshToken.value, refreshed.refreshToken.value)
        assertThrows(InvalidRefreshTokenException::class.java) {
            service.refreshSession(registered.session.refreshToken.value)
        }
        assertEquals(2, refreshSessionRepository.sessions.size)
        assertEquals(1, refreshSessionRepository.sessions.count { it.revokedAt != null })
    }

    @Test
    fun `logout revokes refresh session`() {
        val repository = RecordingIdentityUserRepository()
        val refreshSessionRepository = RecordingRefreshSessionRepository()
        val service =
            IdentityService(
                userRepository = repository,
                refreshSessionRepository = refreshSessionRepository,
                transactionRunner = ImmediateTransactionRunner,
                passwordHasher = PasswordHasher(),
                tokenService = JwtTokenService(secret = "test-secret"),
                refreshSessionTokenService = RefreshSessionTokenService(),
            )

        val registered =
            service.register(
                RegisterIdentityUserCommand(
                    email = "alice@example.com",
                    username = "alice",
                    displayName = "Alice",
                    password = "password123",
                ),
            )

        service.logout(registered.session.refreshToken.value)

        assertThrows(InvalidRefreshTokenException::class.java) {
            service.refreshSession(registered.session.refreshToken.value)
        }
    }
}

private object ImmediateTransactionRunner : TransactionRunner {
    override fun <T> runInTransaction(block: () -> T): T = block()
}

private class RecordingIdentityUserRepository : IdentityUserRepository {
    var savedUser: IdentityUser? = null

    override fun saveUser(user: IdentityUser) {
        savedUser = user
    }

    override fun findByLogin(login: String): IdentityUser? = null

    override fun findByEmails(emails: List<Email>): List<IdentityUser> = emptyList()

    override fun findByIds(ids: List<UserId>): List<IdentityUser> {
        val user = savedUser ?: return emptyList()
        return ids.filter { it == user.id }.map { user }
    }
}

private class RecordingRefreshSessionRepository : RefreshSessionRepository {
    val sessions = mutableListOf<RefreshSession>()

    override fun save(session: RefreshSession) {
        sessions += session
    }

    override fun findByRefreshTokenHash(refreshTokenHash: String): RefreshSession? =
        sessions.lastOrNull { it.refreshTokenHash == refreshTokenHash }

    override fun revoke(
        sessionId: RefreshSessionId,
        revokedAt: Instant,
    ): Boolean {
        val index =
            sessions.indexOfFirst {
                it.id == sessionId && it.revokedAt == null
            }
        if (index < 0) {
            return false
        }

        sessions[index] = sessions[index].copy(revokedAt = revokedAt)
        return true
    }

    override fun revokeByRefreshTokenHash(
        refreshTokenHash: String,
        revokedAt: Instant,
    ): Boolean {
        val index =
            sessions.indexOfFirst {
                it.refreshTokenHash == refreshTokenHash && it.revokedAt == null
            }
        if (index < 0) {
            return false
        }

        sessions[index] = sessions[index].copy(revokedAt = revokedAt)
        return true
    }
}
