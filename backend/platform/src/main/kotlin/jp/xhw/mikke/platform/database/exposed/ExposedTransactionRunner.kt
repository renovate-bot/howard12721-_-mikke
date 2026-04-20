package jp.xhw.mikke.platform.database.exposed

import jp.xhw.mikke.platform.database.TransactionRunner
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedTransactionRunner(
    private val database: Database,
) : TransactionRunner {
    override fun <T> runInTransaction(block: () -> T): T = transaction(database) { block() }
}
