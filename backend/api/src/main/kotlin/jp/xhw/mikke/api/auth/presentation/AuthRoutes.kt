package jp.xhw.mikke.api.auth.presentation

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import jp.xhw.mikke.api.auth.application.AuthApiService
import jp.xhw.mikke.api.auth.application.LoginCommand

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
    }
}
