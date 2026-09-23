package io.github.texport.superkassa.testing.api.bfd

import io.github.texport.superkassa.testing.impl.bfd.BfdFaults
import io.github.texport.superkassa.testing.impl.bfd.BfdFaults.Fault
import io.github.texport.superkassa.testing.impl.bfd.BfdLedger
import io.github.texport.superkassa.testing.impl.bfd.BfdRegistration
import io.github.texport.superkassa.testing.impl.bfd.CpcrFrame
import kotlinx.coroutines.delay
import kz.kazakhtelecom.proto.v203.CloseShiftRequest
import kz.kazakhtelecom.proto.v203.CommandTypeEnum
import kz.kazakhtelecom.proto.v203.MoneyPlacementRequest
import kz.kazakhtelecom.proto.v203.Request
import kz.kazakhtelecom.proto.v203.TicketRequest
import kz.kazakhtelecom.proto.v203.ZXReport
import kz.mybrain.network.OfdEndpoint
import kz.mybrain.network.OfdNetworkClient
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * БФД внутри процесса: разбирает каждый запрос кассы по протоколу CPCR 2.0.3
 * и ведёт учёт каждой кассы по правилам прод-референса — токен, номер
 * запроса, повтор, номер чека, наличные в ящике и смена.
 *
 * Касса, впервые пришедшая в БФД, должна предъявить токен [firstToken]:
 * так касса заводится с токеном, выданным в личном кабинете. На запрос
 * сведений БФД отвечает регистрацией кассы у налогоплательщика [organization].
 *
 * Запросы хранятся разобранными: проверка читает то, что касса отправила
 * бы в БФД, а не то, что она записала у себя.
 *
 * Потокобезопасен: касса шлёт запросы из своих потоков, проверка читает
 * и заказывает сбои из своего.
 */
class FakeBfd(
    val firstToken: Long = FIRST_TOKEN,
    private val organization: BfdOrganization = BfdOrganization()
) : OfdNetworkClient {
    private val ledgers = ConcurrentHashMap<Long, BfdLedger>()
    private val received = CopyOnWriteArrayList<Exchange>()
    private val counted = CopyOnWriteArrayList<Exchange>()
    private val faults = BfdFaults()

    /**
     * Запрос кассы, как его видит БФД.
     *
     * @property kassa номер кассы в БФД.
     * @property token токен из заголовка.
     * @property reqNum номер запроса из заголовка.
     * @property request разобранное тело запроса.
     */
    data class Exchange(val kassa: Long, val token: Long, val reqNum: Int, val request: Request)

    /** Все запросы, дошедшие до БФД, в порядке прихода, включая отвергнутые и повторы. */
    val exchanges: List<Exchange> get() = received.toList()

    /** Тела всех дошедших запросов. */
    val requests: List<Request> get() = received.map { it.request }

    /** Документы, учтённые БФД: каждый ровно столько раз, сколько он учтён. */
    val countedDocuments: List<Exchange> get() = counted.toList()

    /** Чеки, учтённые БФД: повтор с тем же номером запроса второй раз не учитывается. */
    fun countedTickets(): List<TicketRequest> = counted.mapNotNull { it.request.ticket }

    /** Внесения и изъятия, дошедшие до БФД. */
    fun moneyPlacements(): List<MoneyPlacementRequest> = requests.mapNotNull { it.money_placement }

    /** X-отчёты, дошедшие до БФД. */
    fun xReports(): List<ZXReport> = requests.mapNotNull { it.report?.zx_report }

    /** Закрытия смены, дошедшие до БФД. */
    fun closeShifts(): List<CloseShiftRequest> = requests.mapNotNull { it.close_shift }

    /** Токен, с которым касса [kassa] обязана прийти в следующий раз. */
    fun issuedToken(kassa: Long): Long = ledger(kassa).issuedToken

    /** Изъятия кассы [kassa] по сменам БФД, в тиынах: номер смены БФД — сумма. */
    fun withdrawnByShift(kassa: Long): Map<Int, Long> = ledger(kassa).drawer.withdrawnByShift()

    /** Регистрационный номер КГД, который БФД сообщает кассе [kassa]. */
    fun kgdNumber(kassa: Long): String = BfdRegistration.kgdNumber(kassa)

    /** Следующий запрос до БФД не дойдёт. */
    fun unreachableOnce() = faults.addOnce(Fault.Unreachable)

    /** Связь с БФД пропала: ни один запрос не дойдёт, пока не вызван [connect]. */
    fun disconnect() {
        faults.connected = false
    }

    /** Связь с БФД восстановлена. */
    fun connect() {
        faults.connected = true
    }

    /** Следующий запрос БФД учтёт, а ответ до кассы не дойдёт. */
    fun loseNextAnswer() = faults.addOnce(Fault.Lost(waitForAnotherMillis = 0))

    /** Как [loseNextAnswer], но ответ задержан, пока касса не пришлёт другой запрос (не дольше [maxMillis]). */
    fun holdNextAnswerThenLose(maxMillis: Long) = faults.addOnce(Fault.Lost(waitForAnotherMillis = maxMillis))

    /** Следующему запросу БФД откажет кодом [code], ничего не учитывая. */
    fun refuseNext(code: Int) = faults.addOnce(Fault.Refused(code))

    /** Отказывать команде [command] кодом [code], пока не вызван [acceptAll]. */
    fun reject(command: CommandTypeEnum, code: Int) = faults.reject(command, code)

    /** Снимает отказы, заказанные [reject]. */
    fun acceptAll() = faults.acceptAll()

    override suspend fun sendAndReceive(endpoint: OfdEndpoint, request: ByteArray): Result<ByteArray> {
        val frame = CpcrFrame.parse(request)
        val exchange = Exchange(frame.kassa, frame.token, frame.reqNum, frame.request)
        received += exchange
        val arrived = received.size
        return when (val fault = faults.next(frame.request.command)) {
            Fault.Unreachable -> noAnswer()
            is Fault.Refused -> Result.success(frame.reply(frame.token, BfdLedger.refusal(frame.request, fault.code)))
            is Fault.Lost -> {
                answer(frame, exchange)
                awaitAnother(arrived, fault.waitForAnotherMillis)
                noAnswer()
            }
            null -> Result.success(answer(frame, exchange).let { frame.reply(it.token, it.response) })
        }
    }

    private fun answer(frame: CpcrFrame, exchange: Exchange): BfdLedger.Answer {
        val answer = ledger(frame.kassa).answer(frame.token, frame.reqNum, frame.request)
        if (answer.counted) counted += exchange
        return answer
    }

    private fun ledger(kassa: Long): BfdLedger =
        ledgers.computeIfAbsent(kassa) { BfdLedger(firstToken, BfdRegistration.of(it, organization)) }

    private suspend fun awaitAnother(arrived: Int, maxMillis: Long) {
        var waited = 0L
        while (received.size == arrived && waited < maxMillis) {
            delay(POLL_MILLIS)
            waited += POLL_MILLIS
        }
    }

    private fun noAnswer(): Result<ByteArray> = Result.failure(IOException("BFD response timeout"))

    companion object {
        /** Токен, который по умолчанию предъявляет касса при первом обращении. */
        const val FIRST_TOKEN = 123_456_789L

        /** Первый номер чека, который БФД выдаёт каждой кассе. */
        const val FIRST_TICKET_NUMBER = BfdLedger.FIRST_TICKET_NUMBER

        private const val POLL_MILLIS = 10L
    }
}
