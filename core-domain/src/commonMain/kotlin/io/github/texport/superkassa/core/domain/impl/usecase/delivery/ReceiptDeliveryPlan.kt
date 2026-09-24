package io.github.texport.superkassa.core.domain.impl.usecase.delivery

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTask
import io.github.texport.superkassa.core.domain.api.model.receipt.CustomerContact
import io.github.texport.superkassa.core.domain.api.model.settings.DeliveryChannelSettings
import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings

/**
 * Какие задачи доставки ставит чек: по одной на канал и вид нагрузки
 * из настроек доставки.
 *
 * Печать ставится, если у принтера есть адрес в сети: иначе чек печатает
 * само приложение. Цифровой канал ставится на контакт покупателя из чека
 * того вида, что канал понимает: телефон — SMS и WhatsApp, почта — почта,
 * Telegram — Telegram. Нет такого контакта — нет и задачи: покупатель
 * не просил чек в этот канал, и «не удалось» в журнале было бы неправдой.
 * Прежде получатель брался из настроек канала — один номер на все чеки,
 * и чек уходил не покупателю. Ссылка ставится только тогда, когда БФД её дал.
 *
 * @param settings текущие настройки доставки; `null` — доставлять некуда. Читаются
 * при каждой постановке: включённый канал действует без перезапуска кассы.
 */
class ReceiptDeliveryPlan(private val settings: () -> DeliverySettings?) {

    /**
     * Задачи документа [documentId] кассы [kkmId], поставленные в [now].
     *
     * @param hasLink дал ли БФД ссылку на чек.
     * @param contact контакт покупателя из чека.
     */
    fun tasksFor(
        kkmId: String,
        documentId: String,
        hasLink: Boolean,
        now: Long,
        contact: CustomerContact?
    ): List<DeliveryTask> {
        val delivery = settings() ?: return emptyList()
        val routes = printRoutes(delivery) + delivery.channels.filter { it.enabled }.flatMap { routes(it, hasLink, contact) }
        return routes.map { route ->
            DeliveryTask(
                id = DeliveryTask.idOf(documentId, route.channel, route.payloadType),
                kkmId = kkmId,
                documentId = documentId,
                channel = route.channel,
                destination = route.destination,
                payloadType = route.payloadType,
                nextAttemptAt = now,
                createdAt = now
            )
        }
    }

    private fun printRoutes(delivery: DeliverySettings): List<Route> {
        val print = delivery.print?.takeIf { it.enabled } ?: return emptyList()
        val connection = print.connection
        if (connection?.host == null || connection.port == null) return emptyList()
        return listOf(Route(PRINT, null, ESC_POS))
    }

    private fun routes(settings: DeliveryChannelSettings, hasLink: Boolean, contact: CustomerContact?): List<Route> {
        val destination = contact?.destinationFor(settings.channel) ?: return emptyList()
        val link = Route(settings.channel, destination, LINK).takeIf { hasLink }
        val document = Route(settings.channel, destination, settings.documentFormat.uppercase())
        return when (settings.payloadType.uppercase()) {
            LINK -> listOfNotNull(link)
            DOCUMENT -> listOf(document)
            BOTH -> listOfNotNull(link, document)
            else -> emptyList()
        }
    }

    private class Route(val channel: String, val destination: String?, val payloadType: String)

    /** Каналы и виды нагрузки, которые знает доставка ядра. */
    companion object {
        /** Канал печати на принтер чеков. */
        const val PRINT: String = "PRINT"

        /** Нагрузка: ссылка на чек у БФД. */
        const val LINK: String = "LINK"

        /** Нагрузка: чек в командах принтера. */
        const val ESC_POS: String = "ESC_POS"

        private const val DOCUMENT = "DOCUMENT"
        private const val BOTH = "BOTH"
    }
}
