package kz.mybrain.superkassa.core.domain.usecase.ofd

import kz.mybrain.superkassa.core.domain.model.ofd.OfdAuthInfo
import kz.mybrain.superkassa.core.domain.port.TokenCodecPort
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import kz.mybrain.superkassa.core.domain.exception.ValidationException

/**
 * Сценарий (Use Case) получения данных авторизации для работы с ОФД.
 *
 * Предоставляет расшифрованный токен авторизации и следующий порядковый номер запроса без фиксации
 * изменений счетчика в базе данных. Доступен только для пользователей с ролью Администратор.
 *
 * @property authorizeUserUseCase Сценарий проверки прав доступа и состояния ККМ.
 * @property tokenCodec Кодек для шифрования и дешифрования токенов ОФД.
 * @property generateRequestNumberUseCase Сценарий для работы с порядковыми номерами запросов ОФД.
 */
@Suppress("unused")
class GetOfdAuthInfoUseCase(
    private val authorizeUserUseCase: AuthorizeUserUseCase,
    private val tokenCodec: TokenCodecPort,
    private val generateRequestNumberUseCase: GenerateRequestNumberUseCase
) {
    /**
     * Возвращает авторизационные данные для ОФД.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param pin PIN-код для проверки полномочий Администратора.
     * @return [OfdAuthInfo] Объект, содержащий расшифрованный токен и следующий номер запроса.
     * @throws ValidationException если ККМ не найдена или PIN-код неверный.
     * @throws kz.mybrain.superkassa.core.domain.exception.ForbiddenException если у пользователя недостаточно прав.
     */
    fun execute(kkmId: String, pin: String): OfdAuthInfo {
        // Проверяем, что у пользователя есть права Администратора
        authorizeUserUseCase.requireRole(kkmId, pin, setOf(kz.mybrain.superkassa.core.domain.model.auth.UserRole.ADMIN))

        // Получаем информацию о ККМ
        val kkm = authorizeUserUseCase.requireKkm(kkmId)

        // Расшифровываем сохраненный зашифрованный токен ОФД
        val token = tokenCodec.decodeToken(kkm.tokenEncryptedBase64)?.toString()

        // Получаем следующий номер запроса к ОФД без записи его в базу данных (для предпросмотра)
        val nextReq = generateRequestNumberUseCase.execute(kkmId, persist = false)

        return OfdAuthInfo(
            token = token,
            nextReqNum = nextReq
        )
    }
}
