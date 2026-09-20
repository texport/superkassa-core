package io.github.texport.superkassa.core.domain.impl.usecase.kkm

import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmMode
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptBranding
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort

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
    fun updateGeneralSettings(kkm: KkmInfo, autoCloseShift: Boolean, autoCashout: Boolean): KkmInfo {
        requireProgramming(kkm, "KKM_SETTINGS_REQUIRES_PROGRAMMING")
        val updated = kkm.copy(
            updatedAt = clock.now(),
            autoCloseShift = autoCloseShift,
            autoCashout = autoCashout
        )
        storage.updateKkm(updated)
        return updated
    }

    /**
     * Задаёт название кассы.
     *
     * Название — не фискальный реквизит: оно не попадает в чек и не уходит
     * в ОФД, поэтому режим программирования для него не требуется. Иначе
     * владелец не мог бы назвать кассу, не остановив ею работу.
     *
     * Пустая строка означает «названия нет»: касса снова показывается
     * регистрационным номером.
     *
     * @param kkm Текущая информация о ККМ.
     * @param name Новое название или `null`, чтобы его снять.
     * @return Обновленная информация о ККМ.
     */
    fun updateName(kkm: KkmInfo, name: String?): KkmInfo {
        val updated = kkm.copy(
            updatedAt = clock.now(),
            name = name?.trim()?.takeIf { it.isNotEmpty() }
        )
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
                CoreStrings.kkmSettingsRequiresProgramming(),
                "KKM_TAX_SETTINGS_REQUIRES_PROGRAMMING"
            )
        }
        val openShift = storage.findOpenShift(kkm.id)
        if (openShift != null) {
            throw ConflictException(
                CoreStrings.kkmSettingsShiftOpen(),
                "KKM_TAX_SETTINGS_SHIFT_OPEN"
            )
        }
        if (!queue.canSendDirectly(kkm.id)) {
            throw ConflictException(
                CoreStrings.kkmSettingsQueueNotEmpty(),
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
        // Тексты оператора принадлежат ОФД: их присылает служебный ответ вместе
        // с версией, по которой сервер решает, что обновилось. Сохранение
        // настроек кассой их не трогает — иначе чек остаётся без обязательной
        // рекламы до следующего обмена, а версию можно было бы подделать.
        val updated = kkm.copy(
            updatedAt = clock.now(),
            branding = branding.copy(ofdTicketAds = kkm.branding.ofdTicketAds)
        )
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
                CoreStrings.kkmSettingsRequiresProgramming(),
                errorCode
            )
        }
    }
}
