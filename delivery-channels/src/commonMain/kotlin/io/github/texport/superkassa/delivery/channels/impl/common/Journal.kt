package io.github.texport.superkassa.delivery.channels.impl.common

/**
 * Журнал каналов доставки.
 *
 * Свой узкий журнал, а не журнал платформы напрямую: всё, что пишут
 * каналы, проходит через [MaskedJournal], и проверка видит ровно то,
 * что ушло бы в журнал платформы.
 */
internal interface Journal {
    fun info(line: String)
    fun warn(line: String)
}

/** Журнал платформы: SLF4J на JVM и Android, консоль на iOS. */
internal expect fun platformJournal(name: String): Journal

/** Журнал, в который ключи каналов не проходят: каждая строка маскируется перед записью. */
internal class MaskedJournal(private val journal: Journal, private val secrets: Secrets) : Journal {
    override fun info(line: String) = journal.info(secrets.mask(line))

    override fun warn(line: String) = journal.warn(secrets.mask(line))
}
