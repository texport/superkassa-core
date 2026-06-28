package kz.mybrain.superkassa.core.domain.model.kkm

import kotlinx.serialization.Serializable

/**
 * Запрос на проведение операции с наличными (внесение/изъятие).
 *
 * @property pin ПИН-код пользователя для авторизации операции (заполняется из заголовка Authorization).
 * @property amount Сумма операции с наличными.
 * @property idempotencyKey Ключ idempotency для предотвращения дублирования операций.
 */
@Serializable
data class CashOperationRequest(
    val pin: String = "",
    val amount: Double,
    val idempotencyKey: String
) {
    /**
     * Создает копию запроса с измененным ПИН-кодом.
     */
    fun copy(pin: String): CashOperationRequest {
        return CashOperationRequest(
            pin = pin,
            amount = this.amount,
            idempotencyKey = this.idempotencyKey
        )
    }
}
