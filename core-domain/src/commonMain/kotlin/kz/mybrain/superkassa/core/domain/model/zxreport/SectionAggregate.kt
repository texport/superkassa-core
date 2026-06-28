package kz.mybrain.superkassa.core.domain.model.zxreport

import kotlinx.serialization.Serializable

/**
 * Агрегированные показатели продаж в разрезе секций (отделов) за смену.
 *
 * @property sectionCode Уникальный код (номер) секции или отдела.
 * @property operations Список агрегированных фискальных операций ([OperationAggregate]) по данной секции.
 */
@Serializable
data class SectionAggregate(
    val sectionCode: String,
    val operations: List<OperationAggregate>
)
