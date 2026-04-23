package jp.xhw.mikke.services.identity.infrastructure

import jp.xhw.mikke.platform.database.exposed.isUniqueConstraintViolation
import jp.xhw.mikke.services.identity.application.DuplicateIdentityUserException
import jp.xhw.mikke.services.identity.application.IdentityUserRepository
import jp.xhw.mikke.services.identity.model.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Instant
import kotlin.uuid.Uuid

class ExposedIdentityUserRepository : IdentityUserRepository {
    override fun saveUser(user: IdentityUser) {
        try {
            IdentityUsersTable.insert { row ->
                row[id] = user.id.value.toString()
                row[email] = user.email.value
                row[normalizedEmail] = user.email.value.normalizeEmail()
                row[username] = user.username.value
                row[normalizedUsername] = user.username.value.normalizeUsername()
                row[displayName] = user.displayName.value
                row[passwordHashIterations] = user.passwordHash.iterations
                row[passwordHash] = user.passwordHash.hash
                row[passwordSalt] = user.passwordHash.salt
                row[createdAt] = user.createdAt.toJavaInstant()
                row[updatedAt] = user.updatedAt.toJavaInstant()
            }
        } catch (e: ExposedSQLException) {
            if (e.isUniqueConstraintViolation()) {
                throw DuplicateIdentityUserException(cause = e)
            }
            throw e
        }
    }

    override fun findByLogin(login: String): IdentityUser? {
        val trimmedLogin = login.trim()
        if (trimmedLogin.isEmpty()) {
            return null
        }

        return IdentityUsersTable
            .selectAll()
            .where {
                (IdentityUsersTable.normalizedEmail eq trimmedLogin.normalizeEmail()) or
                    (IdentityUsersTable.normalizedUsername eq trimmedLogin.normalizeUsername())
            }.limit(1)
            .singleOrNull()
            ?.toIdentityUser()
    }

    override fun findByEmails(emails: List<Email>): List<IdentityUser> {
        if (emails.isEmpty()) {
            return emptyList()
        }

        val byEmail =
            IdentityUsersTable
                .selectAll()
                .where {
                    IdentityUsersTable.normalizedEmail inList emails.map { it.value.normalizeEmail() }.distinct()
                }.map { it.toIdentityUser() }
                .associateBy { it.email.value.normalizeEmail() }

        return emails.mapNotNull { byEmail[it.value.normalizeEmail()] }
    }

    override fun findByIds(ids: List<UserId>): List<IdentityUser> {
        if (ids.isEmpty()) {
            return emptyList()
        }

        val byId =
            IdentityUsersTable
                .selectAll()
                .where { IdentityUsersTable.id inList ids.map { it.value.toString() }.distinct() }
                .map { it.toIdentityUser() }
                .associateBy { it.id.value.toString() }

        return ids.mapNotNull { byId[it.value.toString()] }
    }
}

private object IdentityUsersTable : Table("identity_users") {
    val id = varchar("id", length = 36)
    val email = varchar("email", length = 255)
    val normalizedEmail = varchar("normalized_email", length = 255).uniqueIndex()
    val username = varchar("username", length = 32)
    val normalizedUsername = varchar("normalized_username", length = 32).uniqueIndex()
    val displayName = varchar("display_name", length = 255)
    val passwordHashIterations = integer("password_hash_iterations")
    val passwordHash = varchar("password_hash", length = 512)
    val passwordSalt = varchar("password_salt", length = 512)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

private fun ResultRow.toIdentityUser(): IdentityUser =
    IdentityUser(
        id = UserId(Uuid.parse(this[IdentityUsersTable.id])),
        email = Email(this[IdentityUsersTable.email]),
        username = Username(this[IdentityUsersTable.username]),
        displayName = DisplayName(this[IdentityUsersTable.displayName]),
        passwordHash =
            PasswordHash(
                iterations = this[IdentityUsersTable.passwordHashIterations],
                hash = this[IdentityUsersTable.passwordHash],
                salt = this[IdentityUsersTable.passwordSalt],
            ),
        createdAt = this[IdentityUsersTable.createdAt].toKotlinInstant(),
        updatedAt = this[IdentityUsersTable.updatedAt].toKotlinInstant(),
    )

private fun String.normalizeEmail(): String = trim().lowercase()

private fun String.normalizeUsername(): String = trim().lowercase()

private fun Instant.toJavaInstant(): java.time.Instant = java.time.Instant.ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong())

private fun java.time.Instant.toKotlinInstant(): Instant = Instant.fromEpochSeconds(epochSecond, nano)
