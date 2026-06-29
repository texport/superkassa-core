package kz.mybrain.superkassa.core.presentation.annotations

import kotlin.reflect.KClass

/**
 * Аннотация для описания OpenAPI схем данных в KMP-моделях.
 *
 * Используется генераторами документации (например, Springdoc) на JVM-платформе,
 * а на iOS-платформах игнорируется благодаря @OptionalExpectation.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class Schema(
    val description: String = "",
    val example: String = "",
    val required: Boolean = false,
    val type: String = "",
    val format: String = "",
    val minimum: String = "",
    val maximum: String = "",
    val hidden: Boolean = false,
    val minLength: Int = 0,
    val maxLength: Int = Int.MAX_VALUE,
    val allowableValues: Array<String> = []
)

/**
 * Аннотация для запуска рекурсивной валидации объектов.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
@Target(AnnotationTarget.TYPE, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
expect annotation class Valid()

/**
 * Базовый интерфейс-маркер для группировки полезной нагрузки (payload) валидации.
 */
expect interface Payload

/**
 * Аннотация валидации: строка не должна быть пустой или состоять только из пробелов.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class NotBlank(
    val message: String = "{jakarta.validation.constraints.NotBlank.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

/**
 * Аннотация валидации: числовое значение должно быть не меньше указанного минимума.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class Min(
    val value: Long,
    val message: String = "{jakarta.validation.constraints.Min.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

/**
 * Аннотация валидации: числовое значение должно быть не больше указанного максимума.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class Max(
    val value: Long,
    val message: String = "{jakarta.validation.constraints.Max.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

/**
 * Аннотация валидации: размер коллекции или строки должен быть в пределах указанного диапазона.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class Size(
    val message: String = "{jakarta.validation.constraints.Size.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
    val min: Int = 0,
    val max: Int = Int.MAX_VALUE
)

/**
 * Аннотация валидации: десятичное значение должно быть не больше указанного максимума.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class DecimalMax(
    val value: String,
    val message: String = "{jakarta.validation.constraints.DecimalMax.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
    val inclusive: Boolean = true
)

/**
 * Аннотация валидации: десятичное значение должно быть не меньше указанного минимума.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class DecimalMin(
    val value: String,
    val message: String = "{jakarta.validation.constraints.DecimalMin.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
    val inclusive: Boolean = true
)

/**
 * Аннотация валидации: строка или коллекция не должна быть пустой.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class NotEmpty(
    val message: String = "{jakarta.validation.constraints.NotEmpty.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

/**
 * Аннотация валидации: значение не должно быть null.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class NotNull(
    val message: String = "{jakarta.validation.constraints.NotNull.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

/**
 * Аннотация валидации: числовое значение должно быть строго больше нуля.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class Positive(
    val message: String = "{jakarta.validation.constraints.Positive.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
