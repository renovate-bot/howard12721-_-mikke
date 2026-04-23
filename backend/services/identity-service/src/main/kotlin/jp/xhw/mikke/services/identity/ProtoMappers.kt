package jp.xhw.mikke.services.identity

import com.google.protobuf.Timestamp
import jp.xhw.mikke.identity.v1.AuthSession
import jp.xhw.mikke.identity.v1.User
import jp.xhw.mikke.identity.v1.UserStatus
import jp.xhw.mikke.platform.auth.IssuedAuthSession
import jp.xhw.mikke.services.identity.model.IdentityUser
import kotlin.time.Instant

fun IdentityUser.toProto(): User =
    User
        .newBuilder()
        .setId(id.value.toString())
        .setEmail(email.value)
        .setUsername(username.value)
        .setDisplayName(displayName.value)
        .setStatus(UserStatus.USER_STATUS_ACTIVE)
        .setCreatedAt(createdAt.toProtoTimestamp())
        .setUpdatedAt(updatedAt.toProtoTimestamp())
        .build()

fun IssuedAuthSession.toProto(): AuthSession =
    AuthSession
        .newBuilder()
        .setAccessToken(accessToken.value)
        .setRefreshToken(refreshToken.value)
        .setAccessTokenExpiresAt(accessToken.expiresAt.toProtoTimestamp())
        .setRefreshTokenExpiresAt(refreshToken.expiresAt.toProtoTimestamp())
        .build()

private fun Instant.toProtoTimestamp(): Timestamp =
    Timestamp
        .newBuilder()
        .setSeconds(epochSeconds)
        .setNanos(nanosecondsOfSecond)
        .build()
