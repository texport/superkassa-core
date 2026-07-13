package io.github.texport.superkassa.core.domain.impl.usecase.ofd

import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.api.exception.ValidationException

import io.github.texport.superkassa.core.domain.impl.helper.KkmCommonHelper
import io.github.texport.superkassa.core.domain.impl.helper.OfdResponseParser

/**
 * Сценарий (Use Case) синхронизации сервисной информации ККМ с ОФД.
 *
 * Получает актуальные служебные/сервисные параметры ККМ из ОФД (например, наименование организации,
 * адрес установки кассы, регистрационные номера) и сохраняет их локально.
 *
 * @property storage Порт для доступа к локальному хранилищу данных.
 * @property queue Порт для работы с офлайн-очередью команд ККМ.
 * @property clock Порт для работы с системным временем.
 * @property idGenerator Порт для генерации уникальных идентификаторов.
 * @property authorizeUserUseCase Сценарий проверки прав доступа и состояния ККМ.
 * @property kkmCommonHelper Вспомогательный класс общего функционала работы с ККМ.
 */
class SyncOfdServiceInfoUseCase(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val clock: ClockPort,
    private val idGenerator: IdGeneratorPort,
    private val authorizeUserUseCase: AuthorizeUserUseCase,
    private val kkmCommonHelper: KkmCommonHelper
) {
    /**
     * Выполняет процедуру синхронизации сервисной информации.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param pin PIN-код администратора для авторизации.
     * @return [OfdCommandResult] Результат выполнения команды ОФД.
     * @throws ValidationException при ошибках валидации ККМ, пин-кода или наличии документов в очереди.
     */
    fun execute(kkmId: String, pin: String): OfdCommandResult {
        // Проверяем, разрешена ли синхронизация (смена при этом не должна быть открытой)
        val kkm = kkmCommonHelper.requireSyncAllowed(
            kkmId = kkmId,
            pin = pin,
            allowOpenShift = false,
            authorization = authorizeUserUseCase,
            queue = queue
        )

        // Запрашиваем информацию о ККМ (INFO) из ОФД
        val result = kkmCommonHelper.sendOfdCommand(
            kkm = kkm,
            commandType = OfdCommandType.INFO,
            payloadRef = idGenerator.nextId()
        )

        // При успешном получении ответа из ОФД обновляем локальные сервисные данные кассы
        if (result.status == OfdCommandStatus.OK && result.responseJson != null) {
            storage.inTransaction {
                val currentInfo = kkm.ofdServiceInfo ?: kkmCommonHelper.defaultServiceInfo()
                // Извлекаем из JSON ответа обновленные сервисные данные
                val parsed = OfdResponseParser.extractServiceInfo(result.responseJson, currentInfo)
                val freshKkm = storage.findKkmForUpdate(kkmId)
                if (freshKkm != null) {
                    storage.updateKkm(freshKkm.copy(updatedAt = clock.now(), ofdServiceInfo = parsed))
                }
            }
        }
        return result
    }
}
