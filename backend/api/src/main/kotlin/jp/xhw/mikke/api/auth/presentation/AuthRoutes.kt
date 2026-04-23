package jp.xhw.mikke.api.auth.presentation

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import jp.xhw.mikke.api.auth.application.*

fun Route.installAuthRoutes(authApiService: AuthApiService) {
    route("/api/v1/auth") {
        post("/login") {
            val request = call.receive<LoginRequest>()
            val result =
                authApiService.login(
                    LoginCommand(
                        loginId = request.loginId,
                        password = request.password,
                    ),
                )

            call.respond(
                status = HttpStatusCode.OK,
                message = result.toResponse(),
            )
        }

        post("/register") {
            val request = call.receive<RegisterRequest>()
            val result =
                authApiService.register(
                    RegisterCommand(
                        email = request.email,
                        username = request.username,
                        displayName = request.displayName,
                        password = request.password,
                    ),
                )

            call.respond(
                status = HttpStatusCode.OK,
                message = result.toResponse(),
            )
        }

        post("/refresh") {
            val request = call.receive<RefreshRequest>()
            val result =
                authApiService.refresh(
                    RefreshCommand(refreshToken = request.refreshToken),
                )

            call.respond(
                status = HttpStatusCode.OK,
                message = result.toResponse(),
            )
        }

        post("/logout") {
            val request = call.receive<LogoutRequest>()
            authApiService.logout(
                LogoutCommand(refreshToken = request.refreshToken),
            )

            call.respond(HttpStatusCode.NoContent)
        }
    }
}
