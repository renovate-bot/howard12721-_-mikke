package jp.xhw.mikke.platform.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database

fun connectMariaDbFromEnv(
    defaultDatabase: String,
    maximumPoolSize: Int = 10,
    minimumIdle: Int = 1,
): Database {
    val host = System.getenv("MARIADB_HOST") ?: "localhost"
    val port = System.getenv("MARIADB_PORT") ?: "3306"
    val database = System.getenv("MARIADB_DATABASE") ?: defaultDatabase
    val user = System.getenv("MARIADB_USER") ?: "mikke"
    val password = System.getenv("MARIADB_PASSWORD") ?: "mikke"

    val hikariConfig =
        HikariConfig().apply {
            jdbcUrl =
                "jdbc:mariadb://$host:$port/$database" +
                "?useUnicode=true&characterEncoding=utf8&connectionTimeZone=UTC"
            driverClassName = "org.mariadb.jdbc.Driver"
            username = user
            this.password = password
            this.maximumPoolSize = maximumPoolSize
            this.minimumIdle = minimumIdle
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            validate()
        }

    return Database.connect(HikariDataSource(hikariConfig))
}
