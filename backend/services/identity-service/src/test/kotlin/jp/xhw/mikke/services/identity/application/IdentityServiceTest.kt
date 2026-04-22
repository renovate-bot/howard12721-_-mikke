package jp.xhw.mikke.services.identity.application

import jp.xhw.mikke.platform.auth.jwt.JwtTokenService
import jp.xhw.mikke.platform.database.TransactionRunner
import jp.xhw.mikke.services.identity.model.Email
import jp.xhw.mikke.services.identity.model.IdentityUser
import jp.xhw.mikke.services.identity.model.UserId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IdentityServiceTest {
    @Test
    fun `register rejects weak password`() {
        val repository = RecordingIdentityUserRepository()
        val service =
            IdentityService(
                userRepository = repository,
                transactionRunner = ImmediateTransactionRunner,
                passwordHasher = PasswordHasher(),
                tokenService = JwtTokenService(secret = "test-secret"),
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
        val service =
            IdentityService(
                userRepository = repository,
                transactionRunner = ImmediateTransactionRunner,
                passwordHasher = PasswordHasher(),
                tokenService = JwtTokenService(secret = "test-secret"),
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

    override fun findByIds(ids: List<UserId>): List<IdentityUser> = emptyList()
}
