package kz.mybrain.superkassa.core.domain.exception

import kz.mybrain.superkassa.core.domain.model.auth.UserRole

/**
 * Реестр мультиязычных шаблонов и сообщений об ошибках в домене Superkassa.
 *
 * Предоставляет централизованные фабричные методы для создания локализованных
 * сообщений [TrilingualMessage], используемых во всех доменных исключениях.
 */
@Suppress(
    "unused"
) // Публичный справочник ошибок; некоторые функции могут вызываться только из слоя презентации или тестов
object ErrorMessages {
    fun badRequest(): TrilingualMessage = TrilingualMessage(
        ru = "Некорректный запрос",
        kk = "Қате сұраныс",
        en = "Bad request"
    )


    fun kkmNotFound(): TrilingualMessage = TrilingualMessage(
        ru = "ККМ не найдена",
        kk = "ККМ табылмады",
        en = "KKM not found"
    )

    fun kkmExists(): TrilingualMessage = TrilingualMessage(
        ru = "ККМ уже зарегистрирована",
        kk = "ККМ тіркеліп қойған",
        en = "KKM already exists"
    )

    fun shiftNotOpen(): TrilingualMessage = TrilingualMessage(
        ru = "Смена не открыта",
        kk = "Ауысым ашылмаған",
        en = "Shift is not open"
    )

    fun shiftAlreadyOpen(): TrilingualMessage = TrilingualMessage(
        ru = "Смена уже открыта",
        kk = "Ауысым ашылып қойған",
        en = "Shift already open"
    )

    fun shiftTooLong(): TrilingualMessage = TrilingualMessage(
        ru = "Смена превышает 24 часа",
        kk = "Ауысым 24 сағаттан асады",
        en = "Shift exceeds 24 hours"
    )

    fun systemTimeInvalid(): TrilingualMessage = TrilingualMessage(
        ru = "Неверное системное время",
        kk = "Жүйелік уақыт қате",
        en = "System time invalid"
    )


    fun ofdProviderUnknown(id: String): TrilingualMessage = TrilingualMessage(
        ru = "Неизвестный ОФД: $id",
        kk = "Белгісіз ОФД: $id",
        en = "Unknown OFD provider: $id"
    )

    fun ofdEnvironmentUnknown(env: String): TrilingualMessage = TrilingualMessage(
        ru = "Неизвестная площадка ОФД: $env",
        kk = "Белгісіз ОФД алаңы: $env",
        en = "Unknown OFD environment: $env"
    )

    fun ofdProviderRequired(): TrilingualMessage = TrilingualMessage(
        ru = "ОФД обязателен",
        kk = "ОФД міндетті",
        en = "OFD provider required"
    )

    fun ofdProviderTagInvalid(tag: String): TrilingualMessage = TrilingualMessage(
        ru = "Неверный тег ОФД: $tag",
        kk = "ОФД қате тегі: $tag",
        en = "Invalid OFD tag: $tag"
    )

    fun ofdTokenRequired(): TrilingualMessage = TrilingualMessage(
        ru = "Токен ОФД обязателен",
        kk = "ОФД токені міндетті",
        en = "OFD token required"
    )

    fun ofdTokenInvalid(token: String): TrilingualMessage = TrilingualMessage(
        ru = "Неверный токен ОФД: $token",
        kk = "ОФД қате токені: $token",
        en = "Invalid OFD token: $token"
    )


    fun kkmRegistrationRequired(): TrilingualMessage = TrilingualMessage(
        ru = "Регистрационный номер ККМ обязателен",
        kk = "ККМ тіркеу нөмірі міндетті",
        en = "KKM registration number required"
    )

    fun kkmFactoryRequired(): TrilingualMessage = TrilingualMessage(
        ru = "Заводской номер ККМ обязателен",
        kk = "ККМ зауыттық нөмірі міндетті",
        en = "KKM factory number required"
    )

    fun kkmSystemIdRequired(): TrilingualMessage = TrilingualMessage(
        ru = "systemId ККМ обязателен",
        kk = "ККМ systemId міндетті",
        en = "KKM systemId required"
    )

    fun kkmSystemIdInvalid(systemId: String): TrilingualMessage = TrilingualMessage(
        ru = "Неверный systemId ККМ: $systemId",
        kk = "ККМ қате systemId: $systemId",
        en = "Invalid KKM systemId: $systemId"
    )

    fun kkmSystemIdExists(systemId: String): TrilingualMessage = TrilingualMessage(
        ru = "systemId ККМ уже существует: $systemId",
        kk = "ККМ systemId бар: $systemId",
        en = "KKM systemId already exists: $systemId"
    )


    fun userPinRequired(): TrilingualMessage = TrilingualMessage(
        ru = "PIN пользователя обязателен",
        kk = "Пайдаланушының PIN-коды міндетті",
        en = "User PIN required"
    )

    fun userNotFound(): TrilingualMessage = TrilingualMessage(
        ru = "Пользователь не найден или неверный PIN",
        kk = "Пайдаланушы табылдады немесе PIN қате",
        en = "User not found or invalid PIN"
    )

    fun userForbidden(): TrilingualMessage = TrilingualMessage(
        ru = "У пользователя нет прав на операцию",
        kk = "Пайдаланушының операцияға құқығы жоқ",
        en = "Operation forbidden for role"
    )

    fun userNameRequired(): TrilingualMessage = TrilingualMessage(
        ru = "Имя пользователя обязательно",
        kk = "Пайдаланушының аты міндетті",
        en = "User name required"
    )

    fun userRoleRequired(role: UserRole): TrilingualMessage = TrilingualMessage(
        ru = "Нужен хотя бы один ${role.name}",
        kk = "Кем дегенде бір ${role.name} қажет",
        en = "At least one ${role.name} required"
    )

    fun userPinConflict(): TrilingualMessage = TrilingualMessage(
        ru = "PIN уже используется",
        kk = "PIN қолданылып қойған",
        en = "User PIN already exists"
    )

    /**
     * Возвращает ошибку при попытке использования стандартных небезопасных ПИН-кодов ("0000" или "1111").
     */
    fun defaultPinNotAllowed(): TrilingualMessage = TrilingualMessage(
        ru = "Использование стандартного ПИН-кода запрещено. Пожалуйста, смените ПИН-код в настройках.",
        kk = "Стандартты ПИН-кодты пайдалануға тыйым салынады. ПИН-кодты баптаулардан өзгертіңіз.",
        en = "Using default PIN is not allowed. Please change your PIN in settings."
    )

    fun userUpdateEmpty(): TrilingualMessage = TrilingualMessage(
        ru = "Нужно передать хотя бы одно поле для обновления пользователя",
        kk = "Пайдаланушыны жаңарту үшін кем дегенде бір өрісті беру керек",
        en = "User update requires at least one field"
    )


    fun kkmDeleteRequiresProgramming(): TrilingualMessage = TrilingualMessage(
        ru = "ККМ должна быть в режиме PROGRAMMING",
        kk = "ККМ PROGRAMMING режимінде болуы керек",
        en = "KKM must be in PROGRAMMING"
    )

    fun kkmDeleteShiftOpen(): TrilingualMessage = TrilingualMessage(
        ru = "Смена не закрыта",
        kk = "Ауысым жабылмаған",
        en = "Shift is open"
    )

    fun kkmDeleteQueueNotEmpty(): TrilingualMessage = TrilingualMessage(
        ru = "Очередь не пустая",
        kk = "Кезек бос емес",
        en = "Queue is not empty"
    )

    fun kkmSyncShiftOpen(): TrilingualMessage = TrilingualMessage(
        ru = "Смена не закрыта",
        kk = "Ауысым жабылмаған",
        en = "Shift is open"
    )

    fun kkmSyncQueueNotEmpty(): TrilingualMessage = TrilingualMessage(
        ru = "Очередь не пустая",
        kk = "Кезек бос емес",
        en = "Queue is not empty"
    )

    fun kkmAutonomousTooLong(): TrilingualMessage = TrilingualMessage(
        ru = "Автономный режим превышает 72 часа",
        kk = "Автономды режим 72 сағаттан асады",
        en = "Autonomous mode exceeds 72 hours"
    )

    fun kkmBlocked(): TrilingualMessage = TrilingualMessage(
        ru = "ККМ заблокирована по требованию ОФД",
        kk = "ККМ ОФД талабы бойынша бұғатталған",
        en = "KKM is blocked by OFD instruction"
    )


    fun kkmSettingsRequiresProgramming(): TrilingualMessage = TrilingualMessage(
        ru = "ККМ должна быть в режиме PROGRAMMING",
        kk = "ККМ PROGRAMMING режимінде болуы керек",
        en = "KKM must be in PROGRAMMING"
    )

    fun kkmInProgramming(): TrilingualMessage = TrilingualMessage(
        ru = "ККМ в режиме программирования",
        kk = "ККМ бағдарламалау режимінде",
        en = "KKM is in programming mode"
    )

    fun unauthorized(): TrilingualMessage = TrilingualMessage(
        ru = "Не авторизован",
        kk = "Авторизацияланбаған",
        en = "Unauthorized"
    )

    fun ofdRequestFailed(details: String?): TrilingualMessage = TrilingualMessage(
        ru = if (details.isNullOrBlank()) "Ошибка ОФД" else "Ошибка ОФД: $details",
        kk = if (details.isNullOrBlank()) "ОФД қатесі" else "ОФД қатесі: $details",
        en = if (details.isNullOrBlank()) "OFD request failed" else "OFD request failed: $details"
    )

    fun measureUnitCodeInvalid(code: String): TrilingualMessage = TrilingualMessage(
        ru = "Неверный код единицы измерения: $code",
        kk = "Өлшем бірлігінің қате коды: $code",
        en = "Invalid measure unit code: $code"
    )

    fun measureUnitNotFound(code: String): TrilingualMessage = TrilingualMessage(
        ru = "Единица измерения не найдена: $code",
        kk = "Өлшем бірлігі табылмады: $code",
        en = "Unit of measurement not found: $code"
    )

    fun documentNotFound(): TrilingualMessage = TrilingualMessage(
        ru = "Документ не найден или содержимое чека недоступно",
        kk = "Құжат табылмады немесе чектің мазмұны қолжетімсіз",
        en = "Document not found or receipt content unavailable"
    )

    fun okvedRequired(): TrilingualMessage = TrilingualMessage(
        ru = "ОКВЭД обязателен",
        kk = "ЭҚЖЖ міндетті",
        en = "OKVED is required"
    )
}
