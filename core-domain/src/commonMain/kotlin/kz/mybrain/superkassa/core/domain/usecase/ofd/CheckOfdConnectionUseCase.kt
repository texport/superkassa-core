package kz.mybrain.superkassa.core.domain.usecase.ofd

import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase

/**
 * Сценарий (Use Case) проверки сетевого соединения с Оператором Фискальных Данных (ОФД).
 *
 * Используется для пингования/проверки доступности сервисов ОФД со стороны конкретной ККМ.
 *
 * @property authorizeUserUseCase Сценарий авторизации и проверки ККМ.
 * @property kkmCommonHelper Вспомогательный класс общего функционала работы с ККМ и отправки команд.
 */
class CheckOfdConnectionUseCase(
    private val authorizeUserUseCase: AuthorizeUserUseCase,
    private val kkmCommonHelper: KkmCommonHelper
) {
    /**
     * Выполняет проверку связи с ОФД для указанной ККМ.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @return [OfdCommandResult] Результат выполнения команды ОФД.
     */
    fun execute(kkmId: String): OfdCommandResult {
        // Проверяем существование ККМ
        val kkm = authorizeUserUseCase.requireKkm(kkmId)
        // Отправляем системную команду (пинг) в ОФД
        return kkmCommonHelper.sendOfdCommand(kkm = kkm, commandType = OfdCommandType.SYSTEM, payloadRef = "")
    }
}
