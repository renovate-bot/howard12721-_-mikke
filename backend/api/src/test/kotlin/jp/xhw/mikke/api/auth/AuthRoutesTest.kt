package jp.xhw.mikke.api.auth

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import jp.xhw.mikke.api.apiModule
import jp.xhw.mikke.api.auth.application.*
import jp.xhw.mikke.api.auth.presentation.LoginResponse
import jp.xhw.mikke.api.auth.presentation.RegisterResponse
import jp.xhw.mikke.api.bootstrap.ApiDependencies
import jp.xhw.mikke.api.http.ApiErrorResponse
import jp.xhw.mikke.api.http.ApiHttpException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.ConnectException

class AuthRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Nested
    inner class Login {
        @Test
        fun `returns session from identity client`() =
            testApplicationWithAuthGateway(
                RecordingIdentityAuthGateway(
                    onLogin = { command ->
                        capturedLoginCommand = command
                        sampleLoginResult()
                    },
                ),
            ) {
                val response = login("""{"loginId":" alice@example.com ","password":"secret"}""")

                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals(
                    LoginCommand(
                        loginId = "alice@example.com",
                        password = "secret",
                    ),
                    capturedLoginCommand,
                )

                val body = json.decodeFromString<LoginResponse>(response.bodyAsText())
                assertEquals("access-token", body.session.accessToken)
                assertEquals("alice", body.user.username)
            }

        @Test
        fun `returns unauthorized when identity client rejects credentials`() =
            testApplicationWithAuthGateway(
                RecordingIdentityAuthGateway(
                    onLogin = {
                        throw ApiHttpException(
                            status = HttpStatusCode.Unauthorized,
                            message = "Invalid credentials",
                        )
                    },
                ),
            ) {
                val response = login("""{"loginId":"alice@example.com","password":"bad-password"}""")

                assertEquals(HttpStatusCode.Unauthorized, response.status)

                val body = json.decodeFromString<ApiErrorResponse>(response.bodyAsText())
                assertEquals("Invalid credentials", body.message)
            }

        @Test
        fun `returns service unavailable when upstream is unavailable`() =
            testApplicationWithAuthGateway(
                RecordingIdentityAuthGateway(
                    onLogin = {
                        throw ApiHttpException(
                            status = HttpStatusCode.ServiceUnavailable,
                            message = "Identity service is unavailable",
                        )
                    },
                ),
            ) {
                val response = login("""{"loginId":"alice@example.com","password":"secret"}""")

                assertEquals(HttpStatusCode.ServiceUnavailable, response.status)

                val body = json.decodeFromString<ApiErrorResponse>(response.bodyAsText())
                assertEquals("Identity service is unavailable", body.message)
            }

        @Test
        fun `rejects blank login id before calling identity client`() =
            testApplicationWithAuthGateway(
                RecordingIdentityAuthGateway(
                    onLogin = {
                        loginCalled = true
                        error("should not be called")
                    },
                ),
            ) {
                val response = login("""{"loginId":"   ","password":"secret"}""")

                assertEquals(HttpStatusCode.BadRequest, response.status)
                assertFalse(loginCalled)
                assertTrue(response.bodyAsText().contains("loginId is required"))
            }

        @Test
        fun `returns internal server error for unexpected exception`() =
            testApplicationWithAuthGateway(
                RecordingIdentityAuthGateway(
                    onLogin = { throw ConnectException("Connection refused") },
                ),
            ) {
                val response = login("""{"loginId":"alice@example.com","password":"secret"}""")

                assertEquals(HttpStatusCode.InternalServerError, response.status)

                val body = json.decodeFromString<ApiErrorResponse>(response.bodyAsText())
                assertEquals("Connection refused", body.message)
            }
    }

    @Nested
    inner class Register {
        @Test
        fun `returns session from identity client`() =
            testApplicationWithAuthGateway(
                RecordingIdentityAuthGateway(
                    onRegister = { command ->
                        capturedRegisterCommand = command
                        sampleRegisterResult()
                    },
                ),
            ) {
                val response =
                    register(
                        """
                        {"email":" alice@example.com ","username":" alice ","displayName":" Alice ","password":" password123 "}
                        """.trimIndent(),
                    )

                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals(
                    RegisterCommand(
                        email = "alice@example.com",
                        username = "alice",
                        displayName = "Alice",
                        password = "password123",
                    ),
                    capturedRegisterCommand,
                )

                val body = json.decodeFromString<RegisterResponse>(response.bodyAsText())
                assertEquals("access-token", body.session.accessToken)
                assertEquals("alice", body.user.username)
            }

        @Test
        fun `rejects weak password before calling identity client`() =
            testApplicationWithAuthGateway(
                RecordingIdentityAuthGateway(
                    onRegister = {
                        registerCalled = true
                        error("should not be called")
                    },
                ),
            ) {
                val response =
                    register(
                        """
                        {"email":"alice@example.com","username":"alice","displayName":"Alice","password":"password"}
                        """.trimIndent(),
                    )

                assertEquals(HttpStatusCode.BadRequest, response.status)
                assertFalse(registerCalled)

                val body = json.decodeFromString<ApiErrorResponse>(response.bodyAsText())
                assertEquals(
                    "password must be at least 8 characters and include at least one letter and one digit",
                    body.message,
                )
            }
    }

    private var capturedLoginCommand: LoginCommand? = null
    private var capturedRegisterCommand: RegisterCommand? = null
    private var loginCalled: Boolean = false
    private var registerCalled: Boolean = false

    private fun testApplicationWithAuthGateway(
        identityAuthGateway: IdentityAuthGateway,
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) = testApplication {
        resetCaptures()

        application {
            apiModule(
                dependencies =
                    ApiDependencies(
                        authApiService = AuthApiService(identityAuthGateway = identityAuthGateway),
                    ),
            )
        }

        block()
    }

    private fun resetCaptures() {
        capturedLoginCommand = null
        capturedRegisterCommand = null
        loginCalled = false
        registerCalled = false
    }

    private suspend fun ApplicationTestBuilder.login(body: String): HttpResponse =
        client.post("/api/v1/auth/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(body)
        }

    private suspend fun ApplicationTestBuilder.register(body: String): HttpResponse =
        client.post("/api/v1/auth/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(body)
        }

    private fun sampleLoginResult(): LoginResult =
        LoginResult(
            user = sampleUser(),
            session = sampleSession(),
        )

    private fun sampleRegisterResult(): RegisterResult =
        RegisterResult(
            user = sampleUser(),
            session = sampleSession(),
        )

    private fun sampleUser(): AuthenticatedUser =
        AuthenticatedUser(
            id = "user-1",
            email = "alice@example.com",
            username = "alice",
            displayName = "Alice",
            status = "active",
            createdAt = "2026-04-21T00:00:00Z",
            updatedAt = "2026-04-21T00:00:00Z",
        )

    private fun sampleSession(): AuthSession =
        AuthSession(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            accessTokenExpiresAt = "2026-04-21T01:00:00Z",
            refreshTokenExpiresAt = "2026-04-28T00:00:00Z",
        )
}

private class RecordingIdentityAuthGateway(
    private val onLogin: suspend (LoginCommand) -> LoginResult = { error("Not implemented") },
    private val onRegister: suspend (RegisterCommand) -> RegisterResult = { error("Not implemented") },
) : IdentityAuthGateway {
    override suspend fun login(command: LoginCommand): LoginResult = onLogin(command)

    override suspend fun register(command: RegisterCommand): RegisterResult = onRegister(command)
}
