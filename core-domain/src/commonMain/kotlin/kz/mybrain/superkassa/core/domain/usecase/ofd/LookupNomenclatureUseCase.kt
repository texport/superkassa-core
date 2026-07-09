package kz.mybrain.superkassa.core.domain.usecase.ofd

import kz.mybrain.superkassa.core.domain.model.ofd.OfdNomenclatureLookupResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper
import kz.mybrain.superkassa.core.domain.helper.OfdResponseParser
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import kz.mybrain.superkassa.core.domain.exception.ErrorMessages

/**
 * Сценарий (Use Case) запроса номенклатуры по коду/штрихкоду напрямую в ОФД.
 *
 * @property authorizeUserUseCase Сценарий авторизации и проверки ККМ.
 * @property kkmCommonHelper Вспомогательный класс общего функционала работы с ККМ и отправки команд.
 */
class LookupNomenclatureUseCase(
    private val authorizeUserUseCase: AuthorizeUserUseCase,
    private val kkmCommonHelper: KkmCommonHelper
) {
    /**
     * Выполняет запрос номенклатуры в ОФД и возвращает структурированный доменный результат.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param barcode Штрихкод или маркировочный код товара.
     * @return [OfdNomenclatureLookupResult] Доменный результат выполнения запроса.
     */
    fun execute(kkmId: String, barcode: String): OfdNomenclatureLookupResult {
        val kkm = authorizeUserUseCase.requireKkm(kkmId)
        val result = kkmCommonHelper.sendOfdCommand(
            kkm = kkm,
            commandType = OfdCommandType.NOMENCLATURE,
            payloadRef = barcode
        )
        val parsed = OfdResponseParser.parseNomenclature(
            responseJson = result.responseJson,
            commandStatus = result.status,
            defaultResultCode = result.resultCode,
            defaultError = result.errorMessage ?: result.resultText
        )
        if (!parsed.found) {
            val msg = ErrorMessages.nomenclatureNotFound(barcode).ru
            return parsed.copy(resultText = msg)
        }
        return parsed
    }
}
