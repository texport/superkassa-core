package kz.mybrain.superkassa.core.presentation.facade

import kz.mybrain.superkassa.core.domain.model.common.*
import kz.mybrain.superkassa.core.domain.model.kkm.*
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptBranding
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptLayoutType
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptResult
import kz.mybrain.superkassa.core.domain.model.report.*
import kz.mybrain.superkassa.core.domain.model.shift.*
import kz.mybrain.superkassa.core.presentation.model.FactoryNumberResponse
import kz.mybrain.superkassa.core.presentation.model.KkmInitDirectRequest
import kz.mybrain.superkassa.core.presentation.model.KkmInitSimpleRequest
import kz.mybrain.superkassa.core.presentation.model.KkmListParams
import kz.mybrain.superkassa.core.presentation.model.KkmListResult
import kz.mybrain.superkassa.core.presentation.model.OfdAuthInfoResponse
import kz.mybrain.superkassa.core.presentation.model.ReceiptBuyRequest
import kz.mybrain.superkassa.core.presentation.model.ReceiptBuyReturnRequest
import kz.mybrain.superkassa.core.presentation.model.ReceiptSellRequest
import kz.mybrain.superkassa.core.presentation.model.ReceiptSellReturnRequest
import kz.mybrain.superkassa.core.presentation.model.UserCreateRequest
import kz.mybrain.superkassa.core.presentation.model.UserResponse
import kz.mybrain.superkassa.core.presentation.model.UserUpdateRequest
import kz.mybrain.superkassa.core.presentation.model.VatRateResponse

/**
 * Интерфейс API Superkassa для взаимодействия презентационного слоя с бизнес-логикой.
 */
interface SuperkassaApi {
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
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Информация об авторизации в ОФД.
     */
    fun getOfdAuthInfo(kkmId: String, pin: String): OfdAuthInfoResponse

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
     * Сгенерировать HTML-представление чека для отображения или печати.
     *
     * @param kkmId ID ККМ.
     * @param documentId ID фискального документа.
     * @param pin ПИН-код кассира/администратора.
     * @param layout Шаблон визуализации чека.
     * @return HTML строка с контентом чека.
     */
    fun getReceiptHtml(
        kkmId: String,
        documentId: String,
        pin: String,
        layout: ReceiptLayoutType? = null
    ): String

    /**
     * Сгенерировать HTML-представление печатного документа (чека, отчета смены и др.).
     *
     * @param kkmId ID ККМ.
     * @param type Тип печатного документа.
     * @param documentId ID документа (для чеков).
     * @param shiftId ID смены (для отчетов).
     * @param pin ПИН-код пользователя.
     * @param layout Шаблон визуализации.
     * @return HTML строка.
     */
    fun getPrintHtml(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String,
        layout: ReceiptLayoutType? = null
    ): String

    /**
     * Сгенерировать PDF-документ (в виде байтового массива) для печати/скачивания.
     *
     * @param kkmId ID ККМ.
     * @param type Тип печатного документа.
     * @param documentId ID документа.
     * @param shiftId ID смены.
     * @param pin ПИН-код пользователя.
     * @param layout Шаблон визуализации.
     * @return PDF файл в виде ByteArray.
     */
    fun getPrintPdf(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String,
        layout: ReceiptLayoutType? = null
    ): ByteArray

    /**
     * Создать фискальный чек общего формата.
     *
     * @param request Параметры запроса.
     * @return Результат выполнения фискальной операции.
     */
    fun createReceipt(request: ReceiptRequest): ReceiptResult

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
     * Повторить попытку отправки чека в ОФД по доступным каналам.
     *
     * @param kkmId ID ККМ.
     * @param documentId ID фискального документа.
     * @param pin ПИН-код пользователя.
     * @return Список результатов по каналам (название канала, успех/ошибка).
     */
    fun retryReceiptDelivery(kkmId: String, documentId: String, pin: String): List<Pair<String, Boolean>>

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
}
