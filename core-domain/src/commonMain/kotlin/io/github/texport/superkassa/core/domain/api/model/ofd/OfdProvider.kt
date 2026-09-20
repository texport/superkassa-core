package io.github.texport.superkassa.core.domain.api.model.ofd

/**
 * Перечисление поддерживаемых провайдеров ОФД.
 *
 * @property id Уникальный текстовый код провайдера.
 * @property nameRu Официальное наименование на русском языке.
 * @property nameKk Официальное наименование на казахском языке.
 * @property website Ссылка на веб-сайт провайдера.
 * @property endpoints Карта соответствия окружения ([OfdEnvironment]) сетевой точке подключения ([OfdEndpoint]).
 */
enum class OfdProvider(
    val id: String,
    val nameRu: String,
    val nameKk: String,
    val website: String,
    val endpoints: Map<OfdEnvironment, OfdEndpoint>
) {
    /** АО «Казахтелеком» */
    KAZAKHTELECOM(
        id = "KAZAKHTELECOM",
        nameRu = "АО «Казахтелеком»",
        nameKk = "«Қазақтелеком» АҚ",
        website = "oofd.kz",
        endpoints = mapOf(
            OfdEnvironment.TEST to OfdEndpoint(
                host = "37.150.215.187",
                port = 7777,
                checkDomain = "consumer.test-oofd.kz"
            ),
            OfdEnvironment.PROD to OfdEndpoint(
                host = "10.8.29.11",
                port = 7777,
                checkDomain = "consumer.oofd.kz"
            )
        )
    ),

    /** ТОО «БФД» */
    BFD(
        id = "BFD",
        nameRu = "ОФД БФД",
        nameKk = "БФД ОФД",
        website = "ofd.example.kz",
        endpoints = mapOf(
            // Стенд разработки БФД: сервис приёма данных от касс в контуре
            // ECC, доступен по VPN. Чеки проверяются на receipt.ecc.kz.
            OfdEnvironment.DEV to OfdEndpoint(
                host = "192.168.10.123",
                port = 7778,
                checkDomain = "receipt.ecc.kz"
            ),
            // Локальный стенд сервиса приёма на машине в сети разработчика.
            OfdEnvironment.TEST to OfdEndpoint(
                host = "192.168.50.35",
                port = 17700,
                checkDomain = "receipt.example.kz"
            )
        )
    );

    /** Принимает ли провайдер этот контур: адрес известен только для перечисленных. */
    fun supports(environment: OfdEnvironment): Boolean = endpoints.containsKey(environment)

    /**
     * Адрес, по которому касса отправляет команды в этом контуре.
     *
     * @param environment контур ОФД.
     * @return точка подключения либо `null`, если для контура адреса нет.
     */
    fun endpoint(environment: OfdEnvironment): OfdEndpoint? = endpoints[environment]

    companion object {
        /**
         * Находит провайдера ОФД по его строковому идентификатору (регистронезависимо).
         */
        fun findProvider(providerId: String): OfdProvider? =
            entries.firstOrNull { it.id.equals(providerId, ignoreCase = true) }
    }
}
