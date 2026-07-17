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
        kk = "ККМ табылмады",
        en = "KKM not found"
    )

    internal fun kkmExists(): TrilingualMessage = TrilingualMessage(
        ru = "ККМ уже зарегистрирована",
        kk = "ККМ тіркеліп қойған",
        en = "KKM already exists"
    )

    internal fun shiftNotOpen(): TrilingualMessage = TrilingualMessage(
        ru = "Смена не открыта",
        kk = "Ауысым ашылмаған",
        en = "Shift is not open"
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
        ru = "Неизвестный ОФД: $id",
        kk = "Белгісіз ОФД: $id",
        en = "Unknown OFD provider: $id"
    )

    internal fun ofdEnvironmentUnknown(env: String): TrilingualMessage = TrilingualMessage(
        ru = "Неизвестная площадка ОФД: $env",
        kk = "Белгісіз ОФД алаңы: $env",
        en = "Unknown OFD environment: $env"
    )

    internal fun ofdProviderRequired(): TrilingualMessage = TrilingualMessage(
        ru = "ОФД обязателен",
        kk = "ОФД міндетті",
        en = "OFD provider required"
    )

    internal fun ofdProviderTagInvalid(tag: String): TrilingualMessage = TrilingualMessage(
        ru = "Неверный тег ОФД: $tag",
        kk = "ОФД қате тегі: $tag",
        en = "Invalid OFD tag: $tag"
    )

    internal fun ofdTokenRequired(): TrilingualMessage = TrilingualMessage(
        ru = "Токен ОФД обязателен",
        kk = "ОФД токені міндетті",
        en = "OFD token required"
    )

    internal fun ofdTokenInvalid(token: String): TrilingualMessage = TrilingualMessage(
        ru = "Неверный токен ОФД: $token",
        kk = "ОФД қате токені: $token",
        en = "Invalid OFD token: $token"
    )

    internal fun kkmRegistrationRequired(): TrilingualMessage = TrilingualMessage(
        ru = "Регистрационный номер ККМ обязателен",
        kk = "ККМ тіркеу нөмірі міндетті",
        en = "KKM registration number required"
    )

    internal fun kkmFactoryRequired(): TrilingualMessage = TrilingualMessage(
        ru = "Заводской номер ККМ обязателен",
        kk = "ККМ зауыттық нөмірі міндетті",
        en = "KKM factory number required"
    )

    internal fun kkmSystemIdRequired(): TrilingualMessage = TrilingualMessage(
        ru = "systemId ККМ обязателен",
        kk = "ККМ systemId міндетті",
        en = "KKM systemId required"
    )

    internal fun kkmSystemIdInvalid(systemId: String): TrilingualMessage = TrilingualMessage(
        ru = "Неверный systemId ККМ: $systemId",
        kk = "ККМ қате systemId: $systemId",
        en = "Invalid KKM systemId: $systemId"
    )

    internal fun kkmSystemIdExists(systemId: String): TrilingualMessage = TrilingualMessage(
        ru = "systemId ККМ уже существует: $systemId",
        kk = "ККМ systemId бар: $systemId",
        en = "KKM systemId already exists: $systemId"
    )

    internal fun userPinRequired(): TrilingualMessage = TrilingualMessage(
        ru = "PIN пользователя обязателен",
        kk = "Пайдаланушының PIN-коды міндетті",
        en = "User PIN required"
    )

    internal fun userNotFound(): TrilingualMessage = TrilingualMessage(
        ru = "Пользователь не найден или неверный PIN",
        kk = "Пайдаланушы табылдады немесе PIN қате",
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
        ru = "Нужен хотя бы один $roleName",
        kk = "Кем дегенде бір $roleName қажет",
        en = "At least one $roleName required"
    )

    internal fun userPinConflict(): TrilingualMessage = TrilingualMessage(
        ru = "PIN уже используется",
        kk = "PIN қолданылып қойған",
        en = "User PIN already exists"
    )

    internal fun defaultPinNotAllowed(): TrilingualMessage = TrilingualMessage(
        ru = "Использование стандартного ПИН-кода запрещено. Пожалуйста, смените ПИН-код в настройках.",
        kk = "Стандартты ПИН-кодты пайдалануға тыйым салынады. ПИН-кодты баптаулардан өзгертіңіз.",
        en = "Using default PIN is not allowed. Please change your PIN in settings."
    )

    internal fun userUpdateEmpty(): TrilingualMessage = TrilingualMessage(
        ru = "Нужно передать хотя бы одно поле для обновления пользователя",
        kk = "Пайдаланушыны жаңарту үшін кем дегенде бір өрісті беру керек",
        en = "User update requires at least one field"
    )

    internal fun kkmDeleteRequiresProgramming(): TrilingualMessage = TrilingualMessage(
        ru = "ККМ должна быть в режиме PROGRAMMING",
        kk = "ККМ PROGRAMMING режимінде болуы керек",
        en = "KKM must be in PROGRAMMING"
    )

    internal fun kkmDeleteShiftOpen(): TrilingualMessage = TrilingualMessage(
        ru = "Смена не закрыта",
        kk = "Ауысым жабылмаған",
        en = "Shift is open"
    )

    internal fun kkmDeleteQueueNotEmpty(): TrilingualMessage = TrilingualMessage(
        ru = "Очередь не пустая",
        kk = "Кезек бос емес",
        en = "Queue is not empty"
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

    internal fun kkmAutonomousTooLong(): TrilingualMessage = TrilingualMessage(
        ru = "Автономный режим превышает 72 часа",
        kk = "Автономды режим 72 сағаттан асады",
        en = "Autonomous mode exceeds 72 hours"
    )

    internal fun kkmBlocked(): TrilingualMessage = TrilingualMessage(
        ru = "ККМ заблокирована по требованию ОФД",
        kk = "ККМ ОФД талабы бойынша бұғатталған",
        en = "KKM is blocked by OFD instruction"
    )

    internal fun kkmSettingsRequiresProgramming(): TrilingualMessage = TrilingualMessage(
        ru = "ККМ должна быть в режиме PROGRAMMING",
        kk = "ККМ PROGRAMMING режимінде болуы керек",
        en = "KKM must be in PROGRAMMING"
    )

    internal fun kkmInProgramming(): TrilingualMessage = TrilingualMessage(
        ru = "ККМ в режиме программирования",
        kk = "ККМ бағдарламалау режимінде",
        en = "KKM is in programming mode"
    )

    internal fun unauthorized(): TrilingualMessage = TrilingualMessage(
        ru = "Не авторизован",
        kk = "Авторизацияланбаған",
        en = "Unauthorized"
    )

    internal fun ofdRequestFailed(details: String?): TrilingualMessage = TrilingualMessage(
        ru = if (details.isNullOrBlank()) "Ошибка ОФД" else "Ошибка ОФД: $details",
        kk = if (details.isNullOrBlank()) "ОФД қатесі" else "ОФД қатесі: $details",
        en = if (details.isNullOrBlank()) "OFD request failed" else "OFD request failed: $details"
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

    internal fun documentNotFound(): TrilingualMessage = TrilingualMessage(
        ru = "Документ не найден или содержимое чека недоступно",
        kk = "Құжат табылмады немесе чектің мазмұны қолжетімсіз",
        en = "Document not found or receipt content unavailable"
    )

    internal fun okvedRequired(): TrilingualMessage = TrilingualMessage(
        ru = "ОКВЭД обязателен",
        kk = "ЭҚЖЖ міндетті",
        en = "OKVED is required"
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

    internal fun receiptDiscountScopesConflict(): TrilingualMessage = TrilingualMessage(
        ru = "Нельзя одновременно применять скидки на уровне позиций и на уровне всего чека",
        kk = "Бір чек ішінде позиция деңгейіндегі и бүкіл чек деңгейіндегі жеңілдіктерді бір уақытта қолдануға болмайды",
        en = "Cannot apply both item-level and receipt-level discounts in a single receipt"
    )

    // --- Технические ошибки инфраструктуры (Data/Infrastructure Errors) ---
    internal fun ofdRequestFailedData(details: String?): String {
        val errorText = details ?: "unknown"
        return "RU: Ошибка запроса к ОФД: $errorText | KK: ОФД-ға сұраныс қатесі: $errorText | EN: OFD request failed: $errorText"
    }

    // --- Сообщения очереди (Queue Messages) ---
    internal fun handlerException(reason: String): TrilingualMessage = TrilingualMessage(
        ru = "Ошибка связи или внутренняя ошибка кассы при обработке очереди. " +
            "Пожалуйста, убедитесь в наличии интернета и повторите попытку. (Технические детали: $reason)",
        kk = "Кезекпен жұмыс істеу кезінде байланыс немесе кассаның ішкі қатесі орын алды. " +
            "Интернет байланысын тексеріп, әрекетті қайталаңыз. (Техникалық мәліметтер: $reason)",
        en = "Connection failure or internal cashbox error while processing queue. " +
            "Please verify internet connection and retry the operation. (Technical details: $reason)"
    )

    internal fun invalidDispatchStatus(status: String): TrilingualMessage = TrilingualMessage(
        ru = "Внутренняя системная ошибка: получен некорректный статус обработки очереди ($status). Пожалуйста, обратитесь в службу поддержки.",
        kk = "Ішкі жүйелік қате: кезекті өңдеудің дұрыс емес мәртебесі алынды ($status). Қолдау қызметіне хабарласыңыз.",
        en = "Internal system error: received invalid queue processing status ($status). Please contact support."
    )

    internal fun ofdDeliveryFailure(errorMsg: String): TrilingualMessage = TrilingualMessage(
        ru = "Ошибка отправки в ОФД: $errorMsg",
        kk = "ОФД-ға жіберу қатесі: $errorMsg",
        en = "OFD delivery failure: $errorMsg"
    )

    internal fun ofdTimeout(): TrilingualMessage = TrilingualMessage(
        ru = "Тайм-аут ожидания ответа от ОФД",
        kk = "ОФД жауабын күту уақыты бітті",
        en = "OFD connection timeout"
    )

    internal fun noAdapterForChannel(channel: String): TrilingualMessage = TrilingualMessage(
        ru = "Нет адаптера для канала $channel",
        kk = "$channel арнасы үшін адаптер жоқ",
        en = "No adapter for channel $channel"
    )

    internal fun ofdErrorReason(): TrilingualMessage = TrilingualMessage(
        ru = "Ошибка передачи в ОФД: Документ не доставлен в налоговый орган.",
        kk = "ОФД-ға жіберу қатесі: Құжат салық органына жеткізілмеді.",
        en = "OFD delivery failure: Document not delivered to tax authority."
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
        else -> TrilingualMessage(code, code, code)
    }

    internal fun documentType(code: String): TrilingualMessage = when (code) {
        "SHIFT_OPEN" -> TrilingualMessage("Открытие смены", "Ауысымды ашу", "Shift Open")
        "SHIFT_CLOSE" -> TrilingualMessage("Закрытие смены", "Ауысымды жабу", "Shift Close")
        "SALE" -> TrilingualMessage("Продажа", "Сату", "Sale")
        "RETURN" -> TrilingualMessage("Возврат продажи", "Қайтару", "Return")
        "BUY" -> TrilingualMessage("Покупка", "Сатып алу", "Buy")
        "BUY_RETURN" -> TrilingualMessage("Возврат покупки", "Сатып алуды қайтару", "Buy Return")
        "CASH_IN" -> TrilingualMessage("Внесение наличных", "Салым", "Cash In")
        "CASH_OUT" -> TrilingualMessage("Изъятие наличных", "Изъятие", "Cash Out")
        "X_REPORT" -> TrilingualMessage("X-Отчет", "X-Есеп", "X-Report")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun userRole(code: String): TrilingualMessage = when (code) {
        "ADMIN" -> TrilingualMessage("Администратор", "Администратор", "Administrator")
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
        "VAT_16" -> TrilingualMessage("НДС 16%", "ҚҚС 16%", "VAT 16%")
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
        "DARK_PURPLE" -> TrilingualMessage("Темно-фиолетовый (По умолчанию)", "Күңгірт-күлгін (Әдепкі)", "Dark Purple (Default)")
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
        ru = "Связь с ОФД",
        kk = "ОФД байланысы",
        en = "OFD Connection"
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
        "TIMEOUT" -> TrilingualMessage("Таймаут", "Таймаут", "Timeout")
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
        "DOCUMENT" -> TrilingualMessage("Фискальный документ (Чек)", "Фискалдық құжат (Шек)", "Fiscal Document (Receipt)")
        "X_REPORT" -> TrilingualMessage("Сменный отчет без гашения (X-Отчет)", "Ауысымдық есеп өшірусіз (X-Есеп)", "Shift Report without Clearing (X-Report)")
        "OPEN_SHIFT" -> TrilingualMessage("Документ открытия смены", "Ауысымды ашу құжаты", "Shift Opening Document")
        "CLOSE_SHIFT" -> TrilingualMessage("Сменный отчет с гашением (Z-Отчет)", "Ауысымдық есеп өшірумен (Z-Есеп)", "Shift Report with Clearing (Z-Report)")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun ofdCommandType(code: String): TrilingualMessage = when (code) {
        "TICKET" -> TrilingualMessage("Команда чека", "Шек командасы", "Ticket Command")
        "SYSTEM" -> TrilingualMessage("Системная команда", "Жүйелік команда", "System Command")
        "INFO" -> TrilingualMessage("Запрос информации", "Ақпарат сұрау", "Info Request")
        "MONEY_PLACEMENT" -> TrilingualMessage("Команда внесения/изъятия наличных", "Салым/алу командасы", "Money Placement Command")
        "REPORT" -> TrilingualMessage("Команда X-отчета", "X-есеп командасы", "X-Report Command")
        "CLOSE_SHIFT" -> TrilingualMessage("Команда закрытия смены (Z-отчет)", "Ауысымды жабу командасы (Z-есеп)", "Close Shift Command (Z-Report)")
        "NOMENCLATURE" -> TrilingualMessage("Команда номенклатуры", "Номенклатура командасы", "Nomenclature Command")
        else -> TrilingualMessage(code, code, code)
    }

    internal fun cashOperationType(code: String): TrilingualMessage = when (code) {
        "CASH_IN" -> TrilingualMessage("Внесение наличных", "Салым", "Cash In")
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
            ru = "Неизвестный ID устройства в ОФД",
            kk = "ОФД жүйесінде белгісіз құрылғы ID",
            en = "Unknown device ID in OFD"
        )
        1002 -> TrilingualMessage(
            ru = "Неверный токен. Произведите сброс/авторизацию",
            kk = "Қате токен. Токенді қайта орнатыңыз",
            en = "Invalid token. Please reauthorize"
        )
        1003 -> TrilingualMessage(
            ru = "Ошибка протокола ОФД",
            kk = "ОФД хаттамасының қатесі",
            en = "OFD protocol error"
        )
        1004 -> TrilingualMessage(
            ru = "Неизвестная команда ОФД",
            kk = "ОФД жүйесінде белгісіз пәрмен",
            en = "Unknown OFD command"
        )
        1005 -> TrilingualMessage(
            ru = "Команда не поддерживается ОФД",
            kk = "Пәрменді ОФД қолдамайды",
            en = "Command not supported by OFD"
        )
        1006 -> TrilingualMessage(
            ru = "Неверные настройки ОФД",
            kk = "ОФД қате баптаулары",
            en = "Invalid OFD configuration"
        )
        1007 -> TrilingualMessage(
            ru = "SSL не разрешен ОФД",
            kk = "ОФД SSL рұқсат етпейді",
            en = "SSL is not allowed by OFD"
        )
        1011 -> TrilingualMessage(
            ru = "Смена открыта более 24 часов",
            kk = "Ауысым 24 сағаттан артық ашық",
            en = "Shift is open for more than 24 hours"
        )
        1012 -> TrilingualMessage(
            ru = "Неверный логин/пароль ОФД",
            kk = "ОФД логині/құпия сөзі қате",
            en = "Invalid OFD login/password"
        )
        1015 -> TrilingualMessage(
            ru = "Касса заблокирована ОФД",
            kk = "Касса ОФД жүйесінде бұғатталған",
            en = "Cash register is blocked by OFD"
        )
        else -> TrilingualMessage(
            ru = "Неизвестная причина блокировки (Код $code)",
            kk = "Белгісіз бұғаттау себебі (Код $code)",
            en = "Unknown block reason (Code $code)"
        )
    }

    internal fun documentFailedReason(ofdErrorCode: Int): TrilingualMessage = when (ofdErrorCode) {
        13 -> TrilingualMessage(
            ru = "Неверные данные чека. Проверьте данные и пробейте заново.",
            kk = "Чектің қате мәліметтері. Түзетіп қайта соғыңыз.",
            en = "Invalid receipt data. Fix and resend."
        )
        14 -> TrilingualMessage(
            ru = "Недостаточно наличных в кассе для операции.",
            kk = "Кассада операция үшін қолма-қол ақша жеткіліксіз.",
            en = "Not enough cash in the register."
        )
        17 -> TrilingualMessage(
            ru = "ИИН/БИН покупателя совпадает с продавцом.",
            kk = "Сатып алушының ЖСН/БСН сатушымен сәйкес келеді.",
            en = "Taxpayer and customer ID match."
        )
        else -> TrilingualMessage(
            ru = "Ошибка отправки в ОФД (Код $ofdErrorCode)",
            kk = "ОФД жүйесіне жіберу қатесі (Код $ofdErrorCode)",
            en = "Error sending to OFD (Code $ofdErrorCode)"
        )
    }
}
