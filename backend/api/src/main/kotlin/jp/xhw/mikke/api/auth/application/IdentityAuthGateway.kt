package jp.xhw.mikke.api.auth.application

interface IdentityAuthGateway : AutoCloseable {
    suspend fun login(command: LoginCommand): LoginResult

    override fun close() {}
}
