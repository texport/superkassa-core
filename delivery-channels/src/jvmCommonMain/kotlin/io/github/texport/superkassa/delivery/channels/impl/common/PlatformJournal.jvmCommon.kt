package io.github.texport.superkassa.delivery.channels.impl.common

import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal actual fun platformJournal(name: String): Journal = PlatformJournal(LoggerFactory.getLogger(name))

/** Журнал каналов в SLF4J: приложение само решает, куда он пишется. */
internal class PlatformJournal(private val logger: Logger) : Journal {
    override fun info(line: String) = logger.info(line)

    override fun warn(line: String) = logger.warn(line)
}
