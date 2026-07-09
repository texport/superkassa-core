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
}
