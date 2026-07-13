package io.github.texport.superkassa.core.domain.api.model.kkm

/**
 * Информация о заводских параметрах и номерах ККМ.
 *
 * @property factoryNumber Заводской (серийный) номер устройства ККМ.
 * @property manufactureYear Год выпуска устройства ККМ.
 */
data class FactoryInfo(
    val factoryNumber: String,
    val manufactureYear: Int
)
