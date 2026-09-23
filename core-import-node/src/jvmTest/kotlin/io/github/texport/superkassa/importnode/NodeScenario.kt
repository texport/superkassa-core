package io.github.texport.superkassa.importnode

import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptPayment
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptStoredPayload
import kotlinx.serialization.json.Json

/**
 * Касса узла в разгар работы: закрытая смена и открытая, в открытой —
 * внесение, продажа, возврат, изъятие в очереди, отклонённая ОФД продажа
 * и X-отчёт; два кассира, ключи повтора, в том числе незавершённый.
 */
internal object NodeScenario {

    const val KKM = "kkm-1"
    const val OPEN_SHIFT = "shift-2"
    const val KGD_NUMBER = "600300012345"
    const val BIN = "123456789012"
    const val ADMIN_PIN = "1234"
    const val CASHIER_PIN = "5678"
    val TOKEN = "MTIzNDU2Nzg5".toByteArray()

    /** Своё оформление чека: узел пишет только поля, отличные от умолчания. */
    const val BRANDING_JSON = """{"language":"KK","paperWidthMm":58,"themeColor":"emerald","headerMsg":"Дала дүкені",""" +
        """"footerMsg":"Рахмет!","ofdTicketAds":[{"type":"TICKET_AD_OFD","version":3,"text":"ОФД хабарламасы"}],""" +
        """"printOfdTicketAds":false}"""

    /** Итог продажи: 1 500 тенге 50 тиын. */
    const val SALE_TIYN = 150_050L

    /** Чек узел пишет Jackson-ом: все поля, пустые — `null`. */
    private val jackson = Json {
        encodeDefaults = true
        explicitNulls = true
    }

    fun populate(node: NodeFixture) {
        cashbox(node)
        shifts(node)
        documents(node)
        counters(node)
        queue(node)
        people(node)
    }

    fun receipt(operation: ReceiptOperationType, tiyn: Long, key: String) = ReceiptStoredPayload(
        kkmId = KKM,
        operation = operation,
        items = listOf(ReceiptItem("Нан", "001", 1000, Money.fromTiyn(tiyn), Money.fromTiyn(tiyn), measureUnitCode = "796")),
        payments = listOf(ReceiptPayment(PaymentType.CASH, Money.fromTiyn(tiyn))),
        total = Money.fromTiyn(tiyn),
        taken = Money.fromTiyn(tiyn),
        idempotencyKey = key,
        operatorName = "Айгерім"
    )

    private fun payload(operation: ReceiptOperationType, tiyn: Long, key: String): ByteArray =
        jackson.encodeToString(ReceiptStoredPayload.serializer(), receipt(operation, tiyn, key)).toByteArray()

    private fun cashbox(node: NodeFixture) = node.exec(
        "INSERT INTO cashbox (id, created_at, updated_at, mode, state, ofd_provider, registration_number, factory_number, " +
            "manufacture_year, system_id, token_enc, token_updated_at, last_shift_no, last_receipt_no, autonomous_since, " +
            "ofd_service_info, auto_close_shift, tax_regime, default_vat_group, branding_json, name, auto_cashout) " +
            "VALUES (?, 1, 2, 'REGISTRATION', 'ACTIVE', 'BFD:DEV', ?, 'KZT0001', 2026, '4100', ?, 3, 2, 4, NULL, ?, 0, " +
            "'NO_VAT', 'NO_VAT', ?, 'Касса у входа', 1)",
        KKM, KGD_NUMBER, TOKEN,
        """{"orgTitle":"ТОО Дала","orgAddress":"Алматы","orgAddressKz":"Алматы","orgInn":"$BIN",""" +
            """"orgOkved":"47111","geoLatitude":43250000,"geoLongitude":76900000,"geoSource":"MANUAL"}""",
        BRANDING_JSON
    )

    private fun shifts(node: NodeFixture) {
        node.exec("INSERT INTO shift VALUES ('shift-1', ?, 1, 'CLOSED', 10, 20, 'd-open-1', 'd-close-1')", KKM)
        node.exec("INSERT INTO shift VALUES (?, ?, 2, 'OPEN', 30, NULL, 'd-open-2', NULL)", OPEN_SHIFT, KKM)
    }

    private fun documents(node: NodeFixture) {
        document(node, "d-open-1", "shift-1", "SHIFT_OPEN", Doc(time = 10, status = "INTERNAL"))
        document(node, "d-sale-1", "shift-1", "SALE", Doc(1001, 11, 1_000L, payload(ReceiptOperationType.SELL, 1_000L, "k-1"), "SENT", 1))
        document(node, "d-close-1", "shift-1", "SHIFT_CLOSE", Doc(time = 20, status = "SENT", printed = 2))
        document(node, "d-open-2", OPEN_SHIFT, "SHIFT_OPEN", Doc(time = 30, status = "INTERNAL"))
        document(node, "d-in", OPEN_SHIFT, "CASH_IN", Doc(time = 31, tiyn = 1_000_000L, status = "SENT", printed = 3))
        document(node, "d-sale", OPEN_SHIFT, "SALE", Doc(2001, 32, SALE_TIYN, payload(ReceiptOperationType.SELL, SALE_TIYN, "k-sale"), "SENT", 4))
        document(node, "d-return", OPEN_SHIFT, "RETURN", Doc(2002, 33, 50_000L, payload(ReceiptOperationType.SELL_RETURN, 50_000L, "k-ret"), "SENT", 5))
        document(node, "d-out", OPEN_SHIFT, "CASH_OUT", Doc(time = 34, tiyn = 200_000L, status = "PENDING", printed = 6))
        document(node, "d-rejected", OPEN_SHIFT, "SALE", Doc(time = 35, tiyn = 700L, payload = payload(ReceiptOperationType.SELL, 700L, "k-rej"), status = "FAILED", printed = 7))
        node.exec("UPDATE fiscal_document SET ofd_error_code = 15, ofd_error_text = 'Отказ ОФД' WHERE id = 'd-rejected'")
        document(node, "d-x", OPEN_SHIFT, "X_REPORT", Doc(time = 36, status = "PENDING", printed = 8))
    }

    private class Doc(
        val number: Long? = null,
        val time: Long,
        val tiyn: Long = 0,
        val payload: ByteArray? = null,
        val status: String,
        val printed: Long? = null
    )

    private fun document(node: NodeFixture, id: String, shift: String, type: String, doc: Doc) = node.exec(
        "INSERT INTO fiscal_document (id, cashbox_id, shift_id, doc_type, doc_no, shift_no, created_at, total_amount, " +
            "currency, payload_bin, fiscal_sign, is_autonomous, ofd_status, delivered_at, receipt_url, printed_document_number) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'KZT', ?, ?, 0, ?, ?, ?, ?)",
        id, KKM, shift, type, doc.number, if (shift == OPEN_SHIFT) 2L else 1L, doc.time, doc.tiyn, doc.payload,
        doc.number?.let { "FP$it" }, doc.status, doc.number?.let { doc.time + 1 }, doc.number?.let { "https://ofd.kz/t/$it" }, doc.printed
    )

    private fun counters(node: NodeFixture) {
        val global = mapOf("ofd.req_num" to 17L, "cash.sum" to 1_900_050L, "printed_document.number" to 8L)
        global.forEach { (key, value) -> counter(node, "GLOBAL", null, key, value) }
        val shift = mapOf(
            "start_shift_cash.sum" to 1_000L, "cash.sum" to 1_900_050L,
            "operation.OPERATION_SELL.count" to 1L, "operation.OPERATION_SELL.sum" to SALE_TIYN,
            "operation.OPERATION_SELL_RETURN.count" to 1L, "operation.OPERATION_SELL_RETURN.sum" to 50_000L,
            "money_placement.MONEY_PLACEMENT_DEPOSIT.sum" to 1_000_000L, "revenue.sum" to 100_050L
        )
        shift.forEach { (key, value) -> counter(node, "SHIFT", OPEN_SHIFT, key, value) }
    }

    private fun counter(node: NodeFixture, scope: String, shift: String?, key: String, value: Long) =
        node.exec("INSERT INTO counter VALUES (?, ?, ?, ?, ?, 40)", KKM, scope, shift, key, value)

    private fun queue(node: NodeFixture) {
        task(node, "q-sale", "TICKET", "d-sale", "SENT")
        task(node, "q-out", "MONEY_PLACEMENT", "d-out", "PENDING")
        task(node, "q-x", "REPORT_X", "d-x", "PENDING")
        task(node, "q-rejected", "TICKET", "d-rejected", "REJECTED")
    }

    private fun task(node: NodeFixture, id: String, type: String, ref: String, status: String) = node.exec(
        "INSERT INTO queue_task VALUES (?, ?, 'OFFLINE', ?, ?, 50, ?, 2, NULL, NULL)", id, KKM, type, ref, status
    )

    private fun people(node: NodeFixture) {
        node.exec("INSERT INTO kkm_user VALUES ('u-admin', ?, 'Сауле', 'ADMIN', ?, 1)", KKM, NodeFixture.nodePinHash(ADMIN_PIN))
        node.exec("INSERT INTO kkm_user VALUES ('u-cashier', ?, 'Айгерім', 'CASHIER', ?, 2)", KKM, NodeFixture.nodePinHash(CASHIER_PIN))
        val keys = listOf("k-sale" to "d-sale", "k-ret" to "d-return", "k-in" to "d-in", "k-flight" to null)
        keys.forEach { (key, ref) ->
            node.exec("INSERT INTO idempotency VALUES (?, ?, 'CREATE_RECEIPT', 60, ?, ?)", KKM, key, if (ref == null) "CREATED" else "DONE", ref)
        }
    }
}
