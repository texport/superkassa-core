package io.github.texport.superkassa.core.domain.api.port.internal

import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandRequest
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult

/**
 * Порт взаимодействия с сервисами ОФД (Оператор Фискальных Данных).
 * Отвечает за непосредственную отправку сформированных фискальных команд и документов.
 */
interface OfdManagerPort {

    /**
     * Отправляет команду или фискальный документ в ОФД и ожидает результат обработки.
     *
     * @param command параметры отправляемой команды [OfdCommandRequest].
     * @return результат выполнения команды [OfdCommandResult] (успех, фискальный признак, ошибки).
     */
    fun send(command: OfdCommandRequest): OfdCommandResult
}
