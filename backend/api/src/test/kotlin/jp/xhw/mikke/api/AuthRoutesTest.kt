package jp.xhw.mikke.api

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import jp.xhw.mikke.api.auth.application.*
import jp.xhw.mikke.api.auth.presentation.LoginResponse
import jp.xhw.mikke.api.bootstrap.ApiDependencies
import jp.xhw.mikke.api.http.ApiErrorResponse
import jp.xhw.mikke.api.http.ApiHttpException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.ConnectException

class AuthRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `login returns session from identity client`() =
        testApplication {
            var capturedLoginId: String? = null
            var capturedPassword: String? = null

            application {
                apiModule(
                    dependencies =
                        ApiDependencies(
                            authApiService =
                                AuthApiService(
                                    identityAuthGateway =
                                        object : IdentityAuthGateway {
                                            override suspend fun login(command: LoginCommand): LoginResult {
                                                capturedLoginId = command.loginId
                                                capturedPassword = command.password
                                                return LoginResult(
                                                    user =
                                                        AuthenticatedUser(
                                                            id = "user-1",
                                                            email = "alice@example.com",
                                                            username = "alice",
                                                            displayName = "Alice",
                                                            status = "active",
                                                            createdAt = "2026-04-21T00:00:00Z",
                                                            updatedAt = "2026-04-21T00:00:00Z",
                                                        ),
                                                    session =
                                                        AuthSession(
                                                            accessToken = "access-token",
                                                            refreshToken = "refresh-token",
                                                            accessTokenExpiresAt = "2026-04-21T01:00:00Z",
                                                            refreshTokenExpiresAt = "2026-04-28T00:00:00Z",
                                                        ),
                                                )
                                            }
                                        },
                                ),
                        ),
                )
            }

            val response =
                client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"loginId":" alice@example.com ","password":"secret"}""")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("alice@example.com", capturedLoginId)
            assertEquals("secret", capturedPassword)

            val body = json.decodeFromString<LoginResponse>(response.bodyAsText())
            assertEquals("access-token", body.session.accessToken)
            assertEquals("alice", body.user.username)
        }

    @Test
    fun `login returns unauthorized when identity client rejects credentials`() =
        testApplication {
            application {
                apiModule(
                    dependencies =
                        ApiDependencies(
                            authApiService =
                                AuthApiService(
                                    identityAuthGateway =
                                        object : IdentityAuthGateway {
                                            override suspend fun login(command: LoginCommand): LoginResult =
                                                throw ApiHttpException(
                                                    status = HttpStatusCode.Unauthorized,
                                                    message = "Invalid credentials",
                                                )
                                        },
                                ),
                        ),
                )
            }

            val response =
                client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"loginId":"alice@example.com","password":"bad-password"}""")
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)

            val body = json.decodeFromString<ApiErrorResponse>(response.bodyAsText())
            assertEquals("Invalid credentials", body.message)
        }

    @Test
    fun `login returns service unavailable when upstream is unavailable`() =
        testApplication {
            application {
                apiModule(
                    dependencies =
                        ApiDependencies(
                            authApiService =
                                AuthApiService(
                                    identityAuthGateway =
                                        object : IdentityAuthGateway {
                                            override suspend fun login(command: LoginCommand): LoginResult =
                                                throw ApiHttpException(
                                                    status = HttpStatusCode.ServiceUnavailable,
                                                    message = "Identity service is unavailable",
                                                )
                                        },
                                ),
                        ),
                )
            }

            val response =
                client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"loginId":"alice@example.com","password":"secret"}""")
                }

            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)

            val body = json.decodeFromString<ApiErrorResponse>(response.bodyAsText())
            assertEquals("Identity service is unavailable", body.message)
        }

    @Test
    fun `login rejects blank login id before calling identity client`() =
        testApplication {
            var called = false

            application {
                apiModule(
                    dependencies =
                        ApiDependencies(
                            authApiService =
                                AuthApiService(
                                    identityAuthGateway =
                                        object : IdentityAuthGateway {
                                            override suspend fun login(command: LoginCommand): LoginResult {
                                                called = true
                                                error("should not be called")
                                            }
                                        },
                                ),
                        ),
                )
            }

            val response =
                client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"loginId":"   ","password":"secret"}""")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertFalse(called)
            assertTrue(response.bodyAsText().contains("loginId is required"))
        }

    @Test
    fun `login returns internal server error for unexpected exception`() =
        testApplication {
            application {
                apiModule(
                    dependencies =
                        ApiDependencies(
                            authApiService =
                                AuthApiService(
                                    identityAuthGateway =
                                        object : IdentityAuthGateway {
                                            override suspend fun login(command: LoginCommand): LoginResult =
                                                throw ConnectException("Connection refused")
                                        },
                                ),
                        ),
                )
            }

            val response =
                client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"loginId":"alice@example.com","password":"secret"}""")
                }

            assertEquals(HttpStatusCode.InternalServerError, response.status)

            val body = json.decodeFromString<ApiErrorResponse>(response.bodyAsText())
            assertEquals("Connection refused", body.message)
        }
}
