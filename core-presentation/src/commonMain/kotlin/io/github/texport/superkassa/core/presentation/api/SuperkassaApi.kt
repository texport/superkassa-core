package io.github.texport.superkassa.core.presentation.api

import io.github.texport.superkassa.core.presentation.api.model.kkm.DocumentDetailsResponse
import io.github.texport.superkassa.core.presentation.api.model.auth.*
import io.github.texport.superkassa.core.presentation.api.model.common.*
import io.github.texport.superkassa.core.presentation.api.model.kkm.*
import io.github.texport.superkassa.core.presentation.api.model.ofd.*
import io.github.texport.superkassa.core.presentation.api.model.queue.*
import io.github.texport.superkassa.core.presentation.api.model.receipt.*
import io.github.texport.superkassa.core.presentation.api.model.shift.*
import io.github.texport.superkassa.core.presentation.api.model.user.*
import io.github.texport.superkassa.core.presentation.api.model.reference.*
import io.github.texport.superkassa.core.domain.impl.logging.LogListener

/**
 * Интерфейс API Superkassa для взаимодействия презентационного слоя с бизнес-логикой.
 */
interface SuperkassaApi : PrintApi {

    companion object

    /**
     * Фасад управления офлайн-очередью команд ОФД.
     */
    val queue: OfflineQueueApi

    /**
     * Установить минимальный уровень логирования.
     * @param levelName Название уровня (INFO, DEBUG, TRACE и т.д.).
     */
    fun setLogLevel(levelName: String)

    /**
     * Установить слушатель логов для передачи сообщений во внешний код (например, Swift).
     */
    fun setLogListener(listener: LogListener)

    /**
     * Получить список доступных ставок НДС.
     *
     * @return Список ставок НДС.
     */
    @Throws(Exception::class)
    fun listVatRates(): List<VatRateResponse>

    /**
     * Прямая инициализация (фискализация) ККМ.
     *
     * @param pin ПИН-код администратора.
     * @param request Параметры инициализации ККМ.
     * @return Сведения об инициализированной ККМ.
     */
    @Throws(Exception::class)
    fun initKkm(pin: String, request: KkmInitDirectRequest): KkmResponse

    /**
     * Упрощенная инициализация ККМ с автоматическим получением данных из ОФД.
     *
     * @param pin ПИН-код администратора.
     * @param request Упрощенный запрос на инициализацию.
     * @return Сведения об инициализированной ККМ.
     */
    @Throws(Exception::class)
    fun initKkmSimple(pin: String, request: KkmInitSimpleRequest): KkmResponse

    /**
     * Сгенерировать заводской номер и год выпуска для новой ККМ.
     *
     * @return FactoryNumberResponse с номером и годом.
     */
    @Throws(Exception::class)
    fun generateFactoryInfo(): FactoryNumberResponse

    /**
     * Найти информацию по ККМ по её идентификатору.
     *
     * @param id ID ККМ.
     * @return Сведения о ККМ.
     */
    @Throws(Exception::class)
    fun getKkm(id: String): KkmResponse

    /**
     * Получить список ККМ по параметрам фильтрации.
     *
     * @param params Параметры пагинации, сортировки и поиска.
     * @return Результат поиска ККМ.
     */
    @Throws(Exception::class)
    fun listKkms(params: KkmListParams): KkmListResponse

    /**
     * Снять ККМ с учета (удалить).
     *
     * @param id ID ККМ.
     * @param pin ПИН-код администратора.
     * @return True при успешном удалении.
     */
    @Throws(Exception::class)
    fun deleteKkm(id: String, pin: String): Boolean

    /**
     * Проверить возможность снятия ККМ с учета (удаления).
     *
     * @param id ID ККМ.
     * @param pin ПИН-код администратора.
     * @return True если удаление разрешено.
     */
    @Throws(Exception::class)
    fun validateCanDeleteKkm(id: String, pin: String): Boolean

    /**
     * Получить список накопленных денежных счетчиков ККМ.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Список денежных счетчиков.
     */
    @Throws(Exception::class)
    fun listCounters(kkmId: String, pin: String): List<CounterSnapshotResponse>

    /**
     * Обновить общие настройки ККМ (например, автозакрытие смены).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @param autoCloseShift Флаг автоматического закрытия смены.
     * @param autoCashout Флаг автоматического изъятия наличных при закрытии смены.
     * @return Сведения об обновленной ККМ.
     */
    @Throws(Exception::class)
    fun updateKkmSettings(kkmId: String, pin: String, autoCloseShift: Boolean, autoCashout: Boolean): KkmResponse

    /**
     * Обновить налоговый режим и группу НДС по умолчанию.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @param taxRegime Новое значение налогового режима.
     * @param defaultVatGroup Новая группа НДС по умолчанию.
     * @return Сведения об обновленной ККМ.
     */
    @Throws(Exception::class)
    fun updateTaxSettings(kkmId: String, pin: String, taxRegime: TaxRegime, defaultVatGroup: VatGroup): KkmResponse

    /**
     * Обновить параметры брендирования (шапки/подвала чеков).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @param branding Настройки брендирования чеков.
     * @return Сведения об обновленной ККМ.
     */
    @Throws(Exception::class)
    fun updateBrandingSettings(kkmId: String, pin: String, branding: ReceiptBrandingRequest): KkmResponse

    /**
     * Задать название кассы.
     *
     * Название живёт на узле и потому одинаково на всех рабочих местах.
     * Фискальным реквизитом оно не является: режим программирования
     * для правки не нужен.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код пользователя кассы.
     * @param name Название или `null`, чтобы его снять.
     * @return Сведения об обновленной ККМ.
     */
    @Throws(Exception::class)
    fun updateKkmName(kkmId: String, pin: String, name: String?): KkmResponse

    /**
     * Войти в режим программирования параметров ККМ.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Сведения о ККМ в состоянии программирования.
     */
    @Throws(Exception::class)
    fun enterProgramming(kkmId: String, pin: String): KkmResponse

    /**
     * Выйти из режима программирования параметров ККМ.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Сведения об обновленной ККМ.
     */
    @Throws(Exception::class)
    fun exitProgramming(kkmId: String, pin: String): KkmResponse

    /**
     * Получить список пользователей ККМ (кассиров и администраторов).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Список пользователей.
     */
    @Throws(Exception::class)
    fun listUsers(kkmId: String, pin: String): List<UserResponse>

    /**
     * Узнать, кто работает под этим ПИН-кодом.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код вошедшего.
     * @return Пользователь кассы; ПИН-код в ответе не возвращается.
     */
    @Throws(Exception::class)
    fun currentUser(kkmId: String, pin: String): UserResponse

    /**
     * Создать нового пользователя в ККМ.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @param request Данные создаваемого пользователя.
     * @return Созданный пользователь.
     */
    @Throws(Exception::class)
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
    @Throws(Exception::class)
    fun updateUser(kkmId: String, userId: String, pin: String, request: UserUpdateRequest): UserResponse

    /**
     * Удалить пользователя из ККМ.
     *
     * @param kkmId ID ККМ.
     * @param userId ID удаляемого пользователя.
     * @param pin ПИН-код администратора.
     * @return True при успешном удалении.
     */
    @Throws(Exception::class)
    fun deleteUser(kkmId: String, userId: String, pin: String): Boolean

    /**
     * Получить авторизационные параметры подключения к ОФД.
     *
     * @param pin ПИН-код администратора.
     * @param request Запрос с идентификатором ККМ.
     * @return Информация об авторизации в ОФД.
     */
    @Throws(Exception::class)
    fun getOfdAuthInfo(pin: String, request: OfdAuthInfoRequest): OfdAuthInfoResponse

    /**
     * Обновить токен доступа к ОФД в локальной памяти.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @param token Новый токен.
     * @return True при успешном обновлении.
     */
    @Throws(Exception::class)
    fun updateOfdToken(kkmId: String, pin: String, token: String): Boolean

    /**
     * Проверить физическое соединение с ОФД по сети.
     *
     * @param kkmId ID ККМ.
     * @return Результат выполнения команды.
     */
    @Throws(Exception::class)
    fun checkOfdConnection(kkmId: String): OfdCommandResponse

    /**
     * Получить общую информацию от ОФД о статусе подключения ККМ.
     *
     * @param kkmId ID ККМ.
     * @return Результат выполнения команды.
     */
    @Throws(Exception::class)
    fun getOfdInfo(kkmId: String): OfdCommandResponse

    /**
     * Синхронизировать сервисную информацию с ОФД.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Результат выполнения команды ОФД.
     */
    @Throws(Exception::class)
    fun syncOfdServiceInfo(kkmId: String, pin: String): OfdCommandResponse

    /**
     * Синхронизировать накопленные счетчики и состояние смены с ОФД.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код администратора.
     * @return Результат выполнения команды ОФД.
     */
    @Throws(Exception::class)
    fun syncOfdCounters(kkmId: String, pin: String): OfdCommandResponse

    /**
     * Создать фискальный чек общего формата.
     *
     * @param command Команда создания чека.
     * @return Результат выполнения фискальной операции.
     */
    @Throws(Exception::class)
    fun createReceipt(command: CreateReceiptCommand): ReceiptResponse

    /**
     * Создать чек продажи (SELL).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Запрос продажи.
     * @return Результат создания чека.
     */
    @Throws(Exception::class)
    fun createSellReceipt(kkmId: String, pin: String, request: ReceiptSellRequest): ReceiptResponse

    /**
     * Создать чек возврата продажи (SELL_RETURN).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Запрос возврата продажи.
     * @return Результат создания чека.
     */
    @Throws(Exception::class)
    fun createSellReturnReceipt(kkmId: String, pin: String, request: ReceiptSellReturnRequest): ReceiptResponse

    /**
     * Создать чек покупки (BUY).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Запрос покупки.
     * @return Результат создания чека.
     */
    @Throws(Exception::class)
    fun createBuyReceipt(kkmId: String, pin: String, request: ReceiptBuyRequest): ReceiptResponse

    /**
     * Создать чек возврата покупки (BUY_RETURN).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Запрос возврата покупки.
     * @return Результат создания чека.
     */
    @Throws(Exception::class)
    fun createBuyReturnReceipt(kkmId: String, pin: String, request: ReceiptBuyReturnRequest): ReceiptResponse

    /**
     * Произвести операцию внесения наличных (Cash In) в кассу.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Данные операции.
     * @return Результат проведения операции.
     */
    @Throws(Exception::class)
    fun cashIn(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResponse

    /**
     * Произвести операцию изъятия наличных (Cash Out) из кассы.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @param request Данные операции.
     * @return Результат проведения операции.
     */
    @Throws(Exception::class)
    fun cashOut(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResponse

    // Вспомогательные комментарии для смен

    /**
     * Открыть кассовую смену.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @return Информация по открытой смене.
     */
    @Throws(Exception::class)
    fun openShift(kkmId: String, pin: String): ShiftResponse

    /**
     * Закрыть кассовую смену с печатью Z-отчета и передачей данных в ОФД.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код кассира.
     * @return Результат генерации сменного Z-отчета.
     */
    @Throws(Exception::class)
    fun closeShift(kkmId: String, pin: String): ReportResponse

    /**
     * Получить информацию о текущей открытой смене ККМ.
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код пользователя.
     * @return Информация об открытой смене.
     */
    @Throws(Exception::class)
    fun getOpenShift(kkmId: String, pin: String): ShiftResponse

    /**
     * Получить локальную информацию об открытой смене ККМ (без проверок доступности ОФД).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код пользователя.
     * @return Информация об открытой смене или null, если смена закрыта.
     */
    @Throws(Exception::class)
    fun getLocalOpenShift(kkmId: String, pin: String): ShiftResponse?

    /**
     * Получить историю смен для ККМ с пагинацией.
     *
     * @param kkmId ID ККМ.
     * @param limit Лимит записей.
     * @param offset Смещение выборки.
     * @param pin ПИН-код пользователя.
     * @return Список смен.
     */
    @Throws(Exception::class)
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
    @Throws(Exception::class)
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
    /**
     * Получить документ вместе с составом чека и тем, кто его оформил.
     *
     * @param kkmId ID ККМ.
     * @param documentId ID документа.
     * @param pin ПИН-код пользователя.
     * @return Документ, его позиции и имя оформившего.
     */
    @Throws(Exception::class)
    fun getDocumentDetails(kkmId: String, documentId: String, pin: String): DocumentDetailsResponse

    @Throws(Exception::class)
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
    @Throws(Exception::class)
    fun createReport(kkmId: String, pin: String): ReportResponse

    /**
     * Запросить номенклатурную позицию по штрихкоду напрямую из ОФД.
     *
     * @param pin ПИН-код оператора.
     * @param request Запрос с идентификатором кассы и штрихкодом.
     * @return Результат поиска номенклатуры.
     */
    @Throws(Exception::class)
    fun lookupNomenclature(pin: String, request: NomenclatureLookupRequest): NomenclatureLookupResponse

    /**
     * Аутентифицировать пользователя кассы (проверить PIN-код).
     *
     * @param kkmId ID ККМ.
     * @param pin ПИН-код пользователя.
     * @return Сведения о вошедшем пользователе.
     */
    @Throws(Exception::class)
    fun authenticate(kkmId: String, pin: String): UserResponse

    /**
     * Получить справочник типов оплат.
     *
     * Каждый элемент несёт признак `supported`: допускает ли действующая
     * версия протокола такой вид оплаты.
     *
     * @return Список типов оплат.
     */
    @Throws(Exception::class)
    fun getPaymentTypes(): List<PaymentTypeResponse>

    /**
     * Получить справочник типов документов.
     *
     * @return Список типов документов.
     */
    @Throws(Exception::class)
    fun getDocumentTypes(): List<DocumentTypeResponse>

    /**
     * Получить справочник ролей пользователей.
     *
     * @return Список ролей пользователей.
     */
    @Throws(Exception::class)
    fun getUserRoles(): List<UserRoleResponse>

    /**
     * Получить справочник налоговых режимов.
     *
     * @return Список налоговых режимов.
     */
    @Throws(Exception::class)
    fun getTaxRegimes(): List<TaxRegimeResponse>

    /**
     * Получить справочник ширины чековой ленты.
     *
     * @return Список вариантов ширины ленты.
     */
    @Throws(Exception::class)
    fun getPaperWidths(): List<PaperWidthResponse>

    /**
     * Получить справочник цветов брендирования.
     *
     * @return Список цветов брендирования.
     */
    @Throws(Exception::class)
    fun getBrandingColors(): List<BrandingColorResponse>

    /**
     * Получить справочник состояний ККМ.
     *
     * @return Список состояний ККМ.
     */
    @Throws(Exception::class)
    fun getKkmStates(): List<KkmStateResponse>

    /**
     * Получить справочник режимов работы ККМ.
     *
     * @return Список режимов работы ККМ.
     */
    @Throws(Exception::class)
    fun getKkmModes(): List<KkmModeResponse>

    /**
     * Получить справочник статусов смены.
     *
     * @return Список статусов смены.
     */
    @Throws(Exception::class)
    fun getShiftStatuses(): List<ShiftStatusResponse>

    /**
     * Получить справочник статусов отправки в ОФД.
     *
     * @return Список статусов отправки.
     */
    @Throws(Exception::class)
    fun getDeliveryStatuses(): List<DeliveryStatusResponse>

    /**
     * Получить справочник статусов выполнения команд ОФД.
     *
     * @return Список статусов команд.
     */
    @Throws(Exception::class)
    fun getOfdCommandStatuses(): List<OfdCommandStatusResponse>

    /**
     * Получить справочник типов фискальных операций чека.
     *
     * @return Список типов фискальных операций.
     */
    @Throws(Exception::class)
    fun getReceiptOperationTypes(): List<ReceiptOperationTypeResponse>

    /**
     * Получить справочник окружений/сред взаимодействия с ОФД.
     *
     * @return Список сред ОФД.
     */
    @Throws(Exception::class)
    fun getOfdEnvironments(): List<OfdEnvironmentResponse>

    /**
     * Получить справочник провайдеров ОФД.
     *
     * @return Список провайдеров ОФД.
     */
    @Throws(Exception::class)
    fun getOfdProviders(): List<OfdProviderResponse>

    /**
     * Получить справочник режимов работы ядра.
     *
     * @return Список режимов работы ядра.
     */
    @Throws(Exception::class)
    fun getCoreModes(): List<CoreModeResponse>

    /**
     * Получить справочник режимов авторизации.
     *
     * @return Список режимов авторизации.
     */
    @Throws(Exception::class)
    fun getAuthModes(): List<AuthModeResponse>

    /**
     * Получить справочник языков чеков.
     *
     * @return Список языков чеков.
     */
    @Throws(Exception::class)
    fun getReceiptLanguages(): List<ReceiptLanguageResponse>

    /**
     * Получить справочник типов макетов чека.
     *
     * @return Список типов макетов.
     */
    @Throws(Exception::class)
    fun getReceiptLayoutTypes(): List<ReceiptLayoutTypeResponse>

    /**
     * Получить справочник типов печатных документов.
     *
     * @return Список типов печатных документов.
     */
    @Throws(Exception::class)
    fun getPrintDocumentTypes(): List<PrintDocumentTypeResponse>

    /**
     * Получить справочник типов команд ОФД.
     *
     * @return Список типов команд ОФД.
     */
    @Throws(Exception::class)
    fun getOfdCommandTypes(): List<OfdCommandTypeResponse>

    /**
     * Получить справочник типов операций с наличными.
     *
     * @return Список типов операций с наличными.
     */
    @Throws(Exception::class)
    fun getCashOperationTypes(): List<CashOperationTypeResponse>

    /**
     * Получить справочник видов отрасли чека.
     *
     * @return Список видов отрасли.
     */
    @Throws(Exception::class)
    fun getReceiptDomainTypes(): List<ReceiptDomainTypeResponse>
}
