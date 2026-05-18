package jp.xhw.mikke.platform.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection

fun connectRedisFromEnv(
    defaultHost: String = "localhost",
    defaultPort: Int = 6379,
): StatefulRedisConnection<String, String> {
    val host = System.getenv("REDIS_HOST") ?: defaultHost
    val port = System.getenv("REDIS_PORT")?.toIntOrNull() ?: defaultPort
    val password = System.getenv("REDIS_PASSWORD")?.takeIf { it.isNotEmpty() }

    val uriBuilder =
        RedisURI
            .builder()
            .withHost(host)
            .withPort(port)

    password?.let(uriBuilder::withPassword)

    return RedisClient.create(uriBuilder.build()).connect()
}
