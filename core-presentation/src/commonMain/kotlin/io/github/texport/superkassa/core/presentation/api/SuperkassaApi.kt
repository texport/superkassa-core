package io.github.texport.superkassa.core.presentation.api

import io.github.texport.superkassa.core.domain.model.common.*
import io.github.texport.superkassa.core.domain.model.kkm.*
import io.github.texport.superkassa.core.domain.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.model.receipt.ReceiptBranding
import io.github.texport.superkassa.core.domain.usecase.receipt.CreateReceiptCommand
import io.github.texport.superkassa.core.domain.model.receipt.ReceiptResult
import io.github.texport.superkassa.core.domain.model.report.*
import io.github.texport.superkassa.core.domain.model.shift.*
import io.github.texport.superkassa.core.presentation.api.model.FactoryNumberResponse
import io.github.texport.superkassa.core.presentation.api.model.KkmInitDirectRequest
import io.github.texport.superkassa.core.presentation.api.model.KkmInitSimpleRequest
import io.github.texport.superkassa.core.presentation.api.model.KkmListParams
import io.github.texport.superkassa.core.presentation.api.model.KkmListResult
import io.github.texport.superkassa.core.presentation.api.model.OfdAuthInfoRequest
import io.github.texport.superkassa.core.presentation.api.model.OfdAuthInfoResponse
import io.github.texport.superkassa.core.presentation.api.model.NomenclatureLookupRequest
import io.github.texport.superkassa.core.presentation.api.model.NomenclatureLookupResponse
import io.github.texport.superkassa.core.presentation.api.model.ReceiptBuyRequest
import io.github.texport.superkassa.core.presentation.api.model.ReceiptBuyReturnRequest
import io.github.texport.superkassa.core.presentation.api.model.ReceiptSellRequest
import io.github.texport.superkassa.core.presentation.api.model.ReceiptSellReturnRequest
import io.github.texport.superkassa.core.presentation.api.model.UserCreateRequest
import io.github.texport.superkassa.core.presentation.api.model.UserResponse
import io.github.texport.superkassa.core.presentation.api.model.UserUpdateRequest
import io.github.texport.superkassa.core.presentation.api.model.VatRateResponse

/**
 * Интерфейс API Superkassa для взаимодействия презентационного слоя с бизнес-логикой.
 */
interface SuperkassaApi : PrintApi {
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
    fun initKkm(pin: String, request: KkmInitDirectRequest): KkmInfo

    /**
     * Упрощенная инициализация ККМ с автоматическим получением данных из ОФД.
     *
     * @param pin ПИН-код администратора.
     * @param request Упрощенный запрос на инициализацию.
     * @return Сведения об инициализированной ККМ.
     */
    fun initKkmSimple(pin: String, request: KkmInitSimpleRequest): KkmInfo

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
    fun getKkm(id: String): KkmInfo

    /**
     * Получить список ККМ по параметрам фильтрации.
     *
     * @param params Параметры пагинации, сортировки и поиска.
     * @return Результат поиска ККМ.
     */
    fun listKkms(params: KkmListParams): KkmListResult

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
    fun listCounters(kkmId: String, pin: String): List<CounterSnapshot>

    /**
     * Обновить общие настройки ККМ (например, автозакрытие смены).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @param autoCloseShift Флаг автоматического закрытия смены.
     * @return Сведения об обновленной ККМ.
     */
    fun updateKkmSettings(kkmId: String, pin: String, autoCloseShift: Boolean): KkmInfo

    /**
     * Обновить налоговый режим и группу НДС по умолчанию.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @param taxRegime Новое значение налогового режима.
     * @param defaultVatGroup Новая группа НДС по умолчанию.
     * @return Сведения об обновленной ККМ.
     */
    fun updateTaxSettings(kkmId: String, pin: String, taxRegime: TaxRegime, defaultVatGroup: VatGroup): KkmInfo

    /**
     * Обновить параметры брендирования (шапки/подвала чеков).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @param branding Настройки брендирования чеков.
     * @return Сведения об обновленной ККМ.
     */
    fun updateBrandingSettings(kkmId: String, pin: String, branding: ReceiptBranding): KkmInfo

    /**
     * Войти в режим программирования параметров ККМ.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Сведения о ККМ в состоянии программирования.
     */
    fun enterProgramming(kkmId: String, pin: String): KkmInfo

    /**
     * Выйти из режима программирования параметров ККМ.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Сведения об обновленной ККМ.
     */
    fun exitProgramming(kkmId: String, pin: String): KkmInfo

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
    fun checkOfdConnection(kkmId: String): OfdCommandResult

    /**
     * Получить общую информацию от ОФД о статусе подключения ККМ.
     *
     * @param kkmId ID ККМ.
     * @return Результат выполнения команды.
     */
    fun getOfdInfo(kkmId: String): OfdCommandResult

    /**
     * Синхронизировать сервисную информацию с ОФД.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Результат выполнения команды ОФД.
     */
    fun syncOfdServiceInfo(kkmId: String, pin: String): OfdCommandResult

    /**
     * Синхронизировать накопленные счетчики и состояние смены с ОФД.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Результат выполнения команды ОФД.
     */
    fun syncOfdCounters(kkmId: String, pin: String): OfdCommandResult

    /**
     * Создать фискальный чек общего формата.
     *
     * @param command Команда создания чека.
     * @return Результат выполнения фискальной операции.
     */
    fun createReceipt(command: CreateReceiptCommand): ReceiptResult

    /**
     * Создать чек продажи (SELL).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Запрос продажи.
     * @return Результат создания чека.
     */
    fun createSellReceipt(kkmId: String, pin: String, request: ReceiptSellRequest): ReceiptResult

    /**
     * Создать чек возврата продажи (SELL_RETURN).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Запрос возврата продажи.
     * @return Результат создания чека.
     */
    fun createSellReturnReceipt(kkmId: String, pin: String, request: ReceiptSellReturnRequest): ReceiptResult

    /**
     * Создать чек покупки (BUY).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Запрос покупки.
     * @return Результат создания чека.
     */
    fun createBuyReceipt(kkmId: String, pin: String, request: ReceiptBuyRequest): ReceiptResult

    /**
     * Создать чек возврата покупки (BUY_RETURN).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Запрос возврата покупки.
     * @return Результат создания чека.
     */
    fun createBuyReturnReceipt(kkmId: String, pin: String, request: ReceiptBuyReturnRequest): ReceiptResult

    /**
     * Произвести операцию внесения наличных (Cash In) в кассу.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Данные операции.
     * @return Результат проведения операции.
     */
    fun cashIn(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResult

    /**
     * Произвести операцию изъятия наличных (Cash Out) из кассы.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Данные операции.
     * @return Результат проведения операции.
     */
    fun cashOut(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResult

    /**
     * Открыть кассовую смену.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @return Информация по открытой смене.
     */
    fun openShift(kkmId: String, pin: String): ShiftInfo

    /**
     * Закрыть кассовую смену с печатью Z-отчета и передачей данных в ОФД.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @return Результат генерации сменного Z-отчета.
     */
    fun closeShift(kkmId: String, pin: String): ReportResult

    /**
     * Получить информацию о текущей открытой смене ККМ.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код пользователя.
     * @return Информация об открытой смене.
     */
    fun getOpenShift(kkmId: String, pin: String): ShiftInfo

    /**
     * Получить историю смен для ККМ с пагинацией.
     *
     * @param kkmId ID ККМ.
     * @param limit Лимит записей.
     * @param offset Смещение выборки.
     * @param pin ПИН-код пользователя.
     * @return Список смен.
     */
    fun listShifts(kkmId: String, limit: Int, offset: Int, pin: String): List<ShiftInfo>

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
    ): List<FiscalDocumentSnapshot>

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
    ): List<FiscalDocumentSnapshot>

    /**
     * Сгенерировать X-отчет (сменный отчет без гашения).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира/администратора.
     * @return Сведения об отправке/формировании отчета.
     */
    fun createReport(kkmId: String, pin: String): ReportResult

    /**
     * Запросить номенклатурную позицию по штрихкоду напрямую из ОФД.
     *
     * @param pin ПИН-код оператора.
     * @param request Запрос с идентификатором кассы и штрихкодом.
     * @return Результат поиска номенклатуры.
     */
    fun lookupNomenclature(pin: String, request: NomenclatureLookupRequest): NomenclatureLookupResponse
}
