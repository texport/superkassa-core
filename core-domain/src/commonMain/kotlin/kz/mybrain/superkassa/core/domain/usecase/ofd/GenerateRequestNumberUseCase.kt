package kz.mybrain.superkassa.core.domain.usecase.ofd

import kz.mybrain.superkassa.core.domain.model.common.CounterScopes
import kz.mybrain.superkassa.core.domain.port.StoragePort

/**
 * Сценарий (Use Case) генерации уникального порядкового номера запроса к ОФД.
 *
 * Каждый запрос к ОФД должен содержать инкрементируемый порядковый номер запроса (Request Number).
 * Диапазон номеров обычно ограничен (в данном случае до 65535), после чего счетчик сбрасывается в 0.
 *
 * @property storage Порт для доступа к хранилищу счетчиков ККМ.
 */
class GenerateRequestNumberUseCase(
    private val storage: StoragePort
) {
    /**
     * Ключ счетчика для хранения текущего номера запроса в базе данных.
     */
    private val reqNumCounterKey = "ofd.req_num"

    /**
     * Максимальное значение номера запроса перед сбросом.
     */
    private val maxReqNum = 65535L

    /**
     * Генерирует следующий порядковый номер запроса.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param persist Если true, обновляет значение счетчика в хранилище.
     * @return [Int] Новый сгенерированный номер запроса к ОФД.
     */
    fun execute(kkmId: String, persist: Boolean = true): Int {
        // Загружаем текущее значение счетчика из глобальной области видимости ККМ
        val current = storage.loadCounters(kkmId, CounterScopes.GLOBAL, null)[reqNumCounterKey] ?: 0L
        
        // Вычисляем следующий номер с учетом лимита циклического счетчика
        val next = if (current >= maxReqNum) 0L else current + 1L
        
        if (persist) {
            // Сохраняем новое значение счетчика в базу данных
            storage.upsertCounter(kkmId, CounterScopes.GLOBAL, null, reqNumCounterKey, next)
        }
        return next.toInt()
    }
}
