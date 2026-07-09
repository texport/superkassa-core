package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.port.StoragePort
import io.github.texport.superkassa.core.domain.port.PinHasherPort
import io.github.texport.superkassa.core.domain.port.DeliveryPort
import io.github.texport.superkassa.core.domain.port.DocumentConvertPort
import io.github.texport.superkassa.core.domain.port.ReceiptRenderPort
import io.github.texport.superkassa.core.domain.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.helper.ReceiptDeliveryHelper
import io.github.texport.superkassa.core.domain.usecase.receipt.RetryReceiptDeliveryUseCase
import io.github.texport.superkassa.core.presentation.api.DeliveryApi

/**
 * Внутренняя реализация API управления доставкой чеков.
 */
class DeliveryApiImpl(
    storage: StoragePort,
    pinHasher: PinHasherPort,
    delivery: DeliveryPort,
    coreSettings: CoreSettings,
    documentConvertPort: DocumentConvertPort,
    receiptRenderPort: ReceiptRenderPort
) : DeliveryApi {

    private val authorization = AuthorizeUserUseCase(storage, pinHasher)

    private val receiptDeliveryHelper = ReceiptDeliveryHelper(
        storage = storage,
        delivery = delivery,
        coreSettings = coreSettings,
        documentConvertPort = documentConvertPort,
        receiptRenderPort = receiptRenderPort
    )

    private val retryReceiptDeliveryUseCase = RetryReceiptDeliveryUseCase(
        storage = storage,
        authorizeUserUseCase = authorization,
        helper = receiptDeliveryHelper
    )

    override fun retryReceiptDelivery(
        kkmId: String,
        documentId: String,
        pin: String
    ): List<Pair<String, Boolean>> {
        return retryReceiptDeliveryUseCase.execute(kkmId, documentId, pin)
    }
}
