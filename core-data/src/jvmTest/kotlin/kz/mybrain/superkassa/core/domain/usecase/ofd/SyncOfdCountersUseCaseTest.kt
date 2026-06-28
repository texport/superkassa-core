package kz.mybrain.superkassa.core.domain.usecase.ofd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kz.mybrain.superkassa.core.data.adapter.Base64TokenCodecAdapter
import kz.mybrain.superkassa.core.data.adapter.OfdConfigAdapter
import kz.mybrain.superkassa.core.data.adapter.Sha256PinHasherAdapter
import kz.mybrain.superkassa.core.domain.model.auth.UserRole
import kz.mybrain.superkassa.core.domain.model.common.TimeValidationResult
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.kkm.KkmMode
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandRequest
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.ofd.OfdServiceInfo
import kz.mybrain.superkassa.core.domain.model.queue.OfflineQueueCommandRequest
import kz.mybrain.superkassa.core.domain.model.shift.ShiftStatus
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.IdGeneratorPort
import kz.mybrain.superkassa.core.domain.port.OfdManagerPort
import kz.mybrain.superkassa.core.domain.port.OfflineQueuePort
import kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper
import kz.mybrain.superkassa.core.support.TestStoragePort

class SyncOfdCountersUseCaseTest {

    @Test
    fun `syncOfdCounters opens shift and stores global plus shift counters from REPORT_X`() {
        val fixture = Fixture(reportXResponse())

        val result = fixture.useCase.execute(fixture.kkm.id, "1234")

        assertEquals(OfdCommandStatus.OK, result.status)
        val openShift = fixture.storage.findOpenShift(fixture.kkm.id)
        assertNotNull(openShift)
        assertEquals(12L, openShift.shiftNo)

        val shiftCounters = fixture.storage.loadCounters(fixture.kkm.id, "SHIFT", openShift.id)
        val globalCounters = fixture.storage.loadCounters(fixture.kkm.id, "GLOBAL", null)

        assertEquals(2L, shiftCounters["operation.OPERATION_SELL.count"])
        assertEquals(1000L, shiftCounters["operation.OPERATION_SELL.sum"])
        assertEquals(5000L, globalCounters["non_nullable.OPERATION_SELL.sum"])
    }

    @Test
    fun `syncOfdCounters closes local open shift when OFD returns REPORT_Z`() {
        val fixture = Fixture(reportXResponse())
        fixture.useCase.execute(fixture.kkm.id, "1234")
        val shiftBeforeClose = fixture.storage.findOpenShift(fixture.kkm.id)
        assertNotNull(shiftBeforeClose)

        fixture.ofd.responseJson = reportZResponse()
        val closeResult = fixture.useCase.execute(fixture.kkm.id, "1234")
        assertEquals(OfdCommandStatus.OK, closeResult.status)
        assertNull(fixture.storage.findOpenShift(fixture.kkm.id))

        val firstShift = fixture.storage.listShifts(fixture.kkm.id, limit = 1, offset = 0).first()
        assertEquals(ShiftStatus.CLOSED, firstShift.status)
    }

    private class Fixture(initialResponse: JsonObject) {
        val storage = TestStoragePort()
        private val pinHasher = Sha256PinHasherAdapter()
        private val tokenCodec = Base64TokenCodecAdapter()
        private val nowMillis = System.currentTimeMillis()
        private val clock = object : ClockPort {
            override fun now(): Long = nowMillis
            override fun currentYear(): Int = 2026
            override fun parseDateTimeToMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long = 0L
        }
        private val idGenerator = object : IdGeneratorPort {
            private var seq = 0
            override fun nextId(): String {
                seq += 1
                return "id-$seq"
            }
            override fun generateFactoryNumber(): String = "KZT26TEST12345"
        }
        val ofd = MutableOfdManager(initialResponse)
        private val queue = object : OfflineQueuePort {
            override fun canSendDirectly(kkmId: String): Boolean = true
            override fun enqueueOffline(command: OfflineQueueCommandRequest): Boolean = true
            override fun deleteQueuedCommands(kkmId: String): Boolean = true
            override fun processOfflineBatch(kkmId: String, limit: Int): Int = 0
        }

        val kkm =
            KkmInfo(
                id = "kkm-1",
                createdAt = clock.now(),
                updatedAt = clock.now(),
                mode = KkmMode.REGISTRATION.name,
                state = KkmState.ACTIVE.name,
                ofdProvider = "KAZAKHTELECOM:TEST",
                registrationNumber = "RN-1",
                factoryNumber = "FN-1",
                systemId = "1",
                ofdServiceInfo =
                    OfdServiceInfo(
                        orgTitle = "Org",
                        orgAddress = "Addr",
                        orgAddressKz = "Addr KZ",
                        orgInn = "123456789012",
                        orgOkved = "47301",
                        geoLatitude = 1,
                        geoLongitude = 1,
                        geoSource = "GPS"
                    ),
                tokenEncryptedBase64 = tokenCodec.encodeToken(12345L),
                tokenUpdatedAt = clock.now()
            )

        val useCase: SyncOfdCountersUseCase

        init {
            storage.createKkm(kkm)
            storage.createUser(
                kkmId = kkm.id,
                userId = "admin-1",
                name = "Admin",
                role = UserRole.ADMIN,
                pin = "1234",
                pinHash = pinHasher.hash("1234"),
                createdAt = clock.now()
            )

            val authorizeUserUseCase = kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase(storage, pinHasher)
            val generateRequestNumberUseCase = kz.mybrain.superkassa.core.domain.usecase.ofd.GenerateRequestNumberUseCase(storage)
            val ofdCommandRequestFactory = kz.mybrain.superkassa.core.domain.helper.ofd.OfdCommandRequestFactory(OfdConfigAdapter())
            val testTimeValidator = object : kz.mybrain.superkassa.core.domain.port.TimeValidatorPort {
                override fun validate(clock: ClockPort) = TimeValidationResult(true)
            }

            val kkmCommonHelper = KkmCommonHelper(
                storage = storage,
                clock = clock,
                timeValidator = testTimeValidator,
                tokenCodec = tokenCodec,
                generateRequestNumberUseCase = generateRequestNumberUseCase,
                ofdCommandRequestFactory = ofdCommandRequestFactory,
                ofd = ofd
            )

            useCase =
                SyncOfdCountersUseCase(
                    storage = storage,
                    queue = queue,
                    clock = clock,
                    idGenerator = idGenerator,
                    authorizeUserUseCase = authorizeUserUseCase,
                    kkmCommonHelper = kkmCommonHelper
                )
        }
    }

    private class MutableOfdManager(
        var responseJson: JsonObject
    ) : OfdManagerPort {
        override fun send(command: OfdCommandRequest): OfdCommandResult {
            return OfdCommandResult(
                status = OfdCommandStatus.OK,
                responseBin = null,
                responseJson = responseJson,
                responseToken = 12345L,
                responseReqNum = command.reqNum,
                resultCode = 0,
                resultText = null,
                errorMessage = null
            )
        }
    }

    private fun reportXResponse(): JsonObject =
        Json.parseToJsonElement(
            """
            {
              "payload": {
                "report": {
                  "reportType": "REPORT_X",
                  "zxReport": {
                    "shiftNumber": 12,
                    "openShiftTime": {
                      "date": { "year": 2026, "month": 3, "day": 19 },
                      "time": { "hour": 10, "minute": 0, "second": 0 }
                    },
                    "operations": [
                      { "operation": "OPERATION_SELL", "count": 2, "sum": { "bills": 1000, "coins": 0 } }
                    ],
                    "ticketOperations": [
                      {
                        "operation": "OPERATION_SELL",
                        "ticketsTotalCount": 2,
                        "ticketsCount": 2,
                        "ticketsSum": { "bills": 1000, "coins": 0 },
                        "payments": [
                          { "payment": "PAYMENT_CASH", "sum": { "bills": 700, "coins": 0 }, "count": 1 },
                          { "payment": "PAYMENT_CARD", "sum": { "bills": 300, "coins": 0 }, "count": 1 }
                        ],
                        "offlineCount": 0,
                        "discountSum": { "bills": 0, "coins": 0 },
                        "markupSum": { "bills": 0, "coins": 0 },
                        "changeSum": { "bills": 50, "coins": 0 }
                      }
                    ],
                    "cashSum": { "bills": 700, "coins": 0 },
                    "revenue": { "sum": { "bills": 1000, "coins": 0 }, "isNegative": false },
                    "nonNullableSums": [
                      { "operation": "OPERATION_SELL", "sum": { "bills": 5000, "coins": 0 } }
                    ],
                    "startShiftNonNullableSums": [
                      { "operation": "OPERATION_SELL", "sum": { "bills": 4000, "coins": 0 } }
                    ]
                  }
                }
              }
            }
            """.trimIndent()
        ).jsonObject

    private fun reportZResponse(): JsonObject =
        Json.parseToJsonElement(
            """
            {
              "payload": {
                "report": {
                  "reportType": "REPORT_Z",
                  "zxReport": {
                    "shiftNumber": 12,
                    "closeShiftTime": {
                      "date": { "year": 2026, "month": 3, "day": 19 },
                      "time": { "hour": 20, "minute": 1, "second": 2 }
                    },
                    "cashSum": { "bills": 0, "coins": 0 },
                    "revenue": { "sum": { "bills": 0, "coins": 0 }, "isNegative": false },
                    "nonNullableSums": [
                      { "operation": "OPERATION_SELL", "sum": { "bills": 5000, "coins": 0 } }
                    ]
                  }
                }
              }
            }
            """.trimIndent()
        ).jsonObject
}
