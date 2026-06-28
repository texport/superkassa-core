package kz.mybrain.superkassa.core.domain.model.kkm

/**
 * Информация о заводских параметрах и номерах ККМ.
 *
 * @property factoryNumber Заводской (серийный) номер устройства ККМ.
 * @property manufactureYear Год выпуска устройства ККМ.
 */
@Suppress("unused") // Доменная модель считывается и используется внешними слоями и при сериализации API
data class FactoryInfo(
    val factoryNumber: String,
    val manufactureYear: Int
)
