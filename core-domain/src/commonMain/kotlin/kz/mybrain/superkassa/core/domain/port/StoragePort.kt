package kz.mybrain.superkassa.core.domain.port

import kz.mybrain.superkassa.core.domain.model.auth.KkmUser
import kz.mybrain.superkassa.core.domain.model.auth.UserRole
import kz.mybrain.superkassa.core.domain.model.common.CounterSnapshot
import kz.mybrain.superkassa.core.domain.model.common.Money
import kz.mybrain.superkassa.core.domain.model.kkm.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.queue.QueueTask
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.shift.ShiftInfo
import kz.mybrain.superkassa.core.domain.model.shift.ShiftStatus

/**
 * Порт хранения данных (репозиторий) для ядра ККМ.
 *
 * Реализация находится на инфраструктурном уровне (модуль storage) и инкапсулирует
 * все операции с базой данных, транзакциями, хранением документов, ККМ, пользователей и очередей.
 */
interface StoragePort {
    /**
     * Выполняет блок кода внутри транзакции базы данных.
     *
     * @param block функциональный блок, выполняемый в рамках транзакции.
     * @return результат выполнения блока.
     */
    fun <T> inTransaction(block: () -> T): T

    /**
     * Регистрирует ККМ в базе данных.
     *
     * @param info данные ККМ.
     * @return `true`, если регистрация прошла успешно; `false` в противном случае.
     */
    fun createKkm(info: KkmInfo): Boolean

    /**
     * Обновляет состояние и конфигурацию ККМ.
     *
     * @param info новые данные ККМ.
     * @return `true`, если обновление прошло успешно; `false` в противном случае.
     */
    fun updateKkm(info: KkmInfo): Boolean

    /**
     * Находит ККМ по её уникальному идентификатору.
     *
     * @param id уникальный идентификатор ККМ.
     * @return объект [KkmInfo] или `null`, если ККМ не найдена.
     */
    fun findKkm(id: String): KkmInfo?

    /**
     * Находит ККМ по её уникальному идентификатору с пессимистической блокировкой.
     *
     * @param id уникальный идентификатор ККМ.
     * @return объект [KkmInfo] или `null`, если ККМ не найдена.
     */
    fun findKkmForUpdate(id: String): KkmInfo?

    /**
     * Находит ККМ по её регистрационному номеру (РНМ).
     *
     * @param registrationNumber регистрационный номер ККМ в налоговых органах.
     * @return объект [KkmInfo] или `null`, если ККМ не найдена.
     */
    fun findKkmByRegistrationNumber(registrationNumber: String): KkmInfo?

    /**
     * Находит ККМ по системному идентификатору (системный серийный номер).
     *
     * @param systemId системный идентификатор ККМ.
     * @return объект [KkmInfo] или `null`, если ККМ не найдена.
     */
    fun findKkmBySystemId(systemId: String): KkmInfo?

    /**
     * Возвращает постраничный список зарегистрированных ККМ с фильтрацией и сортировкой.
     *
     * @param limit максимальное количество возвращаемых записей.
     * @param offset смещение относительно начала списка.
     * @param state фильтр по состоянию ККМ.
     * @param search поисковая строка (для поиска по имени, номерам ККМ).
     * @param sortBy поле для сортировки (по умолчанию "createdAt").
     * @param sortOrder направление сортировки ("ASC" или "DESC").
     * @return список найденных ККМ [KkmInfo].
     */
    fun listKkms(
        limit: Int,
        offset: Int = 0,
        state: String? = null,
        search: String? = null,
        sortBy: String = "createdAt",
        sortOrder: String = "DESC"
    ): List<KkmInfo>

    /**
     * Возвращает общее количество ККМ с учётом фильтров.
     *
     * @param state фильтр по состоянию ККМ.
     * @param search поисковая строка.
     * @return общее количество ККМ, соответствующих условиям.
     */
    fun countKkms(
        state: String? = null,
        search: String? = null
    ): Int

    /**
     * Удаляет ККМ по её уникальному идентификатору.
     *
     * @param id уникальный идентификатор ККМ.
     * @return `true`, если удаление прошло успешно; `false` в противном случае.
     */
    fun deleteKkm(id: String): Boolean

    /**
     * Проверяет наличие автономной очереди по кассе (queue_task, lane=OFFLINE).
     *
     * @param kkmId уникальный идентификатор ККМ.
     * @return `true`, если в БД есть задачи в офлайн-очереди; `false` в противном случае.
     * @deprecated Используйте [OfflineQueuePort.canSendDirectly] — офлайн-очередь является единственным источником истины.
     */
    fun hasOfflineQueue(kkmId: String): Boolean

    /**
     * Ставит задачу в очередь выполнения (queue_task).
     *
     * @param dto данные задачи для постановки в очередь.
     * @return `true`, если задача успешно добавлена; `false` в противном случае.
     */
    fun enqueueQueueTask(dto: QueueTask): Boolean

    /**
     * Возвращает список задач в очереди для указанной кассы и потока (lane).
     *
     * @param cashboxId идентификатор кассы.
     * @param lane поток обработки задач (например, "OFFLINE" или "DELIVERY").
     * @param limit максимальное количество задач.
     * @param offset смещение для постраничного вывода.
     * @return список задач [QueueTask].
     */
    fun listQueueTasksByCashbox(cashboxId: String, lane: String, limit: Int, offset: Int = 0): List<QueueTask>

    /**
     * Возвращает следующую ожидающую выполнения задачу в очереди для указанной кассы и потока.
     *
     * @param cashboxId идентификатор кассы.
     * @param lane поток обработки задач.
     * @param now текущее время для фильтрации задач по времени следующей попытки выполнения.
     * @return следующая задача [QueueTask] или `null`, если очередь пуста.
     */
    fun nextPendingQueueTask(cashboxId: String, lane: String, now: Long): QueueTask?

    /**
     * Обновляет статус выполнения задачи в очереди.
     *
     * @param id уникальный идентификатор задачи.
     * @param status новый статус задачи.
     * @param attempt текущий номер попытки выполнения.
     * @param lastError текст последней ошибки (если есть).
     * @param nextAttemptAt время следующей попытки выполнения в миллисекундах.
     * @return `true`, если статус успешно обновлен; `false` в противном случае.
     */
    fun updateQueueTaskStatus(
        id: String,
        status: String,
        attempt: Int,
        lastError: String?,
        nextAttemptAt: Long?
    ): Boolean

    /**
     * Помечает задачу в очереди как выполняющуюся в данный момент (in progress).
     *
     * @param id уникальный идентификатор задачи.
     * @param now текущее время пометки.
     * @return `true`, если операция успешна; `false` в противном случае.
     */
    fun markQueueTaskInProgress(id: String, now: Long): Boolean

    /**
     * Удаляет все задачи в очереди для указанной кассы.
     *
     * @param cashboxId идентификатор кассы.
     * @return `true`, если задачи удалены; `false` в противном случае.
     */
    fun deleteQueueTasksByCashbox(cashboxId: String): Boolean

    /**
     * Пытается захватить блокировку (lock) очереди для кассы.
     * Используется для предотвращения одновременной обработки одной кассы разными воркерами.
     *
     * @param cashboxId идентификатор кассы.
     * @param ownerId идентификатор воркера, захватывающего блокировку.
     * @param leaseUntil время, до которого блокировка считается активной (epoch millis).
     * @param acquiredAt время захвата блокировки.
     * @return `true`, если блокировка успешно захвачена; `false` в противном случае.
     */
    fun tryAcquireQueueLock(cashboxId: String, ownerId: String, leaseUntil: Long, acquiredAt: Long): Boolean

    /**
     * Продлевает аренду блокировки очереди кассы.
     *
     * @param cashboxId идентификатор кассы.
     * @param ownerId идентификатор воркера-владельца блокировки.
     * @param leaseUntil новое время окончания действия блокировки.
     * @param now текущее время.
     * @return `true`, если блокировка успешно продлена; `false` в противном случае.
     */
    fun renewQueueLock(cashboxId: String, ownerId: String, leaseUntil: Long, now: Long): Boolean

    /**
     * Освобождает ранее захваченную блокировку очереди кассы.
     *
     * @param cashboxId идентификатор кассы.
     * @param ownerId идентификатор воркера-владельца блокировки.
     * @return `true`, если блокировка успешно снята; `false` в противном случае.
     */
    fun releaseQueueLock(cashboxId: String, ownerId: String): Boolean

    /**
     * Полностью удаляет ККМ и все связанные с ней исторические данные из БД (каскадное удаление).
     *
     * @param kkmId уникальный идентификатор ККМ.
     * @return `true`, если удаление выполнено успешно; `false` в противном случае.
     */
    fun deleteKkmCompletely(kkmId: String): Boolean

    /**
     * Обновляет зашифрованный авторизационный токен ККМ для работы с ОФД.
     *
     * @param id уникальный идентификатор ККМ.
     * @param tokenEncryptedBase64 зашифрованный токен в кодировке Base64.
     * @param updatedAt время обновления токена.
     * @return `true`, если токен обновлен; `false` в противном случае.
     */
    fun updateKkmToken(id: String, tokenEncryptedBase64: String, updatedAt: Long): Boolean

    /**
     * Возвращает список пользователей (кассиров), привязанных к указанной кассе.
     *
     * @param kkmId уникальный идентификатор ККМ.
     * @return список пользователей [KkmUser].
     */
    fun listUsers(kkmId: String): List<KkmUser>

    /**
     * Создает и сохраняет нового пользователя ККМ.
     *
     * @param kkmId идентификатор ККМ.
     * @param userId уникальный идентификатор пользователя.
     * @param name имя пользователя.
     * @param role роль пользователя в системе [UserRole] (например, кассир, админ).
     * @param pin открытый PIN-код (если применимо/требуется).
     * @param pinHash криптографический хеш PIN-кода.
     * @param createdAt время создания пользователя.
     * @return `true`, если пользователь успешно создан; `false` в противном случае.
     */
    fun createUser(
        kkmId: String,
        userId: String,
        name: String,
        role: UserRole,
        pin: String,
        pinHash: String,
        createdAt: Long
    ): Boolean

    /**
     * Обновляет параметры существующего пользователя ККМ.
     * Переданные значения `null` означают, что соответствующее поле обновлять не нужно.
     *
     * @param kkmId идентификатор ККМ.
     * @param userId уникальный идентификатор пользователя.
     * @param name новое имя пользователя (опционально).
     * @param role новая роль пользователя (опционально).
     * @param pin новый PIN-код (опционально).
     * @param pinHash новый хеш PIN-кода (опционально).
     * @return `true`, если пользователь обновлен; `false` в противном случае.
     */
    fun updateUser(
        kkmId: String,
        userId: String,
        name: String?,
        role: UserRole?,
        pin: String?,
        pinHash: String?
    ): Boolean

    /**
     * Удаляет пользователя ККМ.
     *
     * @param kkmId идентификатор ККМ.
     * @param userId идентификатор пользователя.
     * @return `true`, если пользователь удален; `false` в противном случае.
     */
    fun deleteUser(kkmId: String, userId: String): Boolean

    /**
     * Находит пользователя ККМ по его уникальному идентификатору.
     *
     * @param kkmId идентификатор ККМ.
     * @param userId идентификатор пользователя.
     * @return объект [KkmUser] или `null`, если пользователь не найден.
     */
    fun findUserById(kkmId: String, userId: String): KkmUser?

    /**
     * Находит пользователя ККМ по хешу его PIN-кода (используется при авторизации на кассе).
     *
     * @param kkmId идентификатор ККМ.
     * @param pinHash хеш PIN-кода.
     * @return объект [KkmUser] или `null`, если пользователь с таким PIN-кодом не найден.
     */
    fun findUserByPin(kkmId: String, pinHash: String): KkmUser?

    /**
     * Возвращает текущую открытую смену на указанной ККМ.
     *
     * @param kkmId уникальный идентификатор ККМ.
     * @return объект [ShiftInfo] открытой смены или `null`, если смена закрыта.
     */
    fun findOpenShift(kkmId: String): ShiftInfo?

    /**
     * Возвращает смену ККМ по её идентификатору.
     *
     * @param shiftId уникальный идентификатор смены.
     * @return объект [ShiftInfo] или `null`, если смена не найдена.
     */
    fun findShiftById(shiftId: String): ShiftInfo?

    /**
     * Возвращает список всех смен ККМ постранично, отсортированный по убыванию времени открытия.
     *
     * @param kkmId уникальный идентификатор ККМ.
     * @param limit лимит записей.
     * @param offset смещение.
     * @return список смен ККМ [ShiftInfo].
     */
    fun listShifts(kkmId: String, limit: Int, offset: Int = 0): List<ShiftInfo>

    /**
     * Сохраняет информацию о создании новой смены в БД (открытие смены).
     *
     * @param shift объект смены [ShiftInfo].
     * @return `true`, если запись успешна; `false` в противном случае.
     */
    fun createShift(shift: ShiftInfo): Boolean

    /**
     * Фиксирует закрытие смены в БД с изменением статуса и записью времени закрытия.
     *
     * @param shiftId уникальный идентификатор смены.
     * @param status финальный статус смены [ShiftStatus] (например, CLOSED).
     * @param closedAt время закрытия смены (epoch millis).
     * @param closeDocumentId идентификатор фискального документа закрытия смены (Z-отчёта).
     * @return `true`, если смена успешно закрыта в БД; `false` в противном случае.
     */
    fun closeShift(shiftId: String, status: ShiftStatus, closedAt: Long, closeDocumentId: String?): Boolean

    /**
     * Сохраняет данные фискального чека при его регистрации.
     *
     * @param request исходные данные чека (запрос на продажу).
     * @param documentId идентификатор сгенерированного фискального документа.
     * @param shiftId идентификатор смены, в которой зарегистрирован чек.
     * @param createdAt время регистрации чека.
     * @return `true`, если чек успешно сохранен; `false` в противном случае.
     */
    fun saveReceipt(request: ReceiptRequest, documentId: String, shiftId: String, createdAt: Long): Boolean

    /**
     * Сохраняет операцию движения наличных средств (внесение/изъятие).
     *
     * @param kkmId идентификатор ККМ.
     * @param type тип операции ("CASH_IN" / "CASH_OUT").
     * @param amount сумма операции.
     * @param documentId идентификатор нефискального документа операции.
     * @param shiftId идентификатор смены.
     * @param createdAt время выполнения операции.
     * @return `true`, если операция успешно сохранена; `false` в противном случае.
     */
    fun saveCashOperation(
        kkmId: String,
        type: String,
        amount: Money,
        documentId: String,
        shiftId: String,
        createdAt: Long
    ): Boolean

    /**
     * Обновляет фискальный статус чека после отправки в ОФД или перехода в автономный режим.
     *
     * @param documentId уникальный идентификатор документа.
     * @param fiscalSign фискальный признак документа (ФПД), полученный от ОФД.
     * @param autonomousSign автономный фискальный признак (если ККМ работает в автономном режиме).
     * @param ofdStatus строковый статус доставки в ОФД.
     * @param deliveredAt время доставки документа в ОФД (epoch millis).
     * @param isAutonomous признак того, был ли чек пробит в автономном режиме без связи с ОФД.
     * @return `true`, если статус успешно обновлен; `false` в противном случае.
     */
    fun updateReceiptStatus(
        documentId: String,
        fiscalSign: String?,
        autonomousSign: String?,
        ofdStatus: String,
        deliveredAt: Long?,
        isAutonomous: Boolean? = null
    ): Boolean

    /**
     * Возвращает снимок (снапшот) фискального документа по его уникальному идентификатору.
     *
     * @param id уникальный идентификатор документа.
     * @return объект [FiscalDocumentSnapshot] или `null`, если документ не найден.
     */
    fun findFiscalDocumentById(id: String): FiscalDocumentSnapshot?

    /**
     * Возвращает фискальный документ и сохранённые исходные параметры чека (payload) по идентификатору документа.
     * Используется для повторного рендеринга чека в HTML/PDF.
     *
     * @param documentId уникальный идентификатор документа чека.
     * @return пара из снимка документа и исходного запроса чека, либо `null` если документ не найден или не является чеком с полезной нагрузкой.
     */
    fun findFiscalDocumentWithReceiptPayload(documentId: String): Pair<FiscalDocumentSnapshot, ReceiptRequest>?

    /**
     * Возвращает список фискальных документов (чеки, отчёты, операции с наличными), зарегистрированных в рамках смены.
     *
     * @param kkmId идентификатор ККМ.
     * @param shiftId идентификатор смены.
     * @param limit лимит записей для постраничного вывода.
     * @param offset смещение.
     * @return список снимков документов [FiscalDocumentSnapshot].
     */
    fun listFiscalDocumentsByShift(
        kkmId: String,
        shiftId: String,
        limit: Int,
        offset: Int = 0
    ): List<FiscalDocumentSnapshot>

    /**
     * Возвращает список фискальных документов ККМ за указанный временной период.
     *
     * @param kkmId идентификатор ККМ.
     * @param fromInclusive начало периода (включительно, в миллисекундах).
     * @param toExclusive конец периода (исключая, в миллисекундах).
     * @param limit лимит записей.
     * @param offset смещение.
     * @return список найденных снимков документов [FiscalDocumentSnapshot].
     */
    fun listFiscalDocumentsByPeriod(
        kkmId: String,
        fromInclusive: Long,
        toExclusive: Long,
        limit: Int,
        offset: Int = 0
    ): List<FiscalDocumentSnapshot>

    /**
     * Возвращает общее количество фискальных документов в системе.
     * При указании типа документа считает только документы этого типа (например, "CHECK").
     *
     * @param docType тип документа для фильтрации (опционально).
     * @return общее количество документов.
     */
    fun countFiscalDocuments(docType: String? = null): Long

    /**
     * Возвращает общее количество закрытых смен (количество сформированных Z-отчётов).
     *
     * @return количество закрытых смен.
     */
    fun countClosedShifts(): Long

    /**
     * Возвращает количество неотправленных фискальных команд в офлайн-очереди ККМ.
     *
     * @return количество отложенных задач.
     */
    fun countOfflineQueue(): Long

    /**
     * Загружает накопительные фискальные счётчики ККМ по указанной категории (scope).
     *
     * @param kkmId идентификатор ККМ.
     * @param scope область действия счётчиков (например, "shift", "global").
     * @param shiftId идентификатор смены (если scope = "shift").
     * @return словарь (Map), сопоставляющий имя счётчика с его числовым значением.
     */
    fun loadCounters(kkmId: String, scope: String, shiftId: String? = null): Map<String, Long>

    /**
     * Возвращает список всех зарегистрированных снимков счётчиков ККМ.
     *
     * @param kkmId уникальный идентификатор ККМ.
     * @return список снимков счётчиков [CounterSnapshot].
     */
    fun listCounters(kkmId: String): List<CounterSnapshot>

    /**
     * Добавляет или обновляет значение конкретного накопительного счётчика.
     *
     * @param kkmId идентификатор ККМ.
     * @param scope область действия счётчика.
     * @param shiftId идентификатор смены.
     * @param key ключ (идентификатор) счётчика.
     * @param value новое или добавляемое значение счётчика.
     * @return `true`, если операция успешна; `false` в противном случае.
     */
    fun upsertCounter(kkmId: String, scope: String, shiftId: String?, key: String, value: Long): Boolean

    /**
     * Регистрирует уникальный ключ идемпотентности для предотвращения повторного выполнения операции.
     *
     * @param kkmId идентификатор ККМ.
     * @param idempotencyKey уникальный ключ идемпотентного запроса.
     * @param operation название выполняемой бизнес-операции.
     * @return `true`, если ключ успешно зарегистрирован (ранее не существовал); `false`, если ключ уже существует.
     */
    fun insertIdempotency(kkmId: String, idempotencyKey: String, operation: String): Boolean

    /**
     * Возвращает ранее сохраненный текстовый ответ на идемпотентный запрос.
     *
     * @param kkmId идентификатор ККМ.
     * @param idempotencyKey ключ идемпотентности.
     * @return сохраненное сериализованное тело ответа, либо `null` если запрос выполняется впервые.
     */
    fun findIdempotencyResponse(kkmId: String, idempotencyKey: String): String?

    /**
     * Обновляет и сохраняет результат выполнения идемпотентного запроса.
     *
     * @param kkmId идентификатор ККМ.
     * @param idempotencyKey ключ идемпотентности.
     * @param responseRef сериализованный ответ для последующего возврата при дублирующем запросе.
     * @return `true`, если результат сохранен успешно; `false` в противном случае.
     */
    fun updateIdempotencyResponse(kkmId: String, idempotencyKey: String, responseRef: String?): Boolean
}
