package kz.mybrain.superkassa.core.domain.helper

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long
import kotlinx.serialization.json.boolean
import kz.mybrain.superkassa.core.domain.model.common.CounterKeyFormats
import kz.mybrain.superkassa.core.domain.model.common.format
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Объект-парсер накопительных счетчиков и метаданных смены из JSON-пакетов ОФД.
 *
 * Преобразует сырые JSON-данные отчета ОФД в плоские мапы счетчиков
 * для удобного хранения и последующей сверки/агрегации.
 */
object OfdInfoCountersSnapshotParser {

    /**
     * Снимок состояния счетчиков и метаданных смены, полученный из ответа ОФД.
     *
     * @property reportType Тип отчета (например, "REPORT_X" или "REPORT_Z").
     * @property shiftNumber Номер смены.
     * @property isOpenShift Флаг, указывающий, открыта ли смена (обычно true для X-отчетов).
     * @property openShiftTimeMillis Время открытия смены в миллисекундах (Unix Epoch).
     * @property closeShiftTimeMillis Время закрытия смены в миллисекундах (Unix Epoch).
     * @property globalCounters Глобальные накопительные счетчики (не обнуляемые между сменами).
     * @property shiftCounters Счетчики, сбрасываемые в рамках текущей смены.
     */
    class Snapshot(
        val reportType: String,
        val shiftNumber: Int?,
        val isOpenShift: Boolean,
        val openShiftTimeMillis: Long?,
        val closeShiftTimeMillis: Long?,
        val globalCounters: Map<String, Long>,
        val shiftCounters: Map<String, Long>
    )

    /**
     * Выполняет парсинг JSON-ответа ОФД и строит объект [Snapshot].
     *
     * @param response JSON-объект ответа от ОФД.
     * @return Разобранный снимок состояния [Snapshot].
     * @throws IllegalArgumentException если в ответе отсутствует полезная нагрузка ("payload") или блок отчета.
     */
    fun parse(response: JsonObject): Snapshot {
        val payload = response["payload"]?.jsonObject ?: throw IllegalArgumentException("No payload found")

        val report = payload["report"]?.jsonObject
        val service = payload["service"]?.jsonObject

        val reportType = report?.get("reportType")?.jsonPrimitive?.content
            ?: (if (service?.get("lastZReport") != null) "REPORT_Z" else "REPORT_X")

        val isOpenShift = reportType == "REPORT_X"

        val zxReport = report?.get("zxReport")?.jsonObject
            ?: service?.get("lastZReport")?.jsonObject
            ?: throw IllegalArgumentException("No report object found")

        val shiftNumber = zxReport["shiftNumber"]?.jsonPrimitive?.int

        val openShiftTimeMillis = zxReport["openShiftTime"]?.jsonObject?.let { parseDateTimeMillis(it) }
        val closeShiftTimeMillis = zxReport["closeShiftTime"]?.jsonObject?.let { parseDateTimeMillis(it) }

        val globalCounters = mutableMapOf<String, Long>()
        val shiftCounters = mutableMapOf<String, Long>()

        // 1. Парсинг блока operations (счетчики операций за смену)
        zxReport["operations"]?.jsonArray?.forEach { element ->
            val obj = element.jsonObject
            val operation = obj["operation"]?.jsonPrimitive?.content ?: return@forEach
            val count = obj["count"]?.jsonPrimitive?.long ?: 0L
            val sum = moneyBills(obj, "sum")

            shiftCounters[CounterKeyFormats.OPERATION_COUNT.format(operation)] = count
            shiftCounters[CounterKeyFormats.OPERATION_SUM.format(operation)] = sum
        }

        // 2. Парсинг блока ticketOperations (чековые операции и виды оплат)
        zxReport["ticketOperations"]?.jsonArray?.forEach { element ->
            val obj = element.jsonObject
            val operation = obj["operation"]?.jsonPrimitive?.content ?: return@forEach

            shiftCounters[CounterKeyFormats.TICKET_TOTAL_COUNT.format(operation)] = obj["ticketsTotalCount"]?.jsonPrimitive?.long ?: 0L
            shiftCounters[CounterKeyFormats.TICKET_COUNT.format(operation)] = obj["ticketsCount"]?.jsonPrimitive?.long ?: 0L
            shiftCounters[CounterKeyFormats.TICKET_SUM.format(operation)] = moneyBills(obj, "ticketsSum")
            shiftCounters[CounterKeyFormats.TICKET_OFFLINE_COUNT.format(operation)] = obj["offlineCount"]?.jsonPrimitive?.long ?: 0L
            shiftCounters[CounterKeyFormats.TICKET_DISCOUNT_SUM.format(operation)] = moneyBills(obj, "discountSum")
            shiftCounters[CounterKeyFormats.TICKET_MARKUP_SUM.format(operation)] = moneyBills(obj, "markupSum")
            shiftCounters[CounterKeyFormats.TICKET_CHANGE_SUM.format(operation)] = moneyBills(obj, "changeSum")

            obj["payments"]?.jsonArray?.forEach { pe ->
                val pObj = pe.jsonObject
                val payment = pObj["payment"]?.jsonPrimitive?.content ?: return@forEach
                val pSum = moneyBills(pObj, "sum")
                val pCount = pObj["count"]?.jsonPrimitive?.long ?: 0L

                shiftCounters[CounterKeyFormats.PAYMENT_SUM.format(operation, payment)] = pSum
                shiftCounters[CounterKeyFormats.PAYMENT_COUNT.format(operation, payment)] = pCount
            }
        }

        // 3. Парсинг общей суммы наличных в кассе (cashSum)
        val cashSum = moneyBills(zxReport, "cashSum")
        shiftCounters[CounterKeyFormats.CASH_SUM] = cashSum

        // 4. Парсинг общей выручки (revenue)
        zxReport["revenue"]?.jsonObject?.let { rev ->
            val revSum = moneyBills(rev, "sum")
            val isNegative = rev["isNegative"]?.jsonPrimitive?.boolean == true
            shiftCounters[CounterKeyFormats.REVENUE_SUM] = revSum
            shiftCounters[CounterKeyFormats.REVENUE_IS_NEGATIVE] = if (isNegative) 1L else 0L
        }

        // 5. Парсинг накопительных сумм на конец смены (nonNullableSums - глобальные счетчики)
        zxReport["nonNullableSums"]?.jsonArray?.forEach { element ->
            val obj = element.jsonObject
            val operation = obj["operation"]?.jsonPrimitive?.content ?: return@forEach
            val sum = moneyBills(obj, "sum")
            globalCounters[CounterKeyFormats.NON_NULLABLE_SUM.format(operation)] = sum
        }

        // 6. Парсинг накопительных сумм на начало смены (startShiftNonNullableSums)
        zxReport["startShiftNonNullableSums"]?.jsonArray?.forEach { element ->
            val obj = element.jsonObject
            val operation = obj["operation"]?.jsonPrimitive?.content ?: return@forEach
            val sum = moneyBills(obj, "sum")
            shiftCounters[CounterKeyFormats.START_SHIFT_NON_NULLABLE_SUM.format(operation)] = sum
        }

        return Snapshot(
            reportType = reportType,
            shiftNumber = shiftNumber,
            isOpenShift = isOpenShift,
            openShiftTimeMillis = openShiftTimeMillis,
            closeShiftTimeMillis = closeShiftTimeMillis,
            globalCounters = globalCounters,
            shiftCounters = shiftCounters
        )
    }

    /**
     * Конвертирует вложенную дату и время ОФД в миллисекунды.
     */
    private fun parseDateTimeMillis(timeObj: JsonObject): Long? {
        return try {
            val date = timeObj["date"]?.jsonObject ?: return null
            val time = timeObj["time"]?.jsonObject ?: return null

            val year = date["year"]?.jsonPrimitive?.int ?: return null
            val month = date["month"]?.jsonPrimitive?.int ?: return null
            val day = date["day"]?.jsonPrimitive?.int ?: return null

            val hour = time["hour"]?.jsonPrimitive?.int ?: 0
            val minute = time["minute"]?.jsonPrimitive?.int ?: 0
            val second = time["second"]?.jsonPrimitive?.int ?: 0

            val localDateTime = LocalDateTime(year, month, day, hour, minute, second)
            localDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Вспомогательный метод для безопасного извлечения целой части денежной суммы (в тиынах/копейках).
     */
    private fun moneyBills(obj: JsonObject, key: String): Long {
        val money = obj[key]?.jsonObject ?: return 0L
        return money["bills"]?.jsonPrimitive?.long ?: 0L
    }
}

