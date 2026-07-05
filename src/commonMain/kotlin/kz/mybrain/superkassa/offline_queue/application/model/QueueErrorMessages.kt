package kz.mybrain.superkassa.offline_queue.application.model

internal object QueueErrorMessages {
    fun handlerException(reason: String): QueueErrorMessage = QueueErrorMessage(
        messageRu = "Ошибка связи или внутренняя ошибка кассы при обработке очереди. " +
            "Пожалуйста, убедитесь в наличии интернета и повторите попытку. (Технические детали: $reason)",
        messageKk = "Кезекпен жұмыс істеу кезінде байланыс немесе кассаның ішкі қатесі орын алды. " +
            "Интернет байланысын тексеріп, әрекетті қайталаңыз. (Техникалық мәліметтер: $reason)",
        messageEn = "Connection failure or internal cashbox error while processing queue. " +
            "Please verify internet connection and retry the operation. (Technical details: $reason)"
    )

    fun invalidDispatchStatus(status: String): QueueErrorMessage = QueueErrorMessage(
        messageRu = "Внутренняя системная ошибка: получен некорректный статус обработки очереди ($status). Пожалуйста, обратитесь в службу поддержки.",
        messageKk = "Ішкі жүйелік қате: кезекті өңдеудің дұрыс емес мәртебесі алынды ($status). Қолдау қызметіне хабарласыңыз.",
        messageEn = "Internal system error: received invalid queue processing status ($status). Please contact support."
    )
}
