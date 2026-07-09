package io.github.texport.superkassa.core.presentation.api.annotations

/**
 * Фактическая JVM-реализация аннотации Schema.
 *
 * Связывает метаданные OpenAPI с классами моделей презентации на платформе JVM.
 */
actual annotation class Schema(
    actual val description: String = "",
    actual val example: String = "",
    actual val required: Boolean = false,
    actual val type: String = "",
    actual val format: String = "",
    actual val minimum: String = "",
    actual val maximum: String = "",
    actual val hidden: Boolean = false,
    actual val minLength: Int = 0,
    actual val maxLength: Int = Int.MAX_VALUE,
    actual val allowableValues: Array<String> = []
)

/**
 * JVM-алиас для аннотации Valid из пакета jakarta.validation.
 */
actual typealias Valid = jakarta.validation.Valid

/**
 * JVM-алиас, связывающий мультиплатформенный маркер [Payload] с интерфейсом
 * `jakarta.validation.Payload` из библиотеки Jakarta Bean Validation.
 *
 * ### Обоснование дизайна:
 * Позволяет классам-наследникам [Payload], объявленным в общем коде, на уровне байт-кода
 * JVM напрямую наследоваться от стандартного Java-интерфейса `jakarta.validation.Payload`.
 * Это делает общие модели совместимыми со всеми JVM/Spring-валидаторами без ручного написания
 * мостов и конвертеров.
 */
actual typealias Payload = jakarta.validation.Payload

/**
 * JVM-алиас для аннотации NotBlank из пакета jakarta.validation.constraints.
 */
actual typealias NotBlank = jakarta.validation.constraints.NotBlank

/**
 * JVM-алиас для аннотации Min из пакета jakarta.validation.constraints.
 */
actual typealias Min = jakarta.validation.constraints.Min

/**
 * JVM-алиас для аннотации Max из пакета jakarta.validation.constraints.
 */
actual typealias Max = jakarta.validation.constraints.Max

/**
 * JVM-алиас для аннотации Size из пакета jakarta.validation.constraints.
 */
actual typealias Size = jakarta.validation.constraints.Size

/**
 * JVM-алиас для аннотации DecimalMax из пакета jakarta.validation.constraints.
 */
actual typealias DecimalMax = jakarta.validation.constraints.DecimalMax

/**
 * JVM-алиас для аннотации DecimalMin из пакета jakarta.validation.constraints.
 */
actual typealias DecimalMin = jakarta.validation.constraints.DecimalMin

/**
 * JVM-алиас для аннотации NotEmpty из пакета jakarta.validation.constraints.
 */
actual typealias NotEmpty = jakarta.validation.constraints.NotEmpty

/**
 * JVM-алиас для аннотации NotNull из пакета jakarta.validation.constraints.
 */
actual typealias NotNull = jakarta.validation.constraints.NotNull

/**
 * JVM-алиас для аннотации Positive из пакета jakarta.validation.constraints.
 */
actual typealias Positive = jakarta.validation.constraints.Positive

/**
 * Вспомогательный класс для группировки объявлений в файле Annotations.kt.
 */
internal class AndroidAnnotations
