package io.github.texport.superkassa.embedded

import java.util.Base64

/**
 * Пакеты протокола в том виде, в каком их присылает кабинет: запрос кассы
 * и ответ ОФД. Документ пробит на другой кассе — реквизиты у него свои.
 */
internal object ProtocolPackets {
    const val REGISTRATION_NUMBER = "NZ7700123456"
    const val TAXPAYER = "ТОО Пример"
    const val FISCAL_SIGN = "987654321012"

    private val registration = """
        "service": {"regInfo": {
          "kkm": {"fnsKkmId": "$REGISTRATION_NUMBER", "serialNumber": "SW7700987654", "kkmId": "260940000021"},
          "org": {"title": "$TAXPAYER", "address": "г. Алматы, пр. Абая, 1", "iin": "123456789012", "oked": "47110"}
        }}
    """.trimIndent()

    private const val MOMENT =
        """{"date": {"year": 2026, "month": 9, "day": 18}, "time": {"hour": 14, "minute": 53, "second": 7}}"""

    private fun money(bills: Long, coins: Int = 0) = """{"bills": "$bills", "coins": $coins}"""

    private val link = Base64.getEncoder().encodeToString("https://consumer.oofd.kz/r/417".toByteArray())

    val receipt = """
        {"request": {"command": "COMMAND_TICKET", "ticket": {
          "operation": "OPERATION_SELL", "dateTime": $MOMENT, "operator": {"code": 1, "name": "Кассир Алия"},
          "items": [{"type": "ITEM_TYPE_COMMODITY", "commodity": {
            "name": "Хлеб", "sectionCode": "001", "quantity": "2000", "price": ${money(150)}, "sum": ${money(300)},
            "measureUnitCode": "796", "taxes": [{"percent": 16000, "sum": ${money(41, 38)}}]}}],
          "payments": [{"type": "PAYMENT_CASH", "sum": ${money(300)}}],
          "amounts": {"total": ${money(300)}, "taken": ${money(300)}, "change": ${money(0)}},
          "printedDocumentNumber": "417", "frShiftNumber": 12
        }, $registration},
        "response": {"command": "COMMAND_TICKET", "result": {"resultCode": 0},
          "ticket": {"ticketNumber": "$FISCAL_SIGN", "qrCode": "$link"}}}
    """.trimIndent()

    private val totals = """
        {"dateTime": $MOMENT, "openShiftTime": $MOMENT, "shiftNumber": 12, "cashSum": ${money(15_000)},
         "operations": [{"operation": "OPERATION_SELL", "count": 7, "sum": ${money(20_000)}}]}
    """.trimIndent()

    fun report(kind: String) = """
        {"request": {"command": "COMMAND_REPORT",
          "report": {"zxReport": $totals, "report": "$kind", "printedDocumentNumber": "418"}, $registration},
         "response": {"command": "COMMAND_REPORT", "result": {"resultCode": 0}}}
    """.trimIndent()

    val placement = """
        {"request": {"command": "COMMAND_MONEY_PLACEMENT", "moneyPlacement": {
          "dateTime": $MOMENT, "operation": "MONEY_PLACEMENT_WITHDRAWAL", "sum": ${money(1000, 50)},
          "frShiftNumber": 12, "printedDocumentNumber": "420"}, $registration}}
    """.trimIndent()
}
