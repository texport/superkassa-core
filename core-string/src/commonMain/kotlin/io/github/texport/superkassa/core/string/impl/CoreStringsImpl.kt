package io.github.texport.superkassa.core.string.impl

import io.github.texport.superkassa.core.string.api.TrilingualMessage

/**
 * Единая внутренняя реализация всех локализованных сообщений и строк для всех модулей Superkassa.
 */
internal object CoreStringsImpl {

    // --- Доменные ошибки (Domain Errors) ---
    internal fun badRequest(): TrilingualMessage = TrilingualMessage(
        ru = "Некорректный запрос",
        kk = "Қате сұраныс",
        en = "Bad request"
    )

    internal fun kkmNotFound(): TrilingualMessage = TrilingualMessage(
        ru = "ККМ не найдена",
        kk = "БКМ табылмады",
        en = "KKM not found"
    )

    internal fun kkmExists(): TrilingualMessage = TrilingualMessage(
        ru = "ККМ уже зарегистрирована",
        kk = "БКМ тіркеліп қойған",
        en = "KKM already exists"
    )

    internal fun shiftNotOpen(): TrilingualMessage = TrilingualMessage(
        ru = "Смена не открыта",
        kk = "Ауысым ашылмаған",
        en = "Shift is not open"
    )

    /**
     * Смена перевалила сутки.
     *
     * Кассиру говорится, что делать: закрыть смену. Требование к ККМ
     * (пункты 14, 52 и 93) запрещает оформлять кассовые операции, пока
     * смену не закрыли, а не советует поторопиться.
     */
    internal fun shiftLongerThanDay(): TrilingualMessage = TrilingualMessage(
        ru = "Смена длится больше суток. Закройте смену — до этого касса операций не оформляет.",
        kk = "Ауысым тәуліктен асты. Ауысымды жабыңыз — оған дейін касса операция жасамайды.",
        en = "The shift is longer than 24 hours. Close the shift: until then no operations are issued."
    )

    internal fun shiftAlreadyOpen(): TrilingualMessage = TrilingualMessage(
        ru = "Смена уже открыта",
        kk = "Ауысым ашылып қойған",
        en = "Shift already open"
    )

    internal fun shiftTooLong(): TrilingualMessage = TrilingualMessage(
        ru = "Смена превышает 24 часа",
        kk = "Ауысым 24 сағаттан асады",
        en = "Shift exceeds 24 hours"
    )

    internal fun systemTimeInvalid(): TrilingualMessage = TrilingualMessage(
        ru = "Неверное системное время",
        kk = "Жүйелік уақыт қате",
        en = "System time invalid"
    )

    internal fun ofdProviderUnknown(id: String): TrilingualMessage = TrilingualMessage(
        ru = "Неизвестный БФД: $id",
        kk = "Белгісіз БФД: $id",
        en = "Unknown BFD provider: $id"
    )

    internal fun ofdEnvironmentUnknown(env: String): TrilingualMessage = TrilingualMessage(
        ru = "Неизвестная площадка БФД: $env",
        kk = "Белгісіз БФД алаңы: $env",
        en = "Unknown BFD environment: $env"
    )

    internal fun ofdProviderRequired(): TrilingualMessage = TrilingualMessage(
        ru = "БФД обязателен",
        kk = "БФД міндетті",
        en = "BFD provider required"
    )

    internal fun ofdProviderTagInvalid(tag: String): TrilingualMessage = TrilingualMessage(
        ru = "Неверный тег БФД: $tag",
        kk = "БФД қате тегі: $tag",
        en = "Invalid BFD tag: $tag"
    )

    internal fun ofdTokenRequired(): TrilingualMessage = TrilingualMessage(
        ru = "Токен БФД обязателен",
        kk = "БФД токені міндетті",
        en = "BFD token required"
    )

    /**
     * Токен не разобран.
     *
     * Само значение в сообщение не попадает: кто прочитал токен — тот
     * отправляет фискальные документы от имени этой кассы. Кассиру нужно
     * знать, что делать, а не какую строку он ввёл.
     */
    internal fun ofdTokenInvalid(): TrilingualMessage = TrilingualMessage(
        ru = "Токен БФД неверен: получите новый в кабинете",
        kk = "БФД токені жарамсыз: жаңасын кабинеттен алыңыз",
        en = "The BFD token is invalid: get a new one in the BFD cabinet"
    )

    internal fun kkmRegistrationRequired(): TrilingualMessage = TrilingualMessage(
        ru = "Регистрационный номер ККМ обязателен",
        kk = "БКМ тіркеу нөмірі міндетті",
        en = "KKM registration number required"
    )

    internal fun kkmFactoryRequired(): TrilingualMessage = TrilingualMessage(
        ru = "Заводской номер ККМ обязателен",
        kk = "БКМ зауыттық нөмірі міндетті",
        en = "KKM factory number required"
    )

    internal fun kkmSystemIdRequired(): TrilingualMessage = TrilingualMessage(
        ru = "systemId ККМ обязателен",
        kk = "БКМ systemId міндетті",
        en = "KKM systemId required"
    )

    internal fun kkmSystemIdInvalid(systemId: String): TrilingualMessage = TrilingualMessage(
        ru = "Неверный systemId ККМ: $systemId",
        kk = "БКМ қате systemId: $systemId",
        en = "Invalid KKM systemId: $systemId"
    )

    internal fun kkmSystemIdExists(systemId: String): TrilingualMessage = TrilingualMessage(
        ru = "systemId ККМ уже существует: $systemId",
        kk = "БКМ systemId бар: $systemId",
        en = "KKM systemId already exists: $systemId"
    )

    internal fun userPinRequired(): TrilingualMessage = TrilingualMessage(
        ru = "PIN пользователя обязателен",
        kk = "Пайдаланушының PIN-коды міндетті",
        en = "User PIN required"
    )

    internal fun userNotFound(): TrilingualMessage = TrilingualMessage(
        ru = "Пользователь не найден или неверный PIN",
        kk = "Пайдаланушы табылмады немесе PIN қате",
        en = "User not found or invalid PIN"
    )

    internal fun userForbidden(): TrilingualMessage = TrilingualMessage(
        ru = "У пользователя нет прав на операцию",
        kk = "Пайдаланушының операцияға құқығы жоқ",
        en = "Operation forbidden for role"
    )

    internal fun userNameRequired(): TrilingualMessage = TrilingualMessage(
        ru = "Имя пользователя обязательно",
        kk = "Пайдаланушының аты міндетті",
        en = "User name required"
    )

    internal fun userRoleRequired(roleName: String): TrilingualMessage = TrilingualMessage(
        ru = "Нужен хотя бы один ${roleWord(roleName).ru}",
        kk = "Кем дегенде бір ${roleWord(roleName).kk} қажет",
        en = "At least one ${roleWord(roleName).en} required"
    )

    /**
     * Роль словом на языке кассира.
     *
     * В сообщение шло имя перечисления — кассир читал «Нужен хотя бы один
     * CASHIER». Незнакомая роль остаётся как пришла: выдумывать перевод
     * тому, чего не знаем, хуже, чем показать код.
     */
    private fun roleWord(roleName: String): TrilingualMessage = when (roleName.uppercase()) {
        "ADMIN" -> TrilingualMessage(ru = "администратор", kk = "әкімші", en = "administrator")
        "CASHIER" -> TrilingualMessage(ru = "кассир", kk = "кассир", en = "cashier")
        else -> TrilingualMessage(ru = roleName, kk = roleName, en = roleName)
    }

    internal fun userPinConflict(): TrilingualMessage = TrilingualMessage(
        ru = "Такой пин на этой кассе уже занят. Задайте другой.",
        kk = "Бұл пин осы кассада бос емес. Басқасын енгізіңіз.",
        en = "This PIN is already taken on this cash register. Set a different one."
    )

    internal fun userUpdateEmpty(): TrilingualMessage = TrilingualMessage(
        ru = "Нужно передать хотя бы одно поле для обновления пользователя",
        kk = "Пайдаланушыны жаңарту үшін кем дегенде бір өрісті беру керек",
        en = "User update requires at least one field"
    )

    internal fun kkmDeleteRequiresProgramming(): TrilingualMessage = TrilingualMessage(
        ru = "Касса не в режиме программирования. Удаление невозможно.",
        kk = "Касса бағдарламалау режимінде емес. Өшіру мүмкін емес.",
        en = "Cash register is not in programming mode. Cannot delete."
    )

    internal fun kkmDeleteShiftOpen(): TrilingualMessage = TrilingualMessage(
        ru = "Нельзя удалить кассу с открытой сменой. Сначала закройте смену.",
        kk = "Ашық ауысымы бар кассаны өшіруге болмайды. Алдымен ауысымды жабыңыз.",
        en = "Cannot delete cash register with open shift. Please close shift first."
    )

    internal fun cashSumNegative(): TrilingualMessage = TrilingualMessage(
        ru = "Сумма операции не может быть отрицательной.",
        kk = "Операция сомасы теріс бола алмайды.",
        en = "Operation amount cannot be negative."
    )

    /**
     * Наличных в ящике меньше, чем требует действие.
     *
     * Случая два, и оба платят из ящика: изъятие наличных и покупка
     * у населения. Прежний текст называл только изъятие, и кассир,
     * принимая макулатуру, читал «недостаточно наличных для изъятия».
     */
    internal fun insufficientCash(): TrilingualMessage = TrilingualMessage(
        ru = "В кассе недостаточно наличных.",
        kk = "Кассада қолма-қол ақша жеткіліксіз.",
        en = "Not enough cash in the drawer."
    )

    internal fun printDocumentIdRequired(): TrilingualMessage = TrilingualMessage(
        ru = "Для печати документа нужно указать его идентификатор.",
        kk = "Құжатты басып шығару үшін оның идентификаторын көрсету қажет.",
        en = "Document id is required to print the document."
    )

    internal fun printShiftIdRequired(): TrilingualMessage = TrilingualMessage(
        ru = "Для печати отчёта о закрытии смены нужно указать смену.",
        kk = "Ауысымды жабу есебін басып шығару үшін ауысымды көрсету қажет.",
        en = "Shift is required to print the shift close report."
    )

    internal fun cashierCannotChangeRole(): TrilingualMessage = TrilingualMessage(
        ru = "Кассир не может менять свою роль.",
        kk = "Кассир өз рөлін өзгерте алмайды.",
        en = "A cashier cannot change their own role."
    )

    internal fun kkmSettingsShiftOpen(): TrilingualMessage = TrilingualMessage(
        ru = "Нельзя менять настройки кассы при открытой смене. Сначала закройте смену.",
        kk = "Ашық ауысым кезінде касса баптауларын өзгертуге болмайды. Алдымен ауысымды жабыңыз.",
        en = "Cannot change cash register settings while a shift is open. Please close shift first."
    )

    internal fun kkmSettingsQueueNotEmpty(): TrilingualMessage = TrilingualMessage(
        ru = "Нельзя менять настройки кассы, пока есть неотправленные документы в БФД.",
        kk = "БФД-ға жіберілмеген құжаттар бар кезде касса баптауларын өзгертуге болмайды.",
        en = "Cannot change cash register settings while documents are undelivered to the BFD."
    )

    internal fun queueRetryShiftOpen(): TrilingualMessage = TrilingualMessage(
        ru = "Нельзя повторить отправку очереди при открытой смене. Сначала закройте смену.",
        kk = "Ашық ауысым кезінде кезекті қайта жіберуге болмайды. Алдымен ауысымды жабыңыз.",
        en = "Cannot retry the delivery queue while a shift is open. Please close shift first."
    )

    internal fun kkmDeleteQueueNotEmpty(): TrilingualMessage = TrilingualMessage(
        ru = "Нельзя удалить кассу, пока есть неотправленные документы в БФД.",
        kk = "БФД-ға жіберілмеген құжаттар бар кезде кассаны өшіруге болмайды.",
        en = "Cannot delete cash register while unsent documents remain."
    )

    internal fun kkmDeleteAutonomousNotAllowed(): TrilingualMessage = TrilingualMessage(
        ru = "Нельзя удалить кассу в автономном режиме.",
        kk = "Автономды режимдегі кассаны өшіруге болмайды.",
        en = "Cannot delete cash register in autonomous mode."
    )

    internal fun kkmSyncShiftOpen(): TrilingualMessage = TrilingualMessage(
        ru = "Смена не закрыта",
        kk = "Ауысым жабылмаған",
        en = "Shift is open"
    )

    internal fun kkmSyncQueueNotEmpty(): TrilingualMessage = TrilingualMessage(
        ru = "Очередь не пустая",
        kk = "Кезек бос емес",
        en = "Queue is not empty"
    )

    internal fun kkmSyncShiftDiverged(shiftNo: Long): TrilingualMessage = TrilingualMessage(
        ru = "БФД считает смену закрытой, а на кассе смена $shiftNo открыта. " +
            "Снимите Z-отчёт и повторите сверку",
        kk = "БФД ауысымды жабық деп санайды, ал кассада $shiftNo ауысымы ашық. " +
            "Z-есепті алып, салыстыруды қайталаңыз",
        en = "BFD considers the shift closed while shift $shiftNo is open on the register. " +
            "Take a Z-report and repeat the reconciliation"
    )

    internal fun kkmAutonomousTooLong(): TrilingualMessage = TrilingualMessage(
        ru = "Автономный режим превышает 72 часа",
        kk = "Автономды режим 72 сағаттан асады",
        en = "Autonomous mode exceeds 72 hours"
    )

    internal fun kkmBlocked(): TrilingualMessage = TrilingualMessage(
        ru = "ККМ заблокирована по требованию БФД",
        kk = "БКМ БФД талабы бойынша бұғатталған",
        en = "KKM is blocked by BFD instruction"
    )

    /**
     * Блокировка с причиной и с тем, что делать.
     *
     * Голое «заблокирована по требованию ОФД» кассиру не говорит ничего:
     * снимается такая блокировка разными действиями, и угадывать их
     * у прилавка он не должен.
     *
     * @param code код блокировки: ответ ОФД плюс тысяча либо свой код кассы.
     */
    internal fun kkmBlockedWithReason(code: Int?): TrilingualMessage = when (code) {
        OPEN_SHIFT_TIMEOUT_BLOCK -> TrilingualMessage(
            ru = "Смена открыта дольше допустимого — закройте смену Z-отчётом",
            kk = "Ауысым рұқсат етілгеннен ұзақ ашық — ауысымды Z-есеппен жабыңыз",
            en = "The shift has been open too long - close it with a Z-report"
        )
        INVALID_TOKEN_BLOCK -> TrilingualMessage(
            ru = "Токен БФД недействителен — замените токен в настройках",
            kk = "БФД токені жарамсыз — баптауларда токенді ауыстырыңыз",
            en = "The BFD token is invalid - replace it in the settings"
        )
        AUTONOMOUS_LIMIT_BLOCK -> TrilingualMessage(
            ru = "Автономная работа дольше допустимого — восстановите связь с БФД",
            kk = "Автономды жұмыс рұқсат етілгеннен ұзақ — БФД байланысын қалпына келтіріңіз",
            en = "Autonomous work has lasted too long - restore the link to the BFD"
        )
        else -> kkmBlocked()
    }

    internal fun kkmSettingsRequiresProgramming(): TrilingualMessage = TrilingualMessage(
        ru = "ККМ должна быть в режиме PROGRAMMING",
        kk = "БКМ PROGRAMMING режимінде болуы керек",
        en = "KKM must be in PROGRAMMING"
    )

    internal fun kkmInProgramming(): TrilingualMessage = TrilingualMessage(
        ru = "ККМ в режиме программирования",
        kk = "БКМ бағдарламалау режимінде",
        en = "KKM is in programming mode"
    )

    internal fun unauthorized(): TrilingualMessage = TrilingualMessage(
        ru = "Не авторизован",
        kk = "Авторизацияланбаған",
        en = "Unauthorized"
    )

    internal fun measureUnitCodeInvalid(code: String): TrilingualMessage = TrilingualMessage(
        ru = "Неверный код единицы измерения: $code",
        kk = "Өлшем бірлігінің қате коды: $code",
        en = "Invalid measure unit code: $code"
    )

    internal fun measureUnitNotFound(code: String): TrilingualMessage = TrilingualMessage(
        ru = "Единица измерения не найдена: $code",
        kk = "Өлшем бірлігі табылмады: $code",
        en = "Unit of measurement not found: $code"
    )

    internal fun paymentTypeNotSupported(payment: String, protocolVersion: String): TrilingualMessage =
        TrilingualMessage(
            ru = "Вид оплаты $payment не поддерживается протоколом $protocolVersion",
            kk = "$payment төлем түрі $protocolVersion хаттамасында қолданылмайды",
            en = "Payment type $payment is not supported by protocol $protocolVersion"
        )

    internal fun shiftNotFound(): TrilingualMessage = TrilingualMessage(
        ru = "Смена не найдена",
        kk = "Ауысым табылмады",
        en = "Shift not found"
    )

    internal fun documentNotFound(): TrilingualMessage = TrilingualMessage(
        ru = "Документ не найден или содержимое чека недоступно",
        kk = "Құжат табылмады немесе чектің мазмұны қолжетімсіз",
        en = "Document not found or receipt content unavailable"
    )

    internal fun okedRequired(): TrilingualMessage = TrilingualMessage(
        ru = "ОКЭД обязателен",
        kk = "ЭҚЖЖ міндетті",
        en = "OKED is required"
    )

    internal fun nomenclatureNotFound(barcode: String): TrilingualMessage = TrilingualMessage(
        ru = "Товар со штрихкодом $barcode не найден в Национальном каталоге товаров",
        kk = "Штрихкоды $barcode тауар Ұлттық тауарлар каталогынан табылмады",
        en = "Item with barcode $barcode not found in National Catalog of Goods"
    )

    internal fun parentTicketRequiredForReturns(): TrilingualMessage = TrilingualMessage(
        ru = "Чек-основание (parentTicket) обязателен при возврате",
        kk = "Қайтару кезінде негізгі чек (parentTicket) міндетті болып табылады",
        en = "Parent ticket (parentTicket) is required for returns"
    )

    internal fun documentNotFiscalized(): TrilingualMessage = TrilingualMessage(
        ru = "Документ не был фискализирован: отправлять покупателю нечего",
        kk = "Құжат фискалданбаған: сатып алушыға жіберетін ештеңе жоқ",
        en = "Document was never fiscalized: there is nothing to send"
    )

    internal fun deliveryChannelsNotConfigured(): TrilingualMessage = TrilingualMessage(
        ru = "Ни один канал доставки чека не настроен",
        kk = "Чекті жеткізудің бірде-бір арнасы бапталмаған",
        en = "No receipt delivery channel is configured"
    )

    internal fun receiptDiscountScopesConflict(): TrilingualMessage = TrilingualMessage(
        ru = "Нельзя одновременно применять скидки на уровне позиций и на уровне всего чека",
        kk = "Бір чек ішінде позиция деңгейіндегі және бүкіл чек деңгейіндегі жеңілдіктерді бір уақытта қолдануға болмайды",
        en = "Cannot apply both item-level and receipt-level discounts in a single receipt"
    )

    /**
     * Скидка и наценка на сам чек вместе.
     *
     * Сервис приёма отвергает такой чек по существу: в `ticket.amounts`
     * допустимо одно из двух. Прежде касса узнавала об этом уже ответом
     * и показывала кассиру путь поля JSON.
     */
    internal fun receiptDiscountAndMarkupConflict(): TrilingualMessage = TrilingualMessage(
        ru = "На чек нельзя поставить скидку и наценку одновременно",
        kk = "Чекке жеңілдік пен үстемені бір уақытта қоюға болмайды",
        en = "A receipt cannot carry both a discount and a markup"
    )

    /**
     * Возврат больше, чем осталось по чеку-основанию.
     *
     * Касса считает уже возвращённое по этому же чеку: без этого по чеку
     * на 700 ₸ можно было вернуть 350 ₸, а затем ещё 700 ₸, и ни ОФД,
     * ни сервис приёма такой пары не ловили.
     */
    internal fun refundExceedsBasis(left: String): TrilingualMessage = TrilingualMessage(
        ru = "По этому чеку можно вернуть не больше $left ₸",
        kk = "Бұл чек бойынша $left ₸ артық қайтаруға болмайды",
        en = "No more than $left KZT can be refunded against this receipt"
    )

    /**
     * По чеку-основанию уже возвращено всё.
     *
     * Отдельно от [refundExceedsBasis]: «не больше 0,00 ₸» — это ребус,
     * а кассиру нужно знать, что возвращать больше нечего.
     */
    internal fun refundAlreadyFull(): TrilingualMessage = TrilingualMessage(
        ru = "По этому чеку уже возвращено всё",
        kk = "Бұл чек бойынша бәрі қайтарылған",
        en = "This receipt has already been fully refunded"
    )

    internal fun receiptVatScopesConflict(): TrilingualMessage = TrilingualMessage(
        ru = "НДС в чеке задаётся одним способом: либо ставкой на весь чек, либо ставками позиций",
        kk = "Чекте ҚҚС бір тәсілмен беріледі: не бүкіл чекке бір мөлшерлемемен, не позициялардың мөлшерлемелерімен",
        en = "VAT in a receipt is set one way: either one rate for the whole receipt or rates on the items"
    )

    internal fun receiptVatNotAllowed(group: String): TrilingualMessage = TrilingualMessage(
        ru = "Касса не является плательщиком НДС: ставка $group в чеке недопустима",
        kk = "Касса ҚҚС төлеушісі емес: чектегі $group мөлшерлемесі жарамсыз",
        en = "The register is not a VAT payer: rate $group is not allowed in a receipt"
    )

    internal fun paymentsTotalMismatch(paid: String, total: String): TrilingualMessage = TrilingualMessage(
        ru = "Сумма оплат $paid не сходится с итогом чека $total",
        kk = "Төлемдер сомасы $paid чек қорытындысымен $total сәйкес келмейді",
        en = "Payments sum $paid does not match the receipt total $total"
    )

    // --- Технические ошибки инфраструктуры (Data/Infrastructure Errors) ---
    /**
     * Причина отказа обмена одной строкой, все три языка сразу.
     *
     * Строка собирается тем же способом, каким её потом разбирают
     * ([TrilingualMessage.compact] и [TrilingualMessage.ofCompact]):
     * иначе обёртка не узнала бы в ней трёхъязычную и вставила бы её
     * целиком в каждый язык.
     */
    internal fun ofdRequestFailedData(details: String?): String {
        val errorText = details ?: "unknown"
        return TrilingualMessage(
            ru = "Ошибка запроса к БФД: $errorText",
            kk = "БФД-ға сұраныс қатесі: $errorText",
            en = "BFD request failed: $errorText"
        ).compact()
    }

    // --- Сообщения очереди (Queue Messages) ---
    internal fun handlerException(reason: String): TrilingualMessage = TrilingualMessage(
        ru = "Ошибка связи или внутренняя ошибка кассы при обработке очереди. " +
            "Пожалуйста, убедитесь в наличии интернета и повторите попытку. (Технические детали: {})",
        kk = "Кезекпен жұмыс істеу кезінде байланыс немесе кассаның ішкі қатесі орын алды. " +
            "Интернет байланысын тексеріп, әрекетті қайталаңыз. (Техникалық мәліметтер: {})",
        en = "Connection failure or internal cashbox error while processing queue. " +
            "Please verify internet connection and retry the operation. (Technical details: {})"
    ).wrapping(reason)

    internal fun invalidDispatchStatus(status: String): TrilingualMessage = TrilingualMessage(
        ru = "Внутренняя системная ошибка: получен некорректный статус обработки очереди ($status). Пожалуйста, обратитесь в службу поддержки.",
        kk = "Ішкі жүйелік қате: кезекті өңдеудің дұрыс емес мәртебесі алынды ($status). Қолдау қызметіне хабарласыңыз.",
        en = "Internal system error: received invalid queue processing status ($status). Please contact support."
    )

    internal fun ofdTimeout(): TrilingualMessage = TrilingualMessage(
        ru = "Тайм-аут ожидания ответа от БФД",
        kk = "БФД жауабын күту уақыты бітті",
        en = "BFD connection timeout"
    )

    internal fun ofdSyncError(): TrilingualMessage = TrilingualMessage(
        ru = "Техническая ошибка синхронизации",
        kk = "Синхрондау қатесі",
        en = "Sync error"
    )

    internal fun ofdUnansweredDocument(): String = TrilingualMessage(
        ru = "БФД ещё не ответил на отправленный документ: связь проверится его досылкой",
        kk = "БФД жіберілген құжатқа әлі жауап бермеді: байланыс оны қайта жіберу арқылы тексеріледі",
        en = "The BFD has not answered a sent document yet: the connection is checked by resending it"
    ).compact()

    internal fun noAdapterForChannel(channel: String): TrilingualMessage = TrilingualMessage(
        ru = "Нет адаптера для канала $channel",
        kk = "$channel арнасы үшін адаптер жоқ",
        en = "No adapter for channel $channel"
    )

    internal fun ofdErrorReason(): TrilingualMessage = TrilingualMessage(
        ru = "Ошибка передачи в БФД: Документ не доставлен в налоговый орган.",
        kk = "БФД-ға жіберу қатесі: Құжат салық органына жеткізілмеді.",
        en = "BFD delivery failure: Document not delivered to tax authority."
    )

    internal fun statusError(): TrilingualMessage = TrilingualMessage(
        ru = "Ошибка",
        kk = "Қате",
        en = "Error"
    )

    internal fun paymentType(code: String): TrilingualMessage = when (code) {
        "CASH" -> TrilingualMessage("Наличные средства", "Қолма-қол ақша", "Cash")
        "CARD" -> TrilingualMessage("Платежная карта", "Төлем картасы", "Payment Card")
        "ELECTRONIC" -> TrilingualMessage("Электронные деньги", "Электрондық ақша", "Electronic Money")
        "MOBILE" -> TrilingualMessage("Мобильный платеж (QR)", "Мобильді төлем (QR)", "Mobile Payment (QR)")
        "CREDIT" -> TrilingualMessage("Оплата в кредит", "Несиеге төлеу", "Credit Payment")
        "TARE" -> TrilingualMessage("Оплата тарой", "Ыдыспен төлеу", "Payment by Tare")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun documentType(code: String): TrilingualMessage = when (code) {
        "SHIFT_OPEN", "OPEN_SHIFT" -> TrilingualMessage("Открытие смены", "Ауысымды ашу", "Shift Open")
        "SHIFT_CLOSE", "CLOSE_SHIFT", "COMMAND_CLOSE_SHIFT", "Z_REPORT" -> TrilingualMessage(
            "Закрытие смены (Z-отчёт)",
            "Ауысымды жабу (Z-есеп)",
            "Shift Close (Z-Report)"
        )
        "SALE" -> TrilingualMessage("Продажа", "Сату", "Sale")
        "RETURN" -> TrilingualMessage("Возврат продажи", "Сатуды қайтару", "Return")
        "BUY" -> TrilingualMessage("Покупка", "Сатып алу", "Buy")
        "BUY_RETURN" -> TrilingualMessage("Возврат покупки", "Сатып алуды қайтару", "Buy Return")
        "CASH_IN" -> TrilingualMessage("Внесение наличных", "Қолма-қол ақшаны салу", "Cash In")
        "CASH_OUT" -> TrilingualMessage("Изъятие наличных", "Қолма-қол ақшаны алу", "Cash Out")
        "X_REPORT" -> TrilingualMessage("X-отчёт", "X-есеп", "X-Report")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun userRole(code: String): TrilingualMessage = when (code) {
        "ADMIN" -> TrilingualMessage("Администратор", "Әкімші", "Administrator")
        "CASHIER" -> TrilingualMessage("Кассир", "Кассир", "Cashier")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun taxRegime(code: String): TrilingualMessage = when (code) {
        "NO_VAT" -> TrilingualMessage("Без НДС", "ҚҚС-сыз", "No VAT")
        "VAT_PAYER" -> TrilingualMessage("Плательщик НДС", "ҚҚС төлеуші", "VAT Payer")
        "MIXED" -> TrilingualMessage("Смешанный режим", "Аралас режим", "Mixed Regime")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun vatGroup(code: String): TrilingualMessage = when (code) {
        "NO_VAT" -> TrilingualMessage("Без НДС", "ҚҚС-сыз", "No VAT")
        "VAT_0" -> TrilingualMessage("НДС 0%", "ҚҚС 0%", "VAT 0%")
        "VAT_5" -> TrilingualMessage("НДС 5%", "ҚҚС 5%", "VAT 5%")
        "VAT_10" -> TrilingualMessage("НДС 10%", "ҚҚС 10%", "VAT 10%")
        "VAT_12" -> TrilingualMessage("НДС 12%", "ҚҚС 12%", "VAT 12%")
        "VAT_16" -> TrilingualMessage("НДС 16%", "ҚҚС 16%", "VAT 16%")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun receiptDomainType(code: String): TrilingualMessage = when (code) {
        "DOMAIN_TRADING" -> TrilingualMessage("Торговля", "Сауда", "Trading")
        "DOMAIN_SERVICES" -> TrilingualMessage("Сфера услуг", "Қызмет көрсету саласы", "Services")
        "DOMAIN_GASOIL" -> TrilingualMessage("Нефтепродукты", "Мұнай өнімдері", "Petroleum Products")
        "DOMAIN_HOTELS" -> TrilingualMessage("Гостиницы", "Қонақ үйлер", "Hotels")
        "DOMAIN_TAXI" -> TrilingualMessage("Такси", "Такси", "Taxi")
        "DOMAIN_PARKING" -> TrilingualMessage("Стоянка", "Тұрақ", "Parking")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun unitOfMeasurement(code: String): TrilingualMessage = when (code) {
        "PIECE" -> TrilingualMessage("Штука", "Дана", "Piece")
        "KILOGRAM" -> TrilingualMessage("Килограмм", "Килограмм", "Kilogram")
        "SERVICE" -> TrilingualMessage("Услуга", "Қызмет", "Service")
        "METER" -> TrilingualMessage("Метр", "Метр", "Meter")
        "LITER" -> TrilingualMessage("Литр", "Литр", "Liter")
        "LINEAR_METER" -> TrilingualMessage("Погонный метр", "Өткел қума метр", "Linear Meter")
        "TON" -> TrilingualMessage("Тонна", "Тонна", "Ton")
        "HOUR" -> TrilingualMessage("Час", "Сағат", "Hour")
        "DAY" -> TrilingualMessage("Сутки", "Тәулік", "Day")
        "WEEK" -> TrilingualMessage("Неделя", "Апта", "Week")
        "MONTH" -> TrilingualMessage("Месяц", "Ай", "Month")
        "MILLIMETER" -> TrilingualMessage("Миллиметр", "Миллиметр", "Millimeter")
        "CENTIMETER" -> TrilingualMessage("Сантиметр", "Сантиметр", "Centimeter")
        "DECIMETER" -> TrilingualMessage("Дециметр", "Дециметр", "Decimeter")
        "UNIT" -> TrilingualMessage("Единица", "Бірлік", "Unit")
        "KILOMETER" -> TrilingualMessage("Километр", "Километр", "Kilometer")
        "HECTOGRAM" -> TrilingualMessage("Гектограмм", "Гектограмм", "Hectogram")
        "MILLIGRAM" -> TrilingualMessage("Миллиграмм", "Миллиграмм", "Milligram")
        "METRIC_CARAT" -> TrilingualMessage("Метрический карат", "Метрлік карат", "Metric Carat")
        "GRAM" -> TrilingualMessage("Грамм", "Грамм", "Gram")
        "MICROGRAM" -> TrilingualMessage("Микрограмм", "Микрограмм", "Microgram")
        "CUBIC_MILLIMETER" -> TrilingualMessage("Кубический миллиметр", "Куб миллиметр", "Cubic Millimeter")
        "MILLILITER" -> TrilingualMessage("Миллилитр", "Миллилитр", "Milliliter")
        "SQUARE_METER" -> TrilingualMessage("Квадратный метр", "Шаршы метр", "Square Meter")
        "HECTARE" -> TrilingualMessage("Гектар", "Гектар", "Hectare")
        "SQUARE_KILOMETER" -> TrilingualMessage("Квадратный километр", "Шаршы километр", "Square Kilometer")
        "SHEET" -> TrilingualMessage("Лист", "Парақ", "Sheet")
        "PACK" -> TrilingualMessage("Пачка", "Бума", "Pack")
        "ROLL" -> TrilingualMessage("Рулон", "Орам", "Roll")
        "PACKAGE" -> TrilingualMessage("Упаковка", "Орама", "Package")
        "BOTTLE" -> TrilingualMessage("Бутылка", "Бөтелке", "Bottle")
        "WORK" -> TrilingualMessage("Работа", "Жұмыс", "Work")
        "CUBIC_METER" -> TrilingualMessage("Метр кубический", "Куб метр", "Cubic Meter")
        else -> TrilingualMessage("Неизвестно", "Белгісіз", "Unknown")
    }

    internal fun paperWidth(code: String): TrilingualMessage = when (code) {
        "WIDTH_58" -> TrilingualMessage("58мм", "58мм", "58mm")
        "WIDTH_80" -> TrilingualMessage("80мм", "80мм", "80mm")
        "FULLSCREEN" -> TrilingualMessage("Полноэкранный", "Толық экранды", "Fullscreen")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun brandingColor(code: String): TrilingualMessage = when (code) {
        "DARK_PURPLE" -> TrilingualMessage(
            "Темно-фиолетовый (По умолчанию)",
            "Күңгірт-күлгін (Әдепкі)",
            "Dark Purple (Default)"
        )
        "DEEP_BLACK" -> TrilingualMessage("Глубокий черный", "Қара", "Deep Black")
        "BLUE" -> TrilingualMessage("Синий", "Көк", "Blue")
        "GREEN" -> TrilingualMessage("Зеленый", "Жасыл", "Green")
        "ORANGE" -> TrilingualMessage("Оранжевый", "Қызғылт сары", "Orange")
        "RED" -> TrilingualMessage("Красный", "Қызыл", "Red")
        "PURPLE" -> TrilingualMessage("Фиолетовый", "Күлгін", "Purple")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun kkmState(code: String): TrilingualMessage = when (code) {
        "REGISTRATION" -> TrilingualMessage("Регистрация", "Тіркеу", "Registration")
        "ACTIVE" -> TrilingualMessage("Активна", "Белсенді", "Active")
        "ONLINE" -> TrilingualMessage("Онлайн", "Онлайн", "Online")
        "OFFLINE_QUEUE" -> TrilingualMessage("Офлайн очередь", "Офлайн кезегі", "Offline Queue")
        "NO_TOKEN" -> TrilingualMessage("Нет токена БФД", "БФД токені жоқ", "No BFD Token")
        "BLOCKED" -> TrilingualMessage("Заблокирована", "Блокталған", "Blocked")
        "PROGRAMMING" -> TrilingualMessage("Программирование", "Бағдарламалау", "Programming")
        "IDLE" -> TrilingualMessage("Смена закрыта", "Ауысым жабық", "Idle")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun kkmLabelMode(): TrilingualMessage = TrilingualMessage(
        ru = "Режим работы",
        kk = "Жұмыс режимі",
        en = "Operating Mode"
    )

    internal fun kkmLabelConnection(): TrilingualMessage = TrilingualMessage(
        ru = "Связь с БФД",
        kk = "БФД байланысы",
        en = "BFD Connection"
    )

    internal fun kkmStatusOnline(): TrilingualMessage = TrilingualMessage(
        ru = "Онлайн",
        kk = "Онлайн",
        en = "Online"
    )

    internal fun kkmStatusOfflineSince(date: String): TrilingualMessage = TrilingualMessage(
        ru = "Автономный с $date",
        kk = "$date бастап автономды",
        en = "Offline since $date"
    )

    internal fun kkmMode(code: String): TrilingualMessage = when (code) {
        "ONLINE" -> TrilingualMessage("Онлайн", "Онлайн", "Online")
        "OFFLINE" -> TrilingualMessage("Автономный (Офлайн)", "Автономды (Офлайн)", "Autonomous (Offline)")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun shiftStatus(code: String): TrilingualMessage = when (code) {
        "OPEN" -> TrilingualMessage("Открыта", "Ашық", "Open")
        "CLOSED" -> TrilingualMessage("Закрыта", "Жабық", "Closed")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun deliveryStatus(code: String): TrilingualMessage = when (code) {
        "ONLINE_OK" -> TrilingualMessage("Доставлен", "Жеткізілді", "Delivered")
        "ONLINE_ERROR" -> TrilingualMessage("Ошибка доставки", "Жеткізу қатесі", "Delivery Error")
        "OFFLINE_QUEUED" -> TrilingualMessage("В офлайн-очереди", "Офлайн-кезекте", "Offline Queued")
        "NOT_SENT" -> TrilingualMessage("Не отправлен", "Жіберілмеді", "Not Sent")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun ofdCommandStatus(code: String): TrilingualMessage = when (code) {
        "OK" -> TrilingualMessage("Успешно", "Сәтті", "Success")
        "TIMEOUT" -> TrilingualMessage("Таймаут", "Күту уақыты бітті", "Timeout")
        "FAILED" -> TrilingualMessage("Ошибка", "Қате", "Failed")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun ofdDocumentStatus(code: String): TrilingualMessage = when (code) {
        "DELIVERED" -> TrilingualMessage("Доставлен", "Жеткізілді", "Delivered")
        "SENT" -> TrilingualMessage("Отправлен", "Жіберілді", "Sent")
        "PENDING" -> TrilingualMessage("В процессе", "Күтуде", "Pending")
        "FAILED" -> TrilingualMessage("Ошибка", "Қате", "Failed")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun receiptOperationType(code: String): TrilingualMessage = when (code) {
        "SELL" -> TrilingualMessage("Продажа", "Сату", "Sale")
        "SELL_RETURN" -> TrilingualMessage("Возврат продажи", "Сатуды қайтару", "Sale Return")
        "BUY" -> TrilingualMessage("Покупка", "Сатып алу", "Buy")
        "BUY_RETURN" -> TrilingualMessage("Возврат покупки", "Сатып алуды қайтару", "Buy Return")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun ofdEnvironment(code: String): TrilingualMessage = when (code) {
        "DEV" -> TrilingualMessage("Стенд разработки", "Әзірлеу стенді", "Development Environment")
        "TEST" -> TrilingualMessage("Тестовый стенд", "Тестілік стенд", "Test Environment")
        "PROD" -> TrilingualMessage("Продуктивный сервер", "Продуктивті сервер", "Production Environment")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun ofdProvider(code: String): TrilingualMessage = when (code) {
        "KAZAKHTELECOM" -> TrilingualMessage("АО «Казахтелеком»", "«Қазақтелеком» АҚ", "JSC Kazakhtelecom")
        "BFD" -> TrilingualMessage("ТОО «БФД»", "«БФД» ЖШС", "BFD LLP")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun coreMode(code: String): TrilingualMessage = when (code) {
        "DESKTOP" -> TrilingualMessage("Локальный (Десктоп)", "Локальді (Десктоп)", "Local (Desktop)")
        "SERVER" -> TrilingualMessage("Серверный (Кластер)", "Серверлік (Кластер)", "Server (Cluster)")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun authMode(code: String): TrilingualMessage = when (code) {
        "NONE" -> TrilingualMessage("Без авторизации", "Авторизациясыз", "No Authentication")
        "BEARER" -> TrilingualMessage("Bearer токен", "Bearer токені", "Bearer Token")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun receiptLanguage(code: String): TrilingualMessage = when (code) {
        "RU" -> TrilingualMessage("Русский", "Орысша", "Russian")
        "KK" -> TrilingualMessage("Казахский", "Қазақша", "Kazakh")
        "MIXED" -> TrilingualMessage("Смешанный (Двуязычный)", "Аралас (Екі тілді)", "Mixed (Bilingual)")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun receiptLayoutType(code: String): TrilingualMessage = when (code) {
        "TAPE_80MM" -> TrilingualMessage("Чековая лента 80мм", "Шекаралық таспа 80мм", "80mm Receipt Tape")
        "TAPE_58MM" -> TrilingualMessage("Чековая лента 58мм", "Шекаралық таспа 58мм", "58mm Receipt Tape")
        "FULLSCREEN" -> TrilingualMessage("Полноэкранный (A4/А5)", "Толық экранды (A4/А5)", "Fullscreen (A4/A5)")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun printDocumentType(code: String): TrilingualMessage = when (code) {
        "DOCUMENT" -> TrilingualMessage(
            "Фискальный документ (Чек)",
            "Фискалдық құжат (Шек)",
            "Fiscal Document (Receipt)"
        )
        "X_REPORT", "REPORT_X" -> TrilingualMessage(
            "Сменный отчёт без гашения (X-отчёт)",
            "Ауысымдық есеп өшірусіз (X-есеп)",
            "Shift Report without Clearing (X-Report)"
        )
        "OPEN_SHIFT", "SHIFT_OPEN" -> TrilingualMessage(
            "Документ открытия смены",
            "Ауысымды ашу құжаты",
            "Shift Opening Document"
        )
        "CLOSE_SHIFT", "SHIFT_CLOSE", "COMMAND_CLOSE_SHIFT", "Z_REPORT" -> TrilingualMessage(
            "Сменный отчёт с гашением (Z-отчёт)",
            "Ауысымдық есеп өшірумен (Z-есеп)",
            "Shift Report with Clearing (Z-Report)"
        )
        else -> TrilingualMessage(code, code, code)
    }

    internal fun ofdCommandType(code: String): TrilingualMessage = when (code) {
        "TICKET" -> TrilingualMessage("Команда чека", "Шек командасы", "Ticket Command")
        "SYSTEM" -> TrilingualMessage("Системная команда", "Жүйелік команда", "System Command")
        "INFO" -> TrilingualMessage("Запрос информации", "Ақпарат сұрау", "Info Request")
        "MONEY_PLACEMENT" -> TrilingualMessage(
            "Команда внесения/изъятия наличных",
            "Салым/алу командасы",
            "Money Placement Command"
        )
        "REPORT" -> TrilingualMessage("Команда X-отчёта", "X-есеп командасы", "X-Report Command")
        "CLOSE_SHIFT", "SHIFT_CLOSE", "COMMAND_CLOSE_SHIFT", "Z_REPORT" -> TrilingualMessage(
            "Команда закрытия смены (Z-отчёт)",
            "Ауысымды жабу командасы (Z-есеп)",
            "Close Shift Command (Z-Report)"
        )
        "NOMENCLATURE" -> TrilingualMessage("Команда номенклатуры", "Номенклатура командасы", "Nomenclature Command")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun cashOperationType(code: String): TrilingualMessage = when (code) {
        "CASH_IN" -> TrilingualMessage("Внесение наличных", "Қолма-қол ақшаны салу", "Cash In")
        "CASH_OUT" -> TrilingualMessage("Изъятие наличных", "Алу", "Cash Out")
        else -> TrilingualMessage(code, code, code)
    }

    // --- OFD Code Handling (CPCR 2.0.3) ---
    internal fun blockReason(code: Int): TrilingualMessage = when (code) {
        2 -> TrilingualMessage(
            ru = "Не отправлены чеки (72 часа)",
            kk = "Чектер жіберілмеді (72 сағат)",
            en = "Receipts not sent (72 hours)"
        )
        1001 -> TrilingualMessage(
            ru = "Неизвестный ID устройства в БФД",
            kk = "БФД жүйесінде белгісіз құрылғы ID",
            en = "Unknown device ID in BFD"
        )
        1002 -> TrilingualMessage(
            ru = "Неверный токен. Произведите сброс/авторизацию",
            kk = "Қате токен. Токенді қайта орнатыңыз",
            en = "Invalid token. Please reauthorize"
        )
        1003 -> TrilingualMessage(
            ru = "Ошибка протокола БФД",
            kk = "БФД хаттамасының қатесі",
            en = "BFD protocol error"
        )
        1004 -> TrilingualMessage(
            ru = "Неизвестная команда БФД",
            kk = "БФД жүйесінде белгісіз пәрмен",
            en = "Unknown BFD command"
        )
        1005 -> TrilingualMessage(
            ru = "Команда не поддерживается БФД",
            kk = "Пәрменді БФД қолдамайды",
            en = "Command not supported by BFD"
        )
        1006 -> TrilingualMessage(
            ru = "Неверные настройки БФД",
            kk = "БФД қате баптаулары",
            en = "Invalid BFD configuration"
        )
        1007 -> TrilingualMessage(
            ru = "SSL не разрешен БФД",
            kk = "БФД SSL рұқсат етпейді",
            en = "SSL is not allowed by BFD"
        )
        1011 -> TrilingualMessage(
            ru = "Смена открыта более 24 часов",
            kk = "Ауысым 24 сағаттан артық ашық",
            en = "Shift is open for more than 24 hours"
        )
        1012 -> TrilingualMessage(
            ru = "Неверный логин/пароль БФД",
            kk = "БФД логині/құпия сөзі қате",
            en = "Invalid BFD login/password"
        )
        1015 -> TrilingualMessage(
            ru = "Касса заблокирована БФД",
            kk = "Касса БФД жүйесінде бұғатталған",
            en = "Cash register is blocked by BFD"
        )
        else -> TrilingualMessage(
            ru = "Неизвестная причина блокировки (Код $code)",
            kk = "Белгісіз бұғаттау себебі (Код $code)",
            en = "Unknown block reason (Code $code)"
        )
    }

    internal fun documentFailedReason(ofdErrorCode: Int): TrilingualMessage = when (ofdErrorCode) {
        // Отказ на стороне кассы: документ не удалось собрать по протоколу ОФД.
        -1 -> TrilingualMessage(
            ru = "Чек не соответствует протоколу БФД. Проверьте данные и пробейте заново.",
            kk = "Чек БФД хаттамасына сәйкес келмейді. Мәліметтерді тексеріп қайта соғыңыз.",
            en = "Receipt does not conform to the BFD protocol. Fix and resend."
        )
        else -> BfdResultStrings.refusal(ofdErrorCode)
    }
}

/** Блокировка за смену, открытую дольше допустимого: ответ ОФД 11. */
private const val OPEN_SHIFT_TIMEOUT_BLOCK = 1011

/** Блокировка по недействительному токену: ответ ОФД 2. */
private const val INVALID_TOKEN_BLOCK = 1002

/** Блокировка за слишком долгую автономную работу: код самой кассы. */
private const val AUTONOMOUS_LIMIT_BLOCK = 2001
