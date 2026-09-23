package io.github.texport.superkassa.core.data.room

import kz.kazakhtelecom.proto.v203.CloseShiftRequest
import kz.kazakhtelecom.proto.v203.CommandTypeEnum
import kz.kazakhtelecom.proto.v203.MoneyPlacementEnum
import kz.kazakhtelecom.proto.v203.MoneyPlacementRequest
import kz.kazakhtelecom.proto.v203.OperationTypeEnum
import kz.kazakhtelecom.proto.v203.PaymentTypeEnum
import kz.kazakhtelecom.proto.v203.ReportResponse
import kz.kazakhtelecom.proto.v203.ReportTypeEnum
import kz.kazakhtelecom.proto.v203.Request
import kz.kazakhtelecom.proto.v203.Response
import kz.kazakhtelecom.proto.v203.TicketRequest
import kz.kazakhtelecom.proto.v203.TicketResponse
import kz.kazakhtelecom.proto.v203.ZXReport
import kz.mybrain.network.OfdEndpoint
import kz.mybrain.network.OfdNetworkClient
import kz.kazakhtelecom.proto.v203.Money as BfdMoney
import kz.kazakhtelecom.proto.v203.Result as BfdResult

/**
 * БФД для проверок: разбирает каждый запрос кассы и принимает документ.
 *
 * Чеку отвечает своим номером — так касса получает номер документа от БФД,
 * как на стенде. Запросы хранятся разобранными: проверка читает то, что
 * касса отправила бы в БФД, а не то, что она записала у себя.
 *
 * Наличные в ящике и смену БФД ведёт так же, как референс: принятое
 * изъятие ложится в текущую смену БФД, закрытие смены с `withdraw_money`
 * изымает весь остаток в закрываемую смену и начинает следующую.
 * Сдачи в проверках нет, и здесь она не учитывается.
 */
internal class FakeBfd : OfdNetworkClient {
    private val received = mutableListOf<Request>()
    private val rejections = mutableMapOf<CommandTypeEnum, Int>()
    private val withdrawn = mutableMapOf<Int, Long>()
    private var lastTicketNumber = FIRST_TICKET_NUMBER - 1
    private var shift = 1
    private var cashTiyn = 0L

    val requests: List<Request> get() = received.toList()

    fun moneyPlacements(): List<MoneyPlacementRequest> = received.mapNotNull { it.money_placement }

    fun xReports(): List<ZXReport> = received.mapNotNull { it.report?.zx_report }

    fun closeShifts(): List<CloseShiftRequest> = received.mapNotNull { it.close_shift }

    /** Изъятия по сменам БФД, в тиынах: номер смены БФД — сумма. */
    fun withdrawnByShift(): Map<Int, Long> = withdrawn.toMap()

    /** Отказывать команде [command] кодом [code], пока не вызван [acceptAll]. */
    fun reject(command: CommandTypeEnum, code: Int) {
        rejections[command] = code
    }

    fun acceptAll() = rejections.clear()

    override suspend fun sendAndReceive(endpoint: OfdEndpoint, request: ByteArray): kotlin.Result<ByteArray> {
        val decoded = Request.ADAPTER.decode(request.copyOfRange(HEADER_SIZE, request.size))
        received += decoded
        val payload = Response.ADAPTER.encode(answer(decoded))
        return kotlin.Result.success(header(request, payload.size) + payload)
    }

    private fun answer(request: Request): Response {
        val code = rejections[request.command] ?: 0
        if (code == 0) apply(request)
        val ticket = request.command == CommandTypeEnum.COMMAND_TICKET && code == 0
        return Response(
            command = request.command,
            result = BfdResult(result_code = code, result_text = if (code == 0) "OK" else "Rejected"),
            ticket = if (ticket) TicketResponse(ticket_number = nextTicket()) else null,
            report = if (code == 0) reportOf(request) else null
        )
    }

    /** Отчёт в ответе: на X-отчёт — он же, на закрытие смены — Z-отчёт, как у референса. */
    private fun reportOf(request: Request): ReportResponse? =
        request.report?.let { ReportResponse(report = it.report, zx_report = it.zx_report) }
            ?: request.close_shift?.let { ReportResponse(report = ReportTypeEnum.REPORT_Z, zx_report = it.z_report) }

    private fun apply(request: Request) {
        request.ticket?.let { cashTiyn += cashOf(it) }
        request.money_placement?.let { place(it.operation, tiyn(it.sum)) }
        request.close_shift?.let {
            if (it.withdraw_money == true && cashTiyn != 0L) place(MoneyPlacementEnum.MONEY_PLACEMENT_WITHDRAWAL, cashTiyn)
            shift += 1
        }
    }

    private fun place(operation: MoneyPlacementEnum, sum: Long) {
        if (operation == MoneyPlacementEnum.MONEY_PLACEMENT_WITHDRAWAL) {
            cashTiyn -= sum
            withdrawn[shift] = (withdrawn[shift] ?: 0L) + sum
        } else {
            cashTiyn += sum
        }
    }

    private fun cashOf(ticket: TicketRequest): Long {
        val cash = ticket.payments.filter { it.type == PaymentTypeEnum.PAYMENT_CASH }.sumOf { tiyn(it.sum) }
        val inflow = ticket.operation == OperationTypeEnum.OPERATION_SELL ||
            ticket.operation == OperationTypeEnum.OPERATION_BUY_RETURN
        return if (inflow) cash else -cash
    }

    private fun tiyn(money: BfdMoney): Long = money.bills * TIYN_IN_TENGE + money.coins

    private fun nextTicket(): String {
        lastTicketNumber += 1
        return lastTicketNumber.toString()
    }

    /** Заголовок ответа — заголовок запроса с общей длиной ответа (u32, little-endian, смещение 4). */
    private fun header(request: ByteArray, payloadSize: Int): ByteArray {
        val header = request.copyOf(HEADER_SIZE)
        val size = HEADER_SIZE + payloadSize
        for (i in 0 until SIZE_BYTES) header[SIZE_OFFSET + i] = (size shr (BYTE_BITS * i)).toByte()
        return header
    }

    companion object {
        /** Первый номер чека, который выдаёт БФД. */
        const val FIRST_TICKET_NUMBER = 9001L

        private const val HEADER_SIZE = 18
        private const val SIZE_OFFSET = 4
        private const val SIZE_BYTES = 4
        private const val BYTE_BITS = 8
        private const val TIYN_IN_TENGE = 100L
    }
}
