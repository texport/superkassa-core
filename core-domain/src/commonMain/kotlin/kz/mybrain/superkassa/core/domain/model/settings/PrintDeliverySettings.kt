package kz.mybrain.superkassa.core.domain.model.settings

/**
 * Настройки каналов доставки печатных форм чеков.
 *
 * @property enabled Флаг, указывающий, включена ли печать чеков.
 * @property paperWidthMm Ширина чековой ленты в миллиметрах (по умолчанию 58 мм).
 * @property connection Настройки подключения к принтеру чеков, если применимо.
 */
data class PrintDeliverySettings(
    val enabled: Boolean = true,
    val paperWidthMm: Int = 58,
    val connection: PrintConnectionSettings? = null
)
