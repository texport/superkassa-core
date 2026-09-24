package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryRetryPolicy
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort
import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.internal.PinHasherPort
import io.github.texport.superkassa.core.domain.api.port.internal.ReceiptRenderPort
import io.github.texport.superkassa.core.domain.impl.helper.ReceiptDeliveryHelper
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.auth.PinGuard
import io.github.texport.superkassa.core.domain.impl.usecase.delivery.DeliveryRequests
import io.github.texport.superkassa.core.domain.impl.usecase.delivery.GetReceiptDeliveriesUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.delivery.ReceiptDeliveryPlan
import io.github.texport.superkassa.core.domain.impl.usecase.delivery.SendDeliveryTasksUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.receipt.RetryReceiptDeliveryUseCase
import io.github.texport.superkassa.core.presentation.api.DeliveryApi
import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryResponse
import io.github.texport.superkassa.core.presentation.impl.mapper.toResponse

/**
 * Внутренняя реализация API доставки чека покупателю.
 *
 * @param clock часы кассы: по ним наступает срок задач.
 * @param policy повторы доставки после отказа канала.
 */
class DeliveryApiImpl(
    storage: StoragePort,
    pinHasher: PinHasherPort,
    delivery: DeliveryPort,
    coreSettings: CoreSettings,
    documentConvertPort: DocumentConvertPort,
    receiptRenderPort: ReceiptRenderPort,
    /** Счёт неверных пинов; сборка ядра даёт тот же, что у фасада. */
    pinGuard: PinGuard,
    clock: ClockPort,
    policy: DeliveryRetryPolicy = DeliveryRetryPolicy()
) : DeliveryApi {

    private val authorization = AuthorizeUserUseCase(storage, pinHasher, pinGuard)

    private val sender = SendDeliveryTasksUseCase(
        storage = storage,
        delivery = delivery,
        requests = DeliveryRequests(storage, coreSettings.delivery?.print, documentConvertPort, receiptRenderPort),
        clock = clock,
        policy = policy
    )

    private val retry = RetryReceiptDeliveryUseCase(
        storage = storage,
        authorizeUserUseCase = authorization,
        helper = ReceiptDeliveryHelper(storage, delivery, coreSettings, documentConvertPort, receiptRenderPort),
        plan = ReceiptDeliveryPlan(coreSettings.delivery),
        sender = sender,
        clock = clock
    )

    private val deliveries = GetReceiptDeliveriesUseCase(storage, authorization)

    override fun retryReceiptDelivery(kkmId: String, documentId: String, pin: String): List<Pair<String, Boolean>> =
        retry.execute(kkmId, documentId, pin)

    override fun resendReceipt(kkmId: String, documentId: String, pin: String): List<ReceiptDeliveryResponse> =
        retry.resend(kkmId, documentId, pin).map { it.toResponse() }

    override fun receiptDeliveries(kkmId: String, documentId: String, pin: String): List<ReceiptDeliveryResponse> =
        deliveries.execute(kkmId, documentId, pin).map { it.toResponse() }

    override fun sendDueDeliveries(limit: Int): Int = sender.sendDue(limit)
}
