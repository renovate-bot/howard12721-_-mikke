package jp.xhw.mikke.services.notification

import jp.xhw.mikke.platform.grpc.grpcServer
import jp.xhw.mikke.platform.grpc.startAndAwait

fun main() {
    grpcServer(
        serviceName = "notification-service",
        portEnv = "NOTIFICATION_SERVICE_PORT",
        defaultPort = 50057,
    ).startAndAwait()
}
