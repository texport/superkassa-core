package kz.mybrain.superkassa.offline_queue.application.model

/**
 * User-facing error message in Russian, Kazakh, and English.
 */
data class QueueErrorMessage(
    val messageRu: String,
    val messageKk: String,
    val messageEn: String
) {
    fun compact(): String = "RU: $messageRu | KK: $messageKk | EN: $messageEn"

    companion object {
        fun mono(message: String): QueueErrorMessage = QueueErrorMessage(
            messageRu = message,
            messageKk = message,
            messageEn = message
        )
    }
}
