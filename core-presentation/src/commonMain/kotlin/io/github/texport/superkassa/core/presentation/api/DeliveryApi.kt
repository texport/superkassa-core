package io.github.texport.superkassa.core.presentation.api

import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryResponse

/**
 * Доставка чека покупателю: что с ней сейчас, повтор вручную и фоновая досылка.
 *
 * Доставка ставится задачами, когда БФД принял чек, и уходит в фоне —
 * ответ кассиру её не ждёт. Все вызовы блокирующие: из главного потока
 * приложения их не делают.
 */
interface DeliveryApi {
    /**
     * Повторить доставку чека и ответить, дошёл ли он по каждому каналу.
     *
     * Повтор идёт через те же задачи, что и фон, и отправляет сразу;
     * доставленное второй раз не уходит.
     *
     * @param kkmId ID ККМ.
     * @param documentId ID фискального документа.
     * @param pin ПИН-код кассира или администратора.
     * @return пары «канал — доставлен ли».
     * @throws Exception документа нет, он не фискальный или ни один канал не настроен.
     */
    @Throws(Exception::class)
    fun retryReceiptDelivery(kkmId: String, documentId: String, pin: String): List<Pair<String, Boolean>>

    /**
     * То же, что [retryReceiptDelivery], но с состоянием каждого канала и причиной отказа.
     *
     * @return доставка по каналам после повтора.
     * @throws Exception документа нет, он не фискальный, ни один канал не настроен
     *   или хранилище задач доставки не держит.
     */
    @Throws(Exception::class)
    fun resendReceipt(kkmId: String, documentId: String, pin: String): List<ReceiptDeliveryResponse>

    /**
     * Доставка чека по каналам для журнала: ждёт, доставлен, не удалось — с кодом и причиной.
     *
     * @param kkmId ID ККМ.
     * @param documentId ID фискального документа.
     * @param pin ПИН-код кассира или администратора.
     * @return доставка по каналам; пусто — доставка чека не заказывалась.
     * @throws Exception документа нет или хранилище задач доставки не держит.
     */
    @Throws(Exception::class)
    fun receiptDeliveries(kkmId: String, documentId: String, pin: String): List<ReceiptDeliveryResponse>

    /**
     * Один заход фоновой досылки: отправляет не больше [limit] задач, срок которых наступил.
     *
     * Для того, кто ведёт фон: встраиваемая сборка зовёт его сама, узел — по своему расписанию.
     *
     * @return сколько задач отправлено, с успехом или отказом.
     * @throws Exception хранилище задач доставки не держит.
     */
    @Throws(Exception::class)
    fun sendDueDeliveries(limit: Int): Int
}
