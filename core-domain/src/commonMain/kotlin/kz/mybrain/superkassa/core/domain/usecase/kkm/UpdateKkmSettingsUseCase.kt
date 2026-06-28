package kz.mybrain.superkassa.core.domain.usecase.kkm

import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.common.TaxRegime
import kz.mybrain.superkassa.core.domain.model.common.VatGroup
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.kkm.KkmMode
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptBranding
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.OfflineQueuePort
import kz.mybrain.superkassa.core.domain.port.StoragePort

/**
 * Сценарий обновления настроек контрольно-кассовой машины (ККМ).
 *
 * Позволяет изменять общие параметры ККМ (например, автозакрытие смены),
 * налоговые настройки и параметры брендирования чеков.
 *
 * @property storage Порт для доступа к хранилищу данных ККМ.
 * @property queue Порт для работы с автономной очередью документов.
 * @property clock Порт для работы с системным временем.
 */
class UpdateKkmSettingsUseCase(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val clock: ClockPort
) {
    /**
     * Обновляет общие настройки ККМ (например, флаг автозакрытия смены).
     *
     * @param kkm Текущая информация о ККМ.
     * @param autoCloseShift Флаг автоматического закрытия смены.
     * @return Обновленная информация о ККМ.
     * @throws ValidationException Если ККМ не находится в режиме программирования.
     */
    fun updateGeneralSettings(kkm: KkmInfo, autoCloseShift: Boolean): KkmInfo {
        requireProgramming(kkm, "KKM_SETTINGS_REQUIRES_PROGRAMMING")
        val updated = kkm.copy(updatedAt = clock.now(), autoCloseShift = autoCloseShift)
        storage.updateKkm(updated)
        return updated
    }

    /**
     * Обновляет налоговые настройки ККМ (режим налогообложения и группу НДС по умолчанию).
     *
     * Налоговые настройки могут быть изменены только если:
     * 1. ККМ находится в режиме программирования (режим и состояние).
     * 2. Смена на ККМ закрыта.
     * 3. Очередь документов пуста (все документы успешно отправлены).
     *
     * @param kkm Текущая информация о ККМ.
     * @param taxRegime Новый налоговый режим.
     * @param defaultVatGroup Новая группа НДС по умолчанию.
     * @return Обновленная информация о ККМ.
     * @throws ValidationException Если ККМ не находится в режиме программирования.
     * @throws ConflictException Если открыта смена или очередь документов не пуста.
     */
    fun updateTaxSettings(kkm: KkmInfo, taxRegime: TaxRegime, defaultVatGroup: VatGroup): KkmInfo {
        if (kkm.mode != KkmMode.PROGRAMMING.name || kkm.state != KkmState.PROGRAMMING.name) {
            throw ValidationException(
                ErrorMessages.kkmSettingsRequiresProgramming(),
                "KKM_TAX_SETTINGS_REQUIRES_PROGRAMMING"
            )
        }
        val openShift = storage.findOpenShift(kkm.id)
        if (openShift != null) {
            throw ConflictException(
                ErrorMessages.kkmDeleteShiftOpen(),
                "KKM_TAX_SETTINGS_SHIFT_OPEN"
            )
        }
        if (!queue.canSendDirectly(kkm.id)) {
            throw ConflictException(
                ErrorMessages.kkmDeleteQueueNotEmpty(),
                "KKM_TAX_SETTINGS_QUEUE_NOT_EMPTY"
            )
        }

        val updated = kkm.copy(
            updatedAt = clock.now(),
            taxRegime = taxRegime,
            defaultVatGroup = defaultVatGroup
        )
        storage.updateKkm(updated)
        return updated
    }

    /**
     * Обновляет параметры брендирования чеков для ККМ.
     *
     * @param kkm Текущая информация о ККМ.
     * @param branding Настройки брендирования (текст заголовка/подвала чека и т.д.).
     * @return Обновленная информация о ККМ.
     * @throws ValidationException Если ККМ не находится в режиме программирования.
     */
    fun updateBranding(kkm: KkmInfo, branding: ReceiptBranding): KkmInfo {
        requireProgramming(kkm, "KKM_BRANDING_SETTINGS_REQUIRES_PROGRAMMING")
        val updated = kkm.copy(updatedAt = clock.now(), branding = branding)
        storage.updateKkm(updated)
        return updated
    }

    /**
     * Вспомогательный метод для проверки нахождения ККМ в режиме программирования.
     *
     * @param kkm Информация о ККМ.
     * @param errorCode Код ошибки для исключения.
     * @throws ValidationException Если состояние ККМ не равно [KkmState.PROGRAMMING].
     */
    private fun requireProgramming(kkm: KkmInfo, errorCode: String) {
        if (kkm.state != KkmState.PROGRAMMING.name) {
            throw ValidationException(
                ErrorMessages.kkmSettingsRequiresProgramming(),
                errorCode
            )
        }
    }
}
