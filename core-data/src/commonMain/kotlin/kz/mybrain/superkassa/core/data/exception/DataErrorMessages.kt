package kz.mybrain.superkassa.core.data.exception

/**
 * Реестр технических ошибок слоя инфраструктуры (data).
 *
 * Форматирует сообщения об ошибках на трех языках (RU, KK, EN) без зависимости от слоя домена.
 * Используется для предоставления детализированной информации во внешних ответах и логах.
 */
object DataErrorMessages {
    
    /**
     * Форматирует сообщение об ошибке запроса к ОФД.
     *
     * @param details Дополнительные технические детали ошибки (может быть null).
     * @return Локализованная строка сообщения об ошибке на русском, казахском и английском языках.
     */
    fun ofdRequestFailed(details: String?): String {
        val errorText = details ?: "unknown"
        return "RU: Ошибка запроса к ОФД: $errorText | KK: ОФД-ға сұраныс қатесі: $errorText | EN: OFD request failed: $errorText"
    }
}
