package kz.mybrain.superkassa.core.domain.usecase.ofd

import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase

/**
 * Сценарий (Use Case) запроса актуальной информации о ККМ непосредственно из ОФД.
 *
 * Позволяет получить регистрационные данные ККМ, статус смены, значения накопительных счетчиков
 * и другую системную информацию, зарегистрированную на сервере ОФД.
 *
 * @property authorizeUserUseCase Сценарий проверки существования ККМ и авторизации.
 * @property kkmCommonHelper Вспомогательный класс общего функционала работы с ККМ.
 */
class GetOfdInfoUseCase(
    private val authorizeUserUseCase: AuthorizeUserUseCase,
    private val kkmCommonHelper: KkmCommonHelper
) {
    /**
     * Выполняет запрос информации о ККМ из ОФД.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @return [OfdCommandResult] Результат отправки запроса INFO и ответ от ОФД.
     */
    fun execute(kkmId: String): OfdCommandResult {
        // Проверяем существование ККМ
        val kkm = authorizeUserUseCase.requireKkm(kkmId)
        // Отправляем команду запроса информации (INFO) в ОФД
        return kkmCommonHelper.sendOfdCommand(kkm = kkm, commandType = OfdCommandType.INFO, payloadRef = "")
    }
}
