package io.github.texport.superkassa.core.data.room

import kz.kazakhtelecom.proto.v203.CommandTypeEnum
import kz.kazakhtelecom.proto.v203.MoneyPlacementRequest
import kz.kazakhtelecom.proto.v203.ReportResponse
import kz.kazakhtelecom.proto.v203.Request
import kz.kazakhtelecom.proto.v203.Response
import kz.kazakhtelecom.proto.v203.TicketResponse
import kz.kazakhtelecom.proto.v203.ZXReport
import kz.mybrain.network.OfdEndpoint
import kz.mybrain.network.OfdNetworkClient
import kz.kazakhtelecom.proto.v203.Result as BfdResult

/**
 * БФД для проверок: разбирает каждый запрос кассы и принимает документ.
 *
 * Чеку отвечает своим номером — так касса получает номер документа от БФД,
 * как на стенде. Запросы хранятся разобранными: проверка читает то, что
 * касса отправила бы в БФД, а не то, что она записала у себя.
 */
internal class FakeBfd : OfdNetworkClient {
    private val received = mutableListOf<Request>()
    private var lastTicketNumber = FIRST_TICKET_NUMBER - 1

    val requests: List<Request> get() = received.toList()

    fun moneyPlacements(): List<MoneyPlacementRequest> = received.mapNotNull { it.money_placement }

    fun xReports(): List<ZXReport> = received.mapNotNull { it.report?.zx_report }

    override suspend fun sendAndReceive(endpoint: OfdEndpoint, request: ByteArray): kotlin.Result<ByteArray> {
        val decoded = Request.ADAPTER.decode(request.copyOfRange(HEADER_SIZE, request.size))
        received += decoded
        val payload = Response.ADAPTER.encode(answer(decoded))
        return kotlin.Result.success(header(request, payload.size) + payload)
    }

    private fun answer(request: Request): Response = Response(
        command = request.command,
        result = BfdResult(result_code = 0, result_text = "OK"),
        ticket = if (request.command == CommandTypeEnum.COMMAND_TICKET) TicketResponse(ticket_number = nextTicket()) else null,
        report = request.report?.let { ReportResponse(report = it.report, zx_report = it.zx_report) }
    )

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
    }
}
