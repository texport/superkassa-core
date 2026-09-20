package io.github.texport.superkassa.core.domain.impl.helper.common

import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort

/**
 * Присваивает документу сквозной номер печатного документа кассы.
 *
 * Номер ведёт сама касса: он не зависит от ответа ОФД и не прерывается
 * при работе в разрыве связи, поэтому по нему восстанавливают нумерацию
 * после сбоя.
 */
internal fun assignPrintedDocumentNumber(storage: StoragePort, kkmId: String, documentId: String): Long {
    val next = 1 + (
        storage.loadCounters(kkmId, CounterScopes.GLOBAL, null)[
            CounterKeyFormats.PRINTED_DOCUMENT_NUMBER
        ] ?: 0L
        )
    storage.upsertCounter(kkmId, CounterScopes.GLOBAL, null, CounterKeyFormats.PRINTED_DOCUMENT_NUMBER, next)
    storage.updatePrintedDocumentNumber(documentId, next)
    return next
}
