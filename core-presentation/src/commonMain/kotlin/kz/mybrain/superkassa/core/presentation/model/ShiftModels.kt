package kz.mybrain.superkassa.core.presentation.model

import kz.mybrain.superkassa.core.presentation.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Запрос на закрытие смены (Z-отчет).
 *
 * @property _unused Не используется.
 */
@Serializable
data class CloseShiftRequest(
    @kotlinx.serialization.Transient
    private val _unused: String? = null
)

/**
 * Запрос на автозакрытие смены.
 *
 * @property autoCloseShift Флаг включения автоматического закрытия смены.
 */
@Serializable
@Schema(description = "Запрос на обновление настройки автозакрытия смены")
data class AutoCloseShiftRequest(
    @Schema(description = "Включить/выключить автоматическое закрытие смены", example = "true")
    val autoCloseShift: Boolean
)

/**
 * Запрос на X-отчет (сменный отчет без гашения).
 *
 * @property _unused Не используется.
 */
@Serializable
data class XReportRequest(
    @kotlinx.serialization.Transient
    private val _unused: String? = null
)
