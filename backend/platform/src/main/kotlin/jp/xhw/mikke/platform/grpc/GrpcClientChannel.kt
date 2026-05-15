package jp.xhw.mikke.platform.grpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

private const val DNS_TARGET_PREFIX = "dns:///"
private const val ROUND_ROBIN_POLICY = "round_robin"

fun grpcClientChannelFromEnvironment(
    targetEnv: String,
    hostEnv: String,
    portEnv: String,
    defaultHost: String,
    defaultPort: Int,
): ManagedChannel {
    val target =
        System.getenv(targetEnv)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: dnsTarget(
                host =
                    System.getenv(hostEnv)
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?: defaultHost,
                port = System.getenv(portEnv)?.toIntOrNull() ?: defaultPort,
            )

    return grpcRoundRobinChannel(target)
}

fun grpcRoundRobinChannel(target: String): ManagedChannel =
    ManagedChannelBuilder
        .forTarget(target)
        .defaultLoadBalancingPolicy(ROUND_ROBIN_POLICY)
        .usePlaintext()
        .build()

fun dnsTarget(
    host: String,
    port: Int,
): String = "$DNS_TARGET_PREFIX$host:$port"
