package kz.mybrain.superkassa.core.domain.model.ofd

/**
 * Конфигурационные параметры конкретного провайдера ОФД.
 *
 * @property nameRu Официальное наименование на русском языке.
 * @property nameKk Официальное наименование на казахском языке.
 * @property website Ссылка на веб-сайт провайдера.
 * @property checkDomain Домен для проверки статуса отправленных чеков потребителем.
 */
data class OfdProviderConfig(
    val nameRu: String,
    val nameKk: String,
    val website: String,
    val checkDomain: String
)
