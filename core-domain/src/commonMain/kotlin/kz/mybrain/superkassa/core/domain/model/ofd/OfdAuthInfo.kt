package kz.mybrain.superkassa.core.domain.model.ofd

/**
 * Авторизационные данные для подключения к ОФД.
 *
 * @property token Токен авторизации, выданный сервером ОФД (null, если авторизация еще не выполнена).
 * @property nextReqNum Порядковый номер следующего ожидаемого запроса.
 */
data class OfdAuthInfo(
    val token: String?,
    val nextReqNum: Int
)
