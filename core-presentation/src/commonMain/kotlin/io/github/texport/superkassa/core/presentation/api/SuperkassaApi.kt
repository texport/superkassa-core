package io.github.texport.superkassa.core.presentation.api

import io.github.texport.superkassa.core.presentation.api.model.auth.*
import io.github.texport.superkassa.core.presentation.api.model.common.*
import io.github.texport.superkassa.core.presentation.api.model.kkm.*
import io.github.texport.superkassa.core.presentation.api.model.ofd.*
import io.github.texport.superkassa.core.presentation.api.model.queue.*
import io.github.texport.superkassa.core.presentation.api.model.receipt.*
import io.github.texport.superkassa.core.presentation.api.model.shift.*
import io.github.texport.superkassa.core.presentation.api.model.user.*

/**
 * Интерфейс API Superkassa для взаимодействия презентационного слоя с бизнес-логикой.
 */
interface SuperkassaApi : PrintApi {
    /**
     * Фасад управления офлайн-очередью команд ОФД.
     */
    val queue: OfflineQueueApi

    /**
     * Получить список доступных ставок НДС.
     *
     * @return Список ставок НДС.
     */
    fun listVatRates(): List<VatRateResponse>

    /**
     * Прямая инициализация (фискализация) ККМ.
     *
     * @param pin ПИН-код администратора.
     * @param request Параметры инициализации ККМ.
     * @return Сведения об инициализированной ККМ.
     */
    fun initKkm(pin: String, request: KkmInitDirectRequest): KkmResponse

    /**
     * Упрощенная инициализация ККМ с автоматическим получением данных из ОФД.
     *
     * @param pin ПИН-код администратора.
     * @param request Упрощенный запрос на инициализацию.
     * @return Сведения об инициализированной ККМ.
     */
    fun initKkmSimple(pin: String, request: KkmInitSimpleRequest): KkmResponse

    /**
     * Сгенерировать заводской номер и год выпуска для новой ККМ.
     *
     * @return FactoryNumberResponse с номером и годом.
     */
    fun generateFactoryInfo(): FactoryNumberResponse

    /**
     * Найти информацию по ККМ по её идентификатору.
     *
     * @param id ID ККМ.
     * @return Сведения о ККМ.
     */
    fun getKkm(id: String): KkmResponse

    /**
     * Получить список ККМ по параметрам фильтрации.
     *
     * @param params Параметры пагинации, сортировки и поиска.
     * @return Результат поиска ККМ.
     */
    fun listKkms(params: KkmListParams): KkmListResponse

    /**
     * Снять ККМ с учета (удалить).
     *
     * @param id ID ККМ.
     * @param pin ПИН-код администратора.
     * @return True при успешном удалении.
     */
    fun deleteKkm(id: String, pin: String): Boolean

    /**
     * Получить список накопленных денежных счетчиков ККМ.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Список денежных счетчиков.
     */
    fun listCounters(kkmId: String, pin: String): List<CounterSnapshotResponse>

    /**
     * Обновить общие настройки ККМ (например, автозакрытие смены).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @param autoCloseShift Флаг автоматического закрытия смены.
     * @return Сведения об обновленной ККМ.
     */
    fun updateKkmSettings(kkmId: String, pin: String, autoCloseShift: Boolean): KkmResponse

    /**
     * Обновить налоговый режим и группу НДС по умолчанию.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @param taxRegime Новое значение налогового режима.
     * @param defaultVatGroup Новая группа НДС по умолчанию.
     * @return Сведения об обновленной ККМ.
     */
    fun updateTaxSettings(kkmId: String, pin: String, taxRegime: TaxRegime, defaultVatGroup: VatGroup): KkmResponse

    /**
     * Обновить параметры брендирования (шапки/подвала чеков).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @param branding Настройки брендирования чеков.
     * @return Сведения об обновленной ККМ.
     */
    fun updateBrandingSettings(kkmId: String, pin: String, branding: ReceiptBrandingRequest): KkmResponse

    /**
     * Войти в режим программирования параметров ККМ.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Сведения о ККМ в состоянии программирования.
     */
    fun enterProgramming(kkmId: String, pin: String): KkmResponse

    /**
     * Выйти из режима программирования параметров ККМ.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Сведения об обновленной ККМ.
     */
    fun exitProgramming(kkmId: String, pin: String): KkmResponse

    /**
     * Получить список пользователей ККМ (кассиров и администраторов).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Список пользователей.
     */
    fun listUsers(kkmId: String, pin: String): List<UserResponse>

    /**
     * Создать нового пользователя в ККМ.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @param request Данные создаваемого пользователя.
     * @return Созданный пользователь.
     */
    fun createUser(kkmId: String, pin: String, request: UserCreateRequest): UserResponse

    /**
     * Обновить данные пользователя в ККМ.
     *
     * @param kkmId ID ККМ.
     * @param userId ID обновляемого пользователя.
     * @param pin ПИН-код администратора.
     * @param request Новые параметры пользователя.
     * @return Обновленный пользователь.
     */
    fun updateUser(kkmId: String, userId: String, pin: String, request: UserUpdateRequest): UserResponse

    /**
     * Удалить пользователя из ККМ.
     *
     * @param kkmId ID ККМ.
     * @param userId ID удаляемого пользователя.
     * @param pin ПИН-код администратора.
     * @return True при успешном удалении.
     */
    fun deleteUser(kkmId: String, userId: String, pin: String): Boolean

    /**
     * Получить авторизационные параметры подключения к ОФД.
     *
     * @param pin ПИН-код администратора.
     * @param request Запрос с идентификатором ККМ.
     * @return Информация об авторизации в ОФД.
     */
    fun getOfdAuthInfo(pin: String, request: OfdAuthInfoRequest): OfdAuthInfoResponse

    /**
     * Обновить токен доступа к ОФД в локальной памяти.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @param token Новый токен.
     * @return True при успешном обновлении.
     */
    fun updateOfdToken(kkmId: String, pin: String, token: String): Boolean

    /**
     * Проверить физическое соединение с ОФД по сети.
     *
     * @param kkmId ID ККМ.
     * @return Результат выполнения команды.
     */
    fun checkOfdConnection(kkmId: String): OfdCommandResponse

    /**
     * Получить общую информацию от ОФД о статусе подключения ККМ.
     *
     * @param kkmId ID ККМ.
     * @return Результат выполнения команды.
     */
    fun getOfdInfo(kkmId: String): OfdCommandResponse

    /**
     * Синхронизировать сервисную информацию с ОФД.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Результат выполнения команды ОФД.
     */
    fun syncOfdServiceInfo(kkmId: String, pin: String): OfdCommandResponse

    /**
     * Синхронизировать накопленные счетчики и состояние смены с ОФД.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Результат выполнения команды ОФД.
     */
    fun syncOfdCounters(kkmId: String, pin: String): OfdCommandResponse

    /**
     * Создать фискальный чек общего формата.
     *
     * @param command Команда создания чека.
     * @return Результат выполнения фискальной операции.
     */
    fun createReceipt(command: CreateReceiptCommand): ReceiptResponse

    /**
     * Создать чек продажи (SELL).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Запрос продажи.
     * @return Результат создания чека.
     */
    fun createSellReceipt(kkmId: String, pin: String, request: ReceiptSellRequest): ReceiptResponse

    /**
     * Создать чек возврата продажи (SELL_RETURN).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Запрос возврата продажи.
     * @return Результат создания чека.
     */
    fun createSellReturnReceipt(kkmId: String, pin: String, request: ReceiptSellReturnRequest): ReceiptResponse

    /**
     * Создать чек покупки (BUY).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Запрос покупки.
     * @return Результат создания чека.
     */
    fun createBuyReceipt(kkmId: String, pin: String, request: ReceiptBuyRequest): ReceiptResponse

    /**
     * Создать чек возврата покупки (BUY_RETURN).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Запрос возврата покупки.
     * @return Результат создания чека.
     */
    fun createBuyReturnReceipt(kkmId: String, pin: String, request: ReceiptBuyReturnRequest): ReceiptResponse

    /**
     * Произвести операцию внесения наличных (Cash In) в кассу.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Данные операции.
     * @return Результат проведения операции.
     */
    fun cashIn(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResponse

    /**
     * Произвести операцию изъятия наличных (Cash Out) из кассы.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Данные операции.
     * @return Результат проведения операции.
     */
    fun cashOut(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResponse

    // Вспомогательные комментарии для смен

    /**
     * Открыть кассовую смену.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @return Информация по открытой смене.
     */
    fun openShift(kkmId: String, pin: String): ShiftResponse

    /**
     * Закрыть кассовую смену с печатью Z-отчета и передачей данных в ОФД.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @return Результат генерации сменного Z-отчета.
     */
    fun closeShift(kkmId: String, pin: String): ReportResponse

    /**
     * Получить информацию о текущей открытой смене ККМ.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код пользователя.
     * @return Информация об открытой смене.
     */
    fun getOpenShift(kkmId: String, pin: String): ShiftResponse

    /**
     * Получить историю смен для ККМ с пагинацией.
     *
     * @param kkmId ID ККМ.
     * @param limit Лимит записей.
     * @param offset Смещение выборки.
     * @param pin ПИН-код пользователя.
     * @return Список смен.
     */
    fun listShifts(kkmId: String, limit: Int, offset: Int, pin: String): List<ShiftResponse>

    /**
     * Получить список фискальных документов для конкретной смены.
     *
     * @param kkmId ID ККМ.
     * @param shiftId ID смены.
     * @param limit Лимит записей.
     * @param offset Смещение.
     * @param pin ПИН-код пользователя.
     * @return Список снимков фискальных документов.
     */
    fun listShiftDocuments(
        kkmId: String,
        shiftId: String,
        limit: Int,
        offset: Int,
        pin: String
    ): List<FiscalDocumentResponse>

    /**
     * Получить фискальные документы за определенный период времени.
     *
     * @param kkmId ID ККМ.
     * @param fromInclusive Начало периода (epoch ms).
     * @param toExclusive Конец периода (epoch ms).
     * @param limit Лимит записей.
     * @param offset Смещение.
     * @param pin ПИН-код пользователя.
     * @return Список фискальных документов.
     */
    fun listFiscalDocumentsByPeriod(
        kkmId: String,
        fromInclusive: Long,
        toExclusive: Long,
        limit: Int,
        offset: Int,
        pin: String
    ): List<FiscalDocumentResponse>

    /**
     * Сгенерировать X-отчет (сменный отчет без гашения).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира/администратора.
     * @return Сведения об отправке/формировании отчета.
     */
    fun createReport(kkmId: String, pin: String): ReportResponse

    /**
     * Запросить номенклатурную позицию по штрихкоду напрямую из ОФД.
     *
     * @param pin ПИН-код оператора.
     * @param request Запрос с идентификатором кассы и штрихкодом.
     * @return Результат поиска номенклатуры.
     */
    fun lookupNomenclature(pin: String, request: NomenclatureLookupRequest): NomenclatureLookupResponse
}
