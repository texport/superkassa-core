package io.github.texport.superkassa.core.presentation.api

/**
 * Интерфейс API управления доставкой фискальных документов.
 *
 * Предоставляет методы для повторной отправки документов клиентам по различным каналам.
 */
interface DeliveryApi {
    /**
     * Повторить попытку отправки чека в ОФД по доступным каналам.
     *
     * @param kkmId ID ККМ.
     * @param documentId ID фискального документа.
     * @param pin ПИН-код пользователя.
     * @return Список результатов по каналам (название канала, успех/ошибка).
     */
    fun retryReceiptDelivery(kkmId: String, documentId: String, pin: String): List<Pair<String, Boolean>>
}
