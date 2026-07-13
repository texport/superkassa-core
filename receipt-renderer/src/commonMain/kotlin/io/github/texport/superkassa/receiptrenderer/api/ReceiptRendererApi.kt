package io.github.texport.superkassa.receiptrenderer.api

import io.github.texport.superkassa.core.domain.api.port.integration.QrCodeGeneratorPort
import io.github.texport.superkassa.core.domain.api.port.internal.ReceiptRenderPort

/**
 * Публичный интерфейс API для рендеринга чеков и отчетов Superkassa.
 */
interface ReceiptRendererApi : ReceiptRenderPort

/**
 * Фабричная функция для создания экземпляра API рендерера чеков.
 *
 * @param qrCodeGenerator Генератор QR-кодов.
 * @return Экземпляр API рендерера.
 */
fun createReceiptRendererApi(qrCodeGenerator: QrCodeGeneratorPort): ReceiptRendererApi =
    io.github.texport.superkassa.receiptrenderer.impl.ReceiptRendererApiImpl(qrCodeGenerator)
