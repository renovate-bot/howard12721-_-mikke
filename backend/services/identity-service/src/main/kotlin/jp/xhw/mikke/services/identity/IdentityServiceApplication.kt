package jp.xhw.mikke.services.identity

import jp.xhw.mikke.platform.auth.grpc.GrpcAuthServerInterceptor
import jp.xhw.mikke.platform.auth.grpc.bearerToken
import jp.xhw.mikke.platform.auth.jwt.JwtTokenService
import jp.xhw.mikke.platform.database.connectMariaDbFromEnv
import jp.xhw.mikke.platform.database.exposed.ExposedTransactionRunner
import jp.xhw.mikke.platform.grpc.grpcServer
import jp.xhw.mikke.platform.grpc.installGrpcHealth
import jp.xhw.mikke.platform.grpc.startAndAwait
import jp.xhw.mikke.services.identity.application.IdentityService
import jp.xhw.mikke.services.identity.application.PasswordHasher
import jp.xhw.mikke.services.identity.infrastructure.ExposedIdentityUserRepository

fun main() {
    val passwordHasher = PasswordHasher()
    val database = connectMariaDbFromEnv(defaultDatabase = "identity_service")
    val userRepository = ExposedIdentityUserRepository()
    val transactionRunner = ExposedTransactionRunner(database)
    val tokenService = JwtTokenService(secret = System.getenv("IDENTITY_JWT_SECRET") ?: "dev-identity-secret")
    val identityApplicationService =
        IdentityService(
            userRepository = userRepository,
            transactionRunner = transactionRunner,
            passwordHasher = passwordHasher,
            tokenService = tokenService,
        )
    val identityService = IdentityServiceRpc(identityService = identityApplicationService)

    grpcServer(serviceName = "identity-service", portEnv = "IDENTITY_SERVICE_PORT", defaultPort = 50051) {
        installGrpcHealth(serviceName = "identity-service")
        intercept(
            GrpcAuthServerInterceptor(
                authenticator = { headers ->
                    headers.bearerToken()?.let(tokenService::authenticateAccessToken)
                },
                optional = true,
            ),
        )
        addService(identityService)
    }.startAndAwait()
}
