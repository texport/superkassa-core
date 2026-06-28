package kz.mybrain.superkassa.core.domain.model.ofd

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
                checkDomain = "dev.consumer.oofd.kz"
            ),
            OfdEnvironment.PROD to OfdEndpoint(
                host = "xml.oofd.kz",
                port = 7777,
                checkDomain = "consumer.oofd.kz"
            )
        )
    );

    companion object {
        /**
         * Находит провайдера ОФД по его строковому идентификатору (регистронезависимо).
         */
        fun findProvider(providerId: String): OfdProvider? =
            entries.firstOrNull { it.id.equals(providerId, ignoreCase = true) }
    }
}
