package io.github.texport.superkassa.delivery.channels.impl.common

internal actual fun platformJournal(name: String): Journal = PlatformJournal(name)

/** Журнал каналов в консоль: на iOS его собирает журнал устройства. */
internal class PlatformJournal(private val name: String) : Journal {
    override fun info(line: String) = println("INFO [$name]: $line")

    override fun warn(line: String) = println("WARN [$name]: $line")
}
