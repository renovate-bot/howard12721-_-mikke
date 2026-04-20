package jp.xhw.mikke.services.feed

import jp.xhw.mikke.platform.grpc.grpcServer
import jp.xhw.mikke.platform.grpc.startAndAwait

fun main() {
    grpcServer(serviceName = "feed-service", portEnv = "FEED_SERVICE_PORT", defaultPort = 50056).startAndAwait()
}
