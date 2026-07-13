package io.github.texport.superkassa.core.domain.impl.usecase.ofd

import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.impl.helper.KkmCommonHelper
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase

/**
 * Сценарий (Use Case) отправки фискальной команды напрямую в ОФД.
 *
 * Используется для прямой незамедлительной передачи команд (таких как регистрация чеков, открытие/закрытие смены)
 * на сервера ОФД, когда касса работает в онлайн-режиме.
 *
 * @property authorizeUserUseCase Сценарий проверки существования ККМ и авторизации.
 * @property kkmCommonHelper Вспомогательный класс общего функционала работы с ККМ.
 */
class SendFiscalCommandUseCase(
    private val authorizeUserUseCase: AuthorizeUserUseCase,
    private val kkmCommonHelper: KkmCommonHelper
) {
    /**
     * Выполняет отправку фискальной команды в ОФД.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param commandType Тип фискальной команды (например, SELL, CLOSE_SHIFT и т.д.).
     * @param payloadRef Ссылка на полезную нагрузку фискального документа (идентификатор документа).
     * @return [OfdCommandResult] Результат выполнения команды ОФД.
     */
    fun execute(kkmId: String, commandType: OfdCommandType, payloadRef: String): OfdCommandResult {
        // Проверяем существование ККМ
        val kkm = authorizeUserUseCase.requireKkm(kkmId)
        // Отправляем команду выбранного типа в ОФД
        return kkmCommonHelper.sendOfdCommand(kkm = kkm, commandType = commandType, payloadRef = payloadRef)
    }
}
