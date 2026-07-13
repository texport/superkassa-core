package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.internal.PinHasherPort
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort
import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import io.github.texport.superkassa.core.domain.api.port.internal.ReceiptRenderPort
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.helper.ReceiptDeliveryHelper
import io.github.texport.superkassa.core.domain.impl.usecase.receipt.RetryReceiptDeliveryUseCase
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
