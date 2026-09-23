package io.github.texport.superkassa.core.domain.impl.usecase.ofd

import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdNomenclatureLookupResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.impl.helper.KkmCommonHelper
import io.github.texport.superkassa.core.domain.impl.helper.OfdResponseParser
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.string.api.CoreStrings

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
        // Заблокированная касса в БФД не ходит ни с чем, в том числе со справочником:
        // правило жило в узле, и касса приложения спрашивала справочник в обход блокировки.
        if (kkm.state == KkmState.BLOCKED.name) {
            throw ValidationException(CoreStrings.kkmBlocked(kkm.blockReasonCode), "KKM_BLOCKED")
        }
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
            val msg = CoreStrings.nomenclatureNotFound(barcode).ru
            return parsed.copy(resultText = msg)
        }
        return parsed
    }
}
