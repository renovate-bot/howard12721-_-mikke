package jp.xhw.mikke.platform.redis

import io.lettuce.core.Consumer
import io.lettuce.core.Limit
import io.lettuce.core.Range
import io.lettuce.core.StreamMessage
import io.lettuce.core.XAutoClaimArgs
import io.lettuce.core.XGroupCreateArgs
import io.lettuce.core.XReadArgs
import io.lettuce.core.api.sync.RedisStreamCommands
import io.lettuce.core.models.stream.PendingMessage
import java.time.Duration

data class RedisStreamRecord(
    val id: String,
    val fields: Map<String, String>,
)

class RedisStreamProducer(
    private val commands: RedisStreamCommands<String, String>,
    private val streamName: String,
) {
    fun append(fields: Map<String, String>): String = commands.xadd(streamName, fields)
}

class RedisStreamConsumerGroup(
    private val commands: RedisStreamCommands<String, String>,
    private val streamName: String,
    val consumerGroup: String,
    private val consumerName: String,
) {
    fun ensureGroup(startId: String = "0") {
        try {
            commands.xgroupCreate(
                XReadArgs.StreamOffset.from(streamName, startId),
                consumerGroup,
                XGroupCreateArgs().mkstream(true),
            )
        } catch (_: Exception) {
            // Group already exists.
        }
    }

    fun read(
        count: Long = 10,
        block: Duration = Duration.ofSeconds(1),
    ): List<RedisStreamRecord> =
        commands
            .xreadgroup(
                Consumer.from(consumerGroup, consumerName),
                XReadArgs.Builder.count(count).block(block.toMillis()),
                XReadArgs.StreamOffset.lastConsumed(streamName),
            ).map(::toRecord)

    fun ack(messageId: String): Long = commands.xack(streamName, consumerGroup, messageId)

    fun pendingSummary(): Long = commands.xpending(streamName, consumerGroup).count

    fun pendingMessages(count: Long = 10): List<PendingMessage> =
        commands.xpending(
            streamName,
            consumerGroup,
            Range.create("-", "+"),
            Limit.create(0, count),
        )

    fun claimStale(
        minIdle: Duration,
        count: Long = 10,
    ): List<RedisStreamRecord> {
        val claimed =
            commands.xautoclaim(
                streamName,
                XAutoClaimArgs<String>()
                    .minIdleTime(minIdle)
                    .startId("0-0")
                    .count(count)
                    .consumer(Consumer.from(consumerGroup, consumerName)),
            )

        return claimed.messages.map(::toRecord)
    }
}

private fun toRecord(message: StreamMessage<String, String>): RedisStreamRecord =
    RedisStreamRecord(
        id = message.id,
        fields = message.body,
    )
