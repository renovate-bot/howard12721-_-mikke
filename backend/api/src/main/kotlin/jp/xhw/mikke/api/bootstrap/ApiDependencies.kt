package jp.xhw.mikke.api.bootstrap

import jp.xhw.mikke.api.auth.application.AuthApiService
import jp.xhw.mikke.api.auth.application.IdentityAuthGateway
import jp.xhw.mikke.api.auth.infrastructure.GrpcIdentityAuthGateway

class ApiDependencies(
    val authApiService: AuthApiService,
    private val closeables: List<AutoCloseable> = emptyList(),
) : AutoCloseable {
    override fun close() {
        closeables.forEach(AutoCloseable::close)
    }

    companion object {
        fun fromEnvironment(): ApiDependencies {
            val identityAuthGateway: IdentityAuthGateway = GrpcIdentityAuthGateway.fromEnvironment()

            return ApiDependencies(
                authApiService = AuthApiService(identityAuthGateway = identityAuthGateway),
                closeables = listOf(identityAuthGateway),
            )
        }
    }
}
