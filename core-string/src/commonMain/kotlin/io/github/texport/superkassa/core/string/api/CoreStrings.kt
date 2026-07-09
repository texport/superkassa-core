package io.github.texport.superkassa.core.string.api

import io.github.texport.superkassa.core.string.impl.CoreStringsImpl

/**
 * Единый реестр мультиязычных шаблонов и сообщений об ошибках во всех слоях и модулях Superkassa.
 *
 * Предоставляет централизованные фабричные методы для создания локализованных
 * сообщений [TrilingualMessage] и строк.
 */
object CoreStrings {

    // --- Доменные ошибки (Domain Errors) ---
    fun badRequest(): TrilingualMessage = CoreStringsImpl.badRequest()
    fun kkmNotFound(): TrilingualMessage = CoreStringsImpl.kkmNotFound()
    fun kkmExists(): TrilingualMessage = CoreStringsImpl.kkmExists()
    fun shiftNotOpen(): TrilingualMessage = CoreStringsImpl.shiftNotOpen()
    fun shiftAlreadyOpen(): TrilingualMessage = CoreStringsImpl.shiftAlreadyOpen()
    fun shiftTooLong(): TrilingualMessage = CoreStringsImpl.shiftTooLong()
    fun systemTimeInvalid(): TrilingualMessage = CoreStringsImpl.systemTimeInvalid()
    fun ofdProviderUnknown(id: String): TrilingualMessage = CoreStringsImpl.ofdProviderUnknown(id)
    fun ofdEnvironmentUnknown(env: String): TrilingualMessage = CoreStringsImpl.ofdEnvironmentUnknown(env)
    fun ofdProviderRequired(): TrilingualMessage = CoreStringsImpl.ofdProviderRequired()
    fun ofdProviderTagInvalid(tag: String): TrilingualMessage = CoreStringsImpl.ofdProviderTagInvalid(tag)
    fun ofdTokenRequired(): TrilingualMessage = CoreStringsImpl.ofdTokenRequired()
    fun ofdTokenInvalid(token: String): TrilingualMessage = CoreStringsImpl.ofdTokenInvalid(token)
    fun kkmRegistrationRequired(): TrilingualMessage = CoreStringsImpl.kkmRegistrationRequired()
    fun kkmFactoryRequired(): TrilingualMessage = CoreStringsImpl.kkmFactoryRequired()
    fun kkmSystemIdRequired(): TrilingualMessage = CoreStringsImpl.kkmSystemIdRequired()
    fun kkmSystemIdInvalid(systemId: String): TrilingualMessage = CoreStringsImpl.kkmSystemIdInvalid(systemId)
    fun kkmSystemIdExists(systemId: String): TrilingualMessage = CoreStringsImpl.kkmSystemIdExists(systemId)
    fun userPinRequired(): TrilingualMessage = CoreStringsImpl.userPinRequired()
    fun userNotFound(): TrilingualMessage = CoreStringsImpl.userNotFound()
    fun userForbidden(): TrilingualMessage = CoreStringsImpl.userForbidden()
    fun userNameRequired(): TrilingualMessage = CoreStringsImpl.userNameRequired()
    fun userRoleRequired(roleName: String): TrilingualMessage = CoreStringsImpl.userRoleRequired(roleName)
    fun userPinConflict(): TrilingualMessage = CoreStringsImpl.userPinConflict()
    fun defaultPinNotAllowed(): TrilingualMessage = CoreStringsImpl.defaultPinNotAllowed()
    fun userUpdateEmpty(): TrilingualMessage = CoreStringsImpl.userUpdateEmpty()
    fun kkmDeleteRequiresProgramming(): TrilingualMessage = CoreStringsImpl.kkmDeleteRequiresProgramming()
    fun kkmDeleteShiftOpen(): TrilingualMessage = CoreStringsImpl.kkmDeleteShiftOpen()
    fun kkmDeleteQueueNotEmpty(): TrilingualMessage = CoreStringsImpl.kkmDeleteQueueNotEmpty()
    fun kkmSyncShiftOpen(): TrilingualMessage = CoreStringsImpl.kkmSyncShiftOpen()
    fun kkmSyncQueueNotEmpty(): TrilingualMessage = CoreStringsImpl.kkmSyncQueueNotEmpty()
    fun kkmAutonomousTooLong(): TrilingualMessage = CoreStringsImpl.kkmAutonomousTooLong()
    fun kkmBlocked(): TrilingualMessage = CoreStringsImpl.kkmBlocked()
    fun kkmSettingsRequiresProgramming(): TrilingualMessage = CoreStringsImpl.kkmSettingsRequiresProgramming()
    fun kkmInProgramming(): TrilingualMessage = CoreStringsImpl.kkmInProgramming()
    fun unauthorized(): TrilingualMessage = CoreStringsImpl.unauthorized()
    fun ofdRequestFailed(details: String?): TrilingualMessage = CoreStringsImpl.ofdRequestFailed(details)
    fun measureUnitCodeInvalid(code: String): TrilingualMessage = CoreStringsImpl.measureUnitCodeInvalid(code)
    fun measureUnitNotFound(code: String): TrilingualMessage = CoreStringsImpl.measureUnitNotFound(code)
    fun documentNotFound(): TrilingualMessage = CoreStringsImpl.documentNotFound()
    fun okvedRequired(): TrilingualMessage = CoreStringsImpl.okvedRequired()
    fun nomenclatureNotFound(barcode: String): TrilingualMessage = CoreStringsImpl.nomenclatureNotFound(barcode)
    fun parentTicketRequiredForReturns(): TrilingualMessage = CoreStringsImpl.parentTicketRequiredForReturns()
    fun receiptDiscountScopesConflict(): TrilingualMessage = CoreStringsImpl.receiptDiscountScopesConflict()

    // --- Технические ошибки инфраструктуры (Data/Infrastructure Errors) ---
    fun ofdRequestFailedData(details: String?): String = CoreStringsImpl.ofdRequestFailedData(details)

    // --- Сообщения очереди (Queue Messages) ---
    fun handlerException(reason: String): TrilingualMessage = CoreStringsImpl.handlerException(reason)
    fun invalidDispatchStatus(status: String): TrilingualMessage = CoreStringsImpl.invalidDispatchStatus(status)
    fun ofdDeliveryFailure(errorMsg: String): TrilingualMessage = CoreStringsImpl.ofdDeliveryFailure(errorMsg)
    fun ofdTimeout(): TrilingualMessage = CoreStringsImpl.ofdTimeout()

    // --- Сообщения доставки (Delivery Messages) ---
    fun noAdapterForChannel(channel: String): TrilingualMessage = CoreStringsImpl.noAdapterForChannel(channel)

    // --- Сообщения визуализации и рендеринга (Rendering Messages) ---
    fun ofdErrorReason(): TrilingualMessage = CoreStringsImpl.ofdErrorReason()
    fun statusError(): TrilingualMessage = CoreStringsImpl.statusError()
}
