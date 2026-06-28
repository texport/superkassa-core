package kz.mybrain.superkassa.core.domain.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Аннотация для валидации наименования товара или услуги в чеке.
 *
 * Применяется к полям и параметрам значений для проверки на соответствие бизнес-требованиям.
 * Не допускаются обобщённые названия, такие как "Товар", "Продукты", "Товар один" и т. д.
 * Проверка делегируется валидатору [ItemNameValidator], который использует бизнес-правила [ItemNameRules].
 *
 * @property message Сообщение об ошибке по умолчанию, если валидация не пройдена.
 * @property groups Группы валидации, к которым относится данное ограничение.
 * @property payload Полезная нагрузка, ассоциированная с ограничением.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ItemNameValidator::class])
annotation class ItemNameValid(
    val message: String = "Укажите конкретное наименование товара/услуги (не допускаются: Товар, Продукт, Продукты, Товар один и т.п.)",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
