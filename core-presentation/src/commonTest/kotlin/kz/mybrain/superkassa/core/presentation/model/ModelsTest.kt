package kz.mybrain.superkassa.core.presentation.model

import kotlinx.serialization.json.Json
import kz.mybrain.superkassa.core.domain.model.common.TaxRegime
import kz.mybrain.superkassa.core.domain.model.common.UnitOfMeasurement
import kz.mybrain.superkassa.core.domain.model.common.VatGroup
import kz.mybrain.superkassa.core.domain.model.auth.UserRole
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testCommonModelsSerialization() {
        val paginated = PaginatedResponse(
            items = listOf("item1", "item2"),
            total = 10,
            limit = 2,
            offset = 0,
            hasMore = true
        )
        val paginatedStr = json.encodeToString(paginated)
        val paginatedDec = json.decodeFromString<PaginatedResponse<String>>(paginatedStr)
        assertEquals(paginated.items, paginatedDec.items)
        assertEquals(paginated.total, paginatedDec.total)

        val apiError = ApiErrorResponse(
            code = "TEST_ERR",
            message = "Test error message",
            details = "Some details"
        )
        val apiErrorStr = json.encodeToString(apiError)
        val apiErrorDec = json.decodeFromString<ApiErrorResponse>(apiErrorStr)
        assertEquals(apiError.code, apiErrorDec.code)

        val factoryInfo = FactoryNumberResponse(
            factoryNumber = "SWK-2026",
            manufactureYear = 2026
        )
        val factoryInfoStr = json.encodeToString(factoryInfo)
        val factoryInfoDec = json.decodeFromString<FactoryNumberResponse>(factoryInfoStr)
        assertEquals(factoryInfo.factoryNumber, factoryInfoDec.factoryNumber)

        val uom = UnitOfMeasurement.PIECE
        val uomResponse = UnitOfMeasurementResponse.from(uom)
        val uomStr = json.encodeToString(uomResponse)
        val uomDec = json.decodeFromString<UnitOfMeasurementResponse>(uomStr)
        assertEquals(uomResponse.code, uomDec.code)

        val vatRate = VatRateResponse.from(VatGroup.VAT_10)
        val vatRateStr = json.encodeToString(vatRate)
        val vatRateDec = json.decodeFromString<VatRateResponse>(vatRateStr)
        assertEquals(vatRate.code, vatRateDec.code)
    }

    @Test
    fun testUserModelsSerialization() {
        val createReq = UserCreateRequest(
            name = "Test User",
            role = UserRole.CASHIER,
            userPin = "1234"
        )
        val createStr = json.encodeToString(createReq)
        val createDec = json.decodeFromString<UserCreateRequest>(createStr)
        assertEquals(createReq.name, createDec.name)

        val updateReq = UserUpdateRequest(
            name = "Updated Name",
            role = UserRole.ADMIN,
            userPin = "4321"
        )
        val updateStr = json.encodeToString(updateReq)
        val updateDec = json.decodeFromString<UserUpdateRequest>(updateStr)
        assertEquals(updateReq.name, updateDec.name)

        val deleteReq = UserDeleteRequest("deprecated")
        val deleteStr = json.encodeToString(deleteReq)
        val deleteDec = json.decodeFromString<UserDeleteRequest>(deleteStr)
        assertEquals(deleteReq._unused, deleteDec._unused)

        val response = UserResponse(
            userId = "user-1",
            name = "User One",
            role = UserRole.CASHIER,
            pin = "1234"
        )
        val responseStr = json.encodeToString(response)
        val responseDec = json.decodeFromString<UserResponse>(responseStr)
        assertEquals(response.userId, responseDec.userId)
    }

    @Test
    fun testKkmModelsSerialization() {
        val response = KkmResponse(
            kkmId = "kkm-1",
            createdAt = 1000L,
            updatedAt = 2000L,
            mode = "ACTIVE",
            state = "ACTIVE",
            ofdId = "telecom",
            ofdEnvironment = "prod",
            kkmKgdId = "kgd-1",
            factoryNumber = "SWK-0001",
            ofdSystemId = "12345",
            autoCloseShift = true
        )
        val responseStr = json.encodeToString(response)
        val responseDec = json.decodeFromString<KkmResponse>(responseStr)
        assertEquals(response.kkmId, responseDec.kkmId)

        val kkmInfo = KkmInfo(
            id = "kkm-1",
            createdAt = 1000L,
            updatedAt = 2000L,
            mode = "ACTIVE",
            state = "ACTIVE",
            registrationNumber = "kgd-1",
            factoryNumber = "SWK-0001",
            ofdProvider = "telecom_prod",
            systemId = "12345"
        )

        val listResult = KkmListResult(
            items = listOf(kkmInfo),
            total = 1
        )
        assertEquals(listResult.total, 1)
        assertEquals(listResult.items.first().id, "kkm-1")

        val listParams = KkmListParams(
            limit = 10,
            offset = 0,
            state = "ACTIVE"
        )
        val listParamsStr = json.encodeToString(listParams)
        val listParamsDec = json.decodeFromString<KkmListParams>(listParamsStr)
        assertEquals(listParams.limit, listParamsDec.limit)

        val settingsUpdate = KkmSettingsUpdateRequest(
            autoCloseShift = true
        )
        val settingsUpdateStr = json.encodeToString(settingsUpdate)
        val settingsUpdateDec = json.decodeFromString<KkmSettingsUpdateRequest>(settingsUpdateStr)
        assertEquals(settingsUpdate.autoCloseShift, settingsUpdateDec.autoCloseShift)

        val draftUpdate = KkmDraftUpdateRequest(
            ofdId = "kazakhtelecom",
            ofdEnvironment = "test"
        )
        val draftUpdateStr = json.encodeToString(draftUpdate)
        val draftUpdateDec = json.decodeFromString<KkmDraftUpdateRequest>(draftUpdateStr)
        assertEquals(draftUpdate.ofdId, draftUpdateDec.ofdId)

        val taxUpdate = KkmTaxSettingsUpdateRequest(
            taxRegime = TaxRegime.MIXED,
            defaultVatGroup = VatGroup.VAT_16
        )
        val taxUpdateStr = json.encodeToString(taxUpdate)
        val taxUpdateDec = json.decodeFromString<KkmTaxSettingsUpdateRequest>(taxUpdateStr)
        assertEquals(taxUpdate.taxRegime, taxUpdateDec.taxRegime)

        val initSimple = KkmInitSimpleRequest(
            ofdId = "kazakhtelecom",
            ofdEnvironment = "test",
            ofdSystemId = "200367",
            ofdToken = "32876190"
        )
        val initSimpleStr = json.encodeToString(initSimple)
        val initSimpleDec = json.decodeFromString<KkmInitSimpleRequest>(initSimpleStr)
        assertEquals(initSimple.ofdId, initSimpleDec.ofdId)

        val initDirect = KkmInitDirectRequest(
            ofdId = "kazakhtelecom",
            ofdEnvironment = "test",
            ofdSystemId = "system-id-12345",
            ofdToken = "token-abc-123",
            kkmKgdId = "123456789012",
            factoryNumber = "SWK-0001",
            manufactureYear = 2024
        )
        val initDirectStr = json.encodeToString(initDirect)
        val initDirectDec = json.decodeFromString<KkmInitDirectRequest>(initDirectStr)
        assertEquals(initDirect.factoryNumber, initDirectDec.factoryNumber)

        val initDraft = KkmInitDraftRequest(
            kkmId = "draft-1",
            ofdSystemId = "system-id-123",
            ofdToken = "token-123",
            kkmKgdId = "kgd-123"
        )
        val initDraftStr = json.encodeToString(initDraft)
        val initDraftDec = json.decodeFromString<KkmInitDraftRequest>(initDraftStr)
        assertEquals(initDraft.kkmId, initDraftDec.kkmId)
    }

    @Test
    fun testDraftKkmModelsSerialization() {
        val request = DraftKkmRequest(
            ofdId = "kazakhtelecom",
            ofdEnvironment = "test"
        )
        val requestStr = json.encodeToString(request)
        val requestDec = json.decodeFromString<DraftKkmRequest>(requestStr)
        assertEquals(request.ofdId, requestDec.ofdId)

        val response = DraftKkmResponse(
            kkmId = "kkm-1",
            factoryNumber = "SWK-0001",
            manufactureYear = 2024
        )
        val responseStr = json.encodeToString(response)
        val responseDec = json.decodeFromString<DraftKkmResponse>(responseStr)
        assertEquals(response.kkmId, responseDec.kkmId)
    }

    @Test
    fun testReceiptModelsSerialization() {
        val item = ReceiptItemDto(
            name = "Item 1",
            price = 10.0,
            quantity = 2L,
            vatGroup = "VAT_16",
            measureUnitCode = "796"
        )
        val itemStr = json.encodeToString(item)
        val itemDec = json.decodeFromString<ReceiptItemDto>(itemStr)
        assertEquals(item.price, itemDec.price)

        val payment = ReceiptPaymentDto(
            type = "CASH",
            sum = 20.0
        )
        val paymentStr = json.encodeToString(payment)
        val paymentDec = json.decodeFromString<ReceiptPaymentDto>(paymentStr)
        assertEquals(payment.type, paymentDec.type)

        val parent = ParentTicketDto(
            parentTicketNumber = 123L,
            parentTicketDateTime = "2026-06-27T16:00:00Z",
            kgdKkmId = "kgd-123",
            parentTicketTotal = 1500.0,
            parentTicketIsOffline = false
        )
        val parentStr = json.encodeToString(parent)
        val parentDec = json.decodeFromString<ParentTicketDto>(parentStr)
        assertEquals(parent.parentTicketNumber, parentDec.parentTicketNumber)

        val sell = ReceiptSellRequest(
            items = listOf(item),
            payments = listOf(payment),
            idempotencyKey = "key-sell"
        )
        val sellStr = json.encodeToString(sell)
        val sellDec = json.decodeFromString<ReceiptSellRequest>(sellStr)
        assertEquals(sell.idempotencyKey, sellDec.idempotencyKey)

        val sellReturn = ReceiptSellReturnRequest(
            items = listOf(item),
            payments = listOf(payment),
            idempotencyKey = "key-sell-ret",
            parentTicket = parent
        )
        val sellReturnStr = json.encodeToString(sellReturn)
        val sellReturnDec = json.decodeFromString<ReceiptSellReturnRequest>(sellReturnStr)
        assertEquals(sellReturn.idempotencyKey, sellReturnDec.idempotencyKey)

        val buy = ReceiptBuyRequest(
            items = listOf(item),
            payments = listOf(payment),
            idempotencyKey = "key-buy"
        )
        val buyStr = json.encodeToString(buy)
        val buyDec = json.decodeFromString<ReceiptBuyRequest>(buyStr)
        assertEquals(buy.idempotencyKey, buyDec.idempotencyKey)

        val buyReturn = ReceiptBuyReturnRequest(
            items = listOf(item),
            payments = listOf(payment),
            idempotencyKey = "key-buy-ret",
            parentTicket = parent
        )
        val buyReturnStr = json.encodeToString(buyReturn)
        val buyReturnDec = json.decodeFromString<ReceiptBuyReturnRequest>(buyReturnStr)
        assertEquals(buyReturn.idempotencyKey, buyReturnDec.idempotencyKey)
    }

    @Test
    fun testShiftModelsSerialization() {
        val close = CloseShiftRequest()
        val closeStr = json.encodeToString(close)
        val closeDec = json.decodeFromString<CloseShiftRequest>(closeStr)
        assertEquals(close, closeDec)

        val autoClose = AutoCloseShiftRequest(autoCloseShift = true)
        val autoCloseStr = json.encodeToString(autoClose)
        val autoCloseDec = json.decodeFromString<AutoCloseShiftRequest>(autoCloseStr)
        assertEquals(autoClose.autoCloseShift, autoCloseDec.autoCloseShift)

        val xreport = XReportRequest()
        val xreportStr = json.encodeToString(xreport)
        val xreportDec = json.decodeFromString<XReportRequest>(xreportStr)
        assertEquals(xreport, xreportDec)
    }

    @Test
    fun testOfdModelsSerialization() {
        val tokenUpdate = OfdTokenUpdateRequest(
            token = "new-token"
        )
        val tokenUpdateStr = json.encodeToString(tokenUpdate)
        val tokenUpdateDec = json.decodeFromString<OfdTokenUpdateRequest>(tokenUpdateStr)
        assertEquals(tokenUpdate.token, tokenUpdateDec.token)

        val authResponse = OfdAuthInfoResponse(
            token = "token",
            nextReqNum = 10
        )
        val authResponseStr = json.encodeToString(authResponse)
        val authResponseDec = json.decodeFromString<OfdAuthInfoResponse>(authResponseStr)
        assertEquals(authResponse.token, authResponseDec.token)
    }

    @Test
    fun testDeliveryModelsSerialization() {
        val item = DeliveryRetryItemResponse(
            channel = "PRINT",
            success = true
        )
        val itemStr = json.encodeToString(item)
        val itemDec = json.decodeFromString<DeliveryRetryItemResponse>(itemStr)
        assertEquals(item.channel, itemDec.channel)

        val response = DeliveryRetryResponse(
            results = listOf(item)
        )
        val responseStr = json.encodeToString(response)
        val responseDec = json.decodeFromString<DeliveryRetryResponse>(responseStr)
        assertEquals(response.results.size, responseDec.results.size)
    }

    @Test
    fun testAuthModelsSerialization() {
        val pin = PinRequest(
            _unused = "deprecated"
        )
        val pinStr = json.encodeToString(pin)
        val pinDec = json.decodeFromString<PinRequest>(pinStr)
        assertEquals(pin._unused, pinDec._unused)

        val mode = AuthMode.BEARER
        val modeStr = json.encodeToString(mode)
        val modeDec = json.decodeFromString<AuthMode>(modeStr)
        assertEquals(mode, modeDec)
    }
}
