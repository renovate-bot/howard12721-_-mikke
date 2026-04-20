package jp.xhw.mikke.events.core

interface DomainEvent {
    val eventType: String
    val eventVersion: Int
        get() = 1
}
