package jp.xhw.mikke.platform.database

interface TransactionRunner {
    fun <T> runInTransaction(block: () -> T): T
}
