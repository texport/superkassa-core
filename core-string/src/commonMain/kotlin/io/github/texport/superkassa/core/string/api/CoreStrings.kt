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
    fun queueRetryShiftOpen(): TrilingualMessage = CoreStringsImpl.queueRetryShiftOpen()
    fun kkmSettingsShiftOpen(): TrilingualMessage = CoreStringsImpl.kkmSettingsShiftOpen()
    fun kkmSettingsQueueNotEmpty(): TrilingualMessage = CoreStringsImpl.kkmSettingsQueueNotEmpty()
    fun cashSumNegative(): TrilingualMessage = CoreStringsImpl.cashSumNegative()
    fun insufficientCash(): TrilingualMessage = CoreStringsImpl.insufficientCash()
    fun printDocumentIdRequired(): TrilingualMessage = CoreStringsImpl.printDocumentIdRequired()
    fun printShiftIdRequired(): TrilingualMessage = CoreStringsImpl.printShiftIdRequired()
    fun cashierCannotChangeRole(): TrilingualMessage = CoreStringsImpl.cashierCannotChangeRole()
    fun shiftNotFound(): TrilingualMessage = CoreStringsImpl.shiftNotFound()
    fun paymentTypeNotSupported(payment: String, protocolVersion: String): TrilingualMessage =
        CoreStringsImpl.paymentTypeNotSupported(payment, protocolVersion)
    fun kkmDeleteAutonomousNotAllowed(): TrilingualMessage = CoreStringsImpl.kkmDeleteAutonomousNotAllowed()
    fun kkmSyncShiftOpen(): TrilingualMessage = CoreStringsImpl.kkmSyncShiftOpen()
    fun kkmSyncQueueNotEmpty(): TrilingualMessage = CoreStringsImpl.kkmSyncQueueNotEmpty()
    fun kkmSyncShiftDiverged(shiftNo: Long): TrilingualMessage =
        CoreStringsImpl.kkmSyncShiftDiverged(shiftNo)
    fun kkmAutonomousTooLong(): TrilingualMessage = CoreStringsImpl.kkmAutonomousTooLong()
    fun kkmBlocked(): TrilingualMessage = CoreStringsImpl.kkmBlocked()

    /** Блокировка с причиной и с тем, что делать; код — из состояния кассы. */
    fun kkmBlocked(code: Int?): TrilingualMessage = CoreStringsImpl.kkmBlockedWithReason(code)

    /** Смена перевалила сутки: оформлять операции нельзя до её закрытия. */
    fun shiftLongerThanDay(): TrilingualMessage = CoreStringsImpl.shiftLongerThanDay()
    fun kkmSettingsRequiresProgramming(): TrilingualMessage = CoreStringsImpl.kkmSettingsRequiresProgramming()
    fun kkmInProgramming(): TrilingualMessage = CoreStringsImpl.kkmInProgramming()
    fun unauthorized(): TrilingualMessage = CoreStringsImpl.unauthorized()
    fun ofdRequestFailed(details: String?): TrilingualMessage = CoreStringsImpl.ofdRequestFailed(details)

    // --- OFD Code Handling (CPCR 2.0.3) ---
    fun blockReason(code: Int): TrilingualMessage = CoreStringsImpl.blockReason(code)
    fun documentFailedReason(ofdErrorCode: Int): TrilingualMessage = CoreStringsImpl.documentFailedReason(ofdErrorCode)
    fun measureUnitCodeInvalid(code: String): TrilingualMessage = CoreStringsImpl.measureUnitCodeInvalid(code)
    fun measureUnitNotFound(code: String): TrilingualMessage = CoreStringsImpl.measureUnitNotFound(code)
    fun documentNotFound(): TrilingualMessage = CoreStringsImpl.documentNotFound()
    fun okvedRequired(): TrilingualMessage = CoreStringsImpl.okvedRequired()
    fun nomenclatureNotFound(barcode: String): TrilingualMessage = CoreStringsImpl.nomenclatureNotFound(barcode)
    fun parentTicketRequiredForReturns(): TrilingualMessage = CoreStringsImpl.parentTicketRequiredForReturns()
    fun receiptDiscountScopesConflict(): TrilingualMessage = CoreStringsImpl.receiptDiscountScopesConflict()

    /** Скидка и наценка на сам чек вместе: сервис приёма принимает одно из двух. */
    fun receiptDiscountAndMarkupConflict(): TrilingualMessage =
        CoreStringsImpl.receiptDiscountAndMarkupConflict()

    /** Возврат больше остатка по чеку-основанию. */
    fun refundExceedsBasis(left: String): TrilingualMessage = CoreStringsImpl.refundExceedsBasis(left)

    /** По чеку-основанию уже возвращено всё. */
    fun refundAlreadyFull(): TrilingualMessage = CoreStringsImpl.refundAlreadyFull()
    fun receiptVatNotAllowed(group: String): TrilingualMessage =
        CoreStringsImpl.receiptVatNotAllowed(group)
    fun paymentsTotalMismatch(paid: String, total: String): TrilingualMessage =
        CoreStringsImpl.paymentsTotalMismatch(paid, total)
    fun documentNotFiscalized(): TrilingualMessage = CoreStringsImpl.documentNotFiscalized()
    fun deliveryChannelsNotConfigured(): TrilingualMessage = CoreStringsImpl.deliveryChannelsNotConfigured()

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

    // --- Переводы справочников (Enum Reference Translations) ---
    fun paymentType(code: String): TrilingualMessage = CoreStringsImpl.paymentType(code)
    fun documentType(code: String): TrilingualMessage = CoreStringsImpl.documentType(code)
    fun userRole(code: String): TrilingualMessage = CoreStringsImpl.userRole(code)
    fun taxRegime(code: String): TrilingualMessage = CoreStringsImpl.taxRegime(code)
    fun vatGroup(code: String): TrilingualMessage = CoreStringsImpl.vatGroup(code)
    fun receiptDomainType(code: String): TrilingualMessage = CoreStringsImpl.receiptDomainType(code)
    fun unitOfMeasurement(code: String): TrilingualMessage = CoreStringsImpl.unitOfMeasurement(code)
    fun paperWidth(code: String): TrilingualMessage = CoreStringsImpl.paperWidth(code)
    fun brandingColor(code: String): TrilingualMessage = CoreStringsImpl.brandingColor(code)
    fun kkmState(code: String): TrilingualMessage = CoreStringsImpl.kkmState(code)
    fun kkmLabelMode(): TrilingualMessage = CoreStringsImpl.kkmLabelMode()
    fun kkmLabelConnection(): TrilingualMessage = CoreStringsImpl.kkmLabelConnection()
    fun kkmStatusOnline(): TrilingualMessage = CoreStringsImpl.kkmStatusOnline()
    fun kkmStatusOfflineSince(date: String): TrilingualMessage = CoreStringsImpl.kkmStatusOfflineSince(date)
    fun kkmMode(code: String): TrilingualMessage = CoreStringsImpl.kkmMode(code)
    fun shiftStatus(code: String): TrilingualMessage = CoreStringsImpl.shiftStatus(code)
    fun deliveryStatus(code: String): TrilingualMessage = CoreStringsImpl.deliveryStatus(code)
    fun ofdCommandStatus(code: String): TrilingualMessage = CoreStringsImpl.ofdCommandStatus(code)
    fun ofdDocumentStatus(code: String): TrilingualMessage = CoreStringsImpl.ofdDocumentStatus(code)
    fun receiptOperationType(code: String): TrilingualMessage = CoreStringsImpl.receiptOperationType(code)
    fun ofdEnvironment(code: String): TrilingualMessage = CoreStringsImpl.ofdEnvironment(code)
    fun ofdProvider(code: String): TrilingualMessage = CoreStringsImpl.ofdProvider(code)
    fun coreMode(code: String): TrilingualMessage = CoreStringsImpl.coreMode(code)
    fun authMode(code: String): TrilingualMessage = CoreStringsImpl.authMode(code)
    fun receiptLanguage(code: String): TrilingualMessage = CoreStringsImpl.receiptLanguage(code)
    fun receiptLayoutType(code: String): TrilingualMessage = CoreStringsImpl.receiptLayoutType(code)
    fun printDocumentType(code: String): TrilingualMessage = CoreStringsImpl.printDocumentType(code)
    fun ofdCommandType(code: String): TrilingualMessage = CoreStringsImpl.ofdCommandType(code)
    fun cashOperationType(code: String): TrilingualMessage = CoreStringsImpl.cashOperationType(code)
}
