package kz.mybrain.superkassa.core.application.service

import kz.mybrain.superkassa.core.application.error.ConflictException
import kz.mybrain.superkassa.core.application.error.ErrorMessages
import kz.mybrain.superkassa.core.application.error.NotFoundException
import kz.mybrain.superkassa.core.application.error.ValidationException
import kz.mybrain.superkassa.core.domain.model.KkmInfo
import kz.mybrain.superkassa.core.domain.model.KkmMode
import kz.mybrain.superkassa.core.domain.model.KkmState
import kz.mybrain.superkassa.core.domain.model.TaxRegime
import kz.mybrain.superkassa.core.domain.model.UserRole
import kz.mybrain.superkassa.core.domain.model.VatGroup
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.OfflineQueuePort
import kz.mybrain.superkassa.core.domain.port.StoragePort

/**
 * Сервис для управления жизненным циклом и настройками ККМ.
 */
class KkmLifecycleService(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val clock: ClockPort,
    private val authorization: AuthorizationService,
    private val kkmCommonHelper: KkmCommonHelper
) {
    private val draftMode = KkmMode.REGISTRATION.name
    private val draftState = KkmState.IDLE.name
    private val registeredMode = KkmMode.REGISTRATION.name
    private val registeredState = KkmState.ACTIVE.name

    fun deleteKkm(id: String, pin: String): Boolean {
        val kkm = authorization.requireKkm(id)
        authorization.requireRole(kkm.id, pin, setOf(UserRole.ADMIN))
        if (kkm.state != KkmState.PROGRAMMING.name) {
            throw ValidationException(
                ErrorMessages.kkmDeleteRequiresProgramming(),
                "KKM_DELETE_REQUIRES_PROGRAMMING"
            )
        }
        val openShift = storage.findOpenShift(id)
        if (openShift != null) {
            throw ConflictException(ErrorMessages.kkmDeleteShiftOpen(), "KKM_DELETE_SHIFT_OPEN")
        }
        if (!queue.canSendDirectly(id)) {
            throw ConflictException(
                ErrorMessages.kkmDeleteQueueNotEmpty(),
                "KKM_DELETE_QUEUE_NOT_EMPTY"
            )
        }
        val deleted = storage.deleteKkmCompletely(id)
        if (!deleted) {
            throw NotFoundException(message = ErrorMessages.kkmNotFound(), code = "KKM_NOT_FOUND")
        }
        queue.deleteQueuedCommands(id)
        return true
    }

    fun updateKkmSettings(kkmId: String, pin: String, autoCloseShift: Boolean): KkmInfo {
        kkmCommonHelper.ensureSystemTimeValid()
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN))
        val kkm = authorization.requireKkm(kkmId)
        if (kkm.state != KkmState.PROGRAMMING.name) {
            throw ValidationException(
                ErrorMessages.kkmSettingsRequiresProgramming(),
                "KKM_SETTINGS_REQUIRES_PROGRAMMING"
            )
        }
        val updated = kkm.copy(updatedAt = clock.now(), autoCloseShift = autoCloseShift)
        storage.updateKkm(updated)
        return updated
    }

    fun updateTaxSettings(
        kkmId: String,
        pin: String,
        taxRegime: TaxRegime,
        defaultVatGroup: VatGroup
    ): KkmInfo {
        kkmCommonHelper.ensureSystemTimeValid()
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN))
        val kkm = authorization.requireKkm(kkmId)
        if (kkm.mode != KkmMode.PROGRAMMING.name || kkm.state != KkmState.PROGRAMMING.name) {
            throw ValidationException(
                ErrorMessages.kkmSettingsRequiresProgramming(),
                "KKM_TAX_SETTINGS_REQUIRES_PROGRAMMING"
            )
        }
        val openShift = storage.findOpenShift(kkmId)
        if (openShift != null) {
            throw ConflictException(
                ErrorMessages.kkmDeleteShiftOpen(),
                "KKM_TAX_SETTINGS_SHIFT_OPEN"
            )
        }
        if (!queue.canSendDirectly(kkmId)) {
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

    fun enterProgramming(kkmId: String, pin: String): KkmInfo {
        return storage.inTransaction {
            val kkm = authorization.requireKkm(kkmId)
            authorization.requireRole(kkm.id, pin, setOf(UserRole.ADMIN))
            val updated = kkm.copy(
                updatedAt = clock.now(),
                mode = KkmMode.PROGRAMMING.name,
                state = KkmState.PROGRAMMING.name
            )
            storage.updateKkm(updated)
            updated
        }
    }

    fun exitProgramming(kkmId: String, pin: String): KkmInfo {
        return storage.inTransaction {
            val kkm = authorization.requireKkm(kkmId)
            authorization.requireRole(kkm.id, pin, setOf(UserRole.ADMIN))
            val isDraft = kkm.registrationNumber.isNullOrBlank()
            val restoredMode = if (isDraft) draftMode else registeredMode
            val restoredState = if (isDraft) draftState else registeredState
            val updated = kkm.copy(
                updatedAt = clock.now(),
                mode = restoredMode,
                state = restoredState
            )
            storage.updateKkm(updated)
            updated
        }
    }
}
