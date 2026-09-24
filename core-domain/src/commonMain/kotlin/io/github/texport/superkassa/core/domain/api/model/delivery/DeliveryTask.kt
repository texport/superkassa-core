package io.github.texport.superkassa.core.domain.api.model.delivery

/**
 * Задача доставки чека покупателю по одному каналу.
 *
 * Ставится, когда БФД принял чек, и досылается в фоне: кассир ответ
 * получает сразу, а медленный провайдер держит только фон. Задача
 * живёт в базе кассы и переживает перезапуск.
 *
 * Идентификатор складывается из документа, канала и вида полезной
 * нагрузки: повторная постановка того же чека попадает в ту же задачу
 * и второй отправки не даёт.
 *
 * @property id документ, канал и вид нагрузки — см. [idOf].
 * @property kkmId касса документа.
 * @property documentId фискальный документ, который доставляется.
 * @property channel канал: `PRINT`, `SMS`, `EMAIL`, `TELEGRAM`, `WHATSAPP`.
 * @property destination получатель; персональные данные — в журнал не пишется.
 * @property payloadType что уходит: `LINK`, `PDF`, `IMAGE`, `HTML` или `ESC_POS`.
 * @property status где задача сейчас.
 * @property attempts сколько раз задача уже отправлялась.
 * @property nextAttemptAt не раньше какого времени отправлять, мс эпохи.
 * @property failure причина последнего отказа; у доставленной — `null`.
 * @property createdAt когда поставлена, мс эпохи.
 * @property updatedAt когда менялась последний раз, мс эпохи.
 */
data class DeliveryTask(
    val id: String,
    val kkmId: String,
    val documentId: String,
    val channel: String,
    val destination: String?,
    val payloadType: String,
    val status: DeliveryTaskStatus = DeliveryTaskStatus.PENDING,
    val attempts: Int = 0,
    val nextAttemptAt: Long,
    val failure: DeliveryFailure? = null,
    val createdAt: Long,
    val updatedAt: Long = createdAt
) {
    /** Получатель — персональные данные покупателя: строка задачи его не несёт. */
    override fun toString(): String =
        "DeliveryTask(id=$id, kkmId=$kkmId, channel=$channel, payloadType=$payloadType, status=$status, " +
            "attempts=$attempts, code=${failure?.code})"

    companion object {
        /** Идентификатор задачи документа [documentId] по каналу [channel] с нагрузкой [payloadType]. */
        fun idOf(documentId: String, channel: String, payloadType: String): String =
            "$documentId/$channel/$payloadType"
    }
}

/** Где задача доставки сейчас. */
enum class DeliveryTaskStatus {
    /** Ждёт отправки: ещё не отправлялась или ждёт повтора после отказа. */
    PENDING,

    /** Канал принял чек. */
    DELIVERED,

    /** Не доставлена окончательно: канал не настроен или попытки кончились. */
    FAILED
}
