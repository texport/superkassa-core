package io.github.texport.superkassa.core.domain.impl.usecase.ofd

import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort

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
        val dbVal = storage.loadCounters(kkmId, CounterScopes.GLOBAL, null)[reqNumCounterKey]
        val current = if (dbVal == null) {
            val envStart = startReqNumOverride
            if (envStart != null) envStart - 1L else 0L
        } else {
            dbVal
        }

        // Вычисляем следующий номер с учетом лимита циклического счетчика
        val next = if (current >= maxReqNum) 0L else current + 1L

        if (persist) {
            // Сохраняем новое значение счетчика в базу данных
            storage.upsertCounter(kkmId, CounterScopes.GLOBAL, null, reqNumCounterKey, next)
        }
        return next.toInt()
    }

    /**
     * Отмечает номер израсходованным: ОФД его увидел.
     *
     * До ответа номер не расходуется. Спецификация CPCR, «Работа в нормальном
     * режиме»: при обрыве соединения или отсутствии ответа повтор отправляется
     * с теми же TOKEN и REQNUM, чтобы сервер отличил повтор от нового
     * обращения. Раньше номер выдавался и сохранялся до отправки, поэтому
     * повтор уходил под новым номером, а сервер видел два разных запроса.
     */
    fun commit(kkmId: String, reqNum: Int) {
        storage.upsertCounter(kkmId, CounterScopes.GLOBAL, null, reqNumCounterKey, reqNum.toLong())
        storage.upsertCounter(kkmId, CounterScopes.GLOBAL, null, UNANSWERED_KEY, NONE)
    }

    /**
     * Номер документа, отправленного без ответа, либо `null`.
     *
     * Такой номер принадлежит своему документу до ответа на него: повтор
     * уходит с теми же TOKEN и REQNUM (CPCR, п. 5.1), и по ним БФД узнаёт
     * повтор, а не новый документ. Раньше этот номер доставался первому
     * следующему запросу — «Проверить связь» уходила с ним же, номер
     * считался израсходованным, и досылка чека шла под следующим номером
     * как новый документ.
     */
    fun unanswered(kkmId: String): Int? =
        storage.loadCounters(kkmId, CounterScopes.GLOBAL, null)[UNANSWERED_KEY]
            ?.takeIf { it != NONE }
            ?.toInt()

    /** Отмечает, что документ под номером [reqNum] ушёл, а ответа на него нет. */
    fun markUnanswered(kkmId: String, reqNum: Int) {
        storage.upsertCounter(kkmId, CounterScopes.GLOBAL, null, UNANSWERED_KEY, reqNum.toLong())
    }

    companion object {
        var startReqNumOverride: Long? = null

        /** Номер документа, ушедшего без ответа. */
        private const val UNANSWERED_KEY = "ofd.req_num.unanswered"

        /** Документа без ответа нет. */
        private const val NONE = -1L
    }
}
