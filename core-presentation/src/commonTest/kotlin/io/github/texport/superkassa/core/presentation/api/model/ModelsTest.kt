package io.github.texport.superkassa.core.presentation.api.model

import kotlinx.serialization.json.Json
import io.github.texport.superkassa.core.domain.model.common.UnitOfMeasurement
import io.github.texport.superkassa.core.domain.model.common.VatGroup
import io.github.texport.superkassa.core.domain.model.kkm.KkmInfo
import io.github.texport.superkassa.core.presentation.impl.mapper.ReceiptMapper
import io.github.texport.superkassa.core.presentation.api.model.toDto
import io.github.texport.superkassa.core.presentation.api.model.toDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
            role = UserRoleDto.CASHIER,
            userPin = "1234"
        )
        val createStr = json.encodeToString(createReq)
        val createDec = json.decodeFromString<UserCreateRequest>(createStr)
        assertEquals(createReq.name, createDec.name)

        val updateReq = UserUpdateRequest(
            name = "Updated Name",
            role = UserRoleDto.ADMIN,
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
            role = UserRoleDto.CASHIER,
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
            taxRegime = TaxRegimeDto.MIXED,
            defaultVatGroup = VatGroupDto.VAT_16
        )
        val taxUpdateStr = json.encodeToString(taxUpdate)
        val taxUpdateDec = json.decodeFromString<KkmTaxSettingsUpdateRequest>(taxUpdateStr)
        assertEquals(taxUpdate.taxRegime, taxUpdateDec.taxRegime)
        assertEquals(taxUpdate.defaultVatGroup, taxUpdateDec.defaultVatGroup)

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
            quantity = 2.0,
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

    @Test
    fun testOfflineQueueModelsSerialization() {
        val req = QueueStatusRequest(kkmId = "kkm-test")
        val reqStr = json.encodeToString(req)
        val reqDec = json.decodeFromString<QueueStatusRequest>(reqStr)
        assertEquals(req.kkmId, reqDec.kkmId)

        val resp = QueueStatusResponse(hasPendingItems = true, pendingCount = 5)
        val respStr = json.encodeToString(resp)
        val respDec = json.decodeFromString<QueueStatusResponse>(respStr)
        assertEquals(resp.hasPendingItems, respDec.hasPendingItems)
        assertEquals(resp.pendingCount, respDec.pendingCount)
    }

    @Test
    fun testModelMappingAndFieldCoverage() {
        // 1. ApiErrorResponse
        val apiErr = ApiErrorResponse(code = "ERR", message = "Msg", details = "Det")
        assertEquals("ERR", apiErr.code)
        assertEquals("Msg", apiErr.message)
        assertEquals("Det", apiErr.details)

        // 2. UserResponse
        val userResp = UserResponse(userId = "u1", name = "N1", role = UserRoleDto.CASHIER, pin = "1234")
        assertEquals("u1", userResp.userId)
        assertEquals("N1", userResp.name)
        assertEquals(UserRoleDto.CASHIER, userResp.role)
        assertEquals("1234", userResp.pin)

        // 3. DraftKkmRequest & Response
        val draftReq = DraftKkmRequest(ofdId = "ofd", ofdEnvironment = "env")
        assertEquals("ofd", draftReq.ofdId)
        assertEquals("env", draftReq.ofdEnvironment)

        // 4. OfdServiceInfo mapping
        val domainOfd = io.github.texport.superkassa.core.domain.model.ofd.OfdServiceInfo(
            orgTitle = "T1",
            orgAddress = "A1",
            orgAddressKz = "AK1",
            orgInn = "I1",
            orgOkved = "O1",
            geoLatitude = 10,
            geoLongitude = 20,
            geoSource = "S1"
        )
        val dtoOfd = domainOfd.toDto()
        assertEquals("T1", dtoOfd.orgTitle)
        assertEquals("A1", dtoOfd.orgAddress)
        assertEquals("AK1", dtoOfd.orgAddressKz)
        assertEquals("I1", dtoOfd.orgInn)
        assertEquals("O1", dtoOfd.orgOkved)
        assertEquals(10, dtoOfd.geoLatitude)
        assertEquals(20, dtoOfd.geoLongitude)
        assertEquals("S1", dtoOfd.geoSource)

        val domainOfd2 = dtoOfd.toDomain()
        assertEquals(domainOfd, domainOfd2)

        // 5. ReceiptBranding mapping
        val domainBranding = io.github.texport.superkassa.core.domain.model.receipt.ReceiptBranding(
            language = io.github.texport.superkassa.core.domain.model.receipt.ReceiptLanguage.MIXED,
            headerLogoUrl = "logo",
            paperWidthMm = 58,
            themeColor = "red",
            beforeHeaderMsg = "bm1",
            headerMsg = "hm1",
            afterHeaderMsg = "ahm1",
            beforeItemsMsg = "bim1",
            afterItemsMsg = "aim1",
            beforeTotalsMsg = "btm1",
            afterTotalsMsg = "atm1",
            beforeQrMsg = "bq1",
            footerMsg = "fm1",
            useForceDarkTheme = true,
            customBackgroundColorHex = "bg",
            customCardTopBorderColorHex = "border",
            ofdTicketAds = listOf("ad1"),
            printOfdTicketAds = false
        )
        val dtoBranding = domainBranding.toDto()
        assertEquals(ReceiptLanguageDto.MIXED, dtoBranding.language)
        assertEquals("logo", dtoBranding.headerLogoUrl)
        assertEquals(58, dtoBranding.paperWidthMm)
        assertEquals("red", dtoBranding.themeColor)
        assertEquals("bm1", dtoBranding.beforeHeaderMsg)
        assertEquals("hm1", dtoBranding.headerMsg)
        assertEquals("ahm1", dtoBranding.afterHeaderMsg)
        assertEquals("bim1", dtoBranding.beforeItemsMsg)
        assertEquals("aim1", dtoBranding.afterItemsMsg)
        assertEquals("btm1", dtoBranding.beforeTotalsMsg)
        assertEquals("atm1", dtoBranding.afterTotalsMsg)
        assertEquals("bq1", dtoBranding.beforeQrMsg)
        assertEquals("fm1", dtoBranding.footerMsg)
        assertTrue(dtoBranding.useForceDarkTheme)
        assertEquals("bg", dtoBranding.customBackgroundColorHex)
        assertEquals("border", dtoBranding.customCardTopBorderColorHex)
        assertEquals(listOf("ad1"), dtoBranding.ofdTicketAds)
        assertTrue(!dtoBranding.printOfdTicketAds)

        val domainBranding2 = dtoBranding.toDomain()
        assertEquals(domainBranding, domainBranding2)

        // 6. Additional Receipt mapping using ReceiptMapper (ReceiptItemDto to ItemInput)
        val itemDto = ReceiptItemDto(
            name = "Item",
            price = 100.0,
            quantity = 2.0,
            vatGroup = "VAT_16",
            measureUnitCode = "796"
        )
        val itemInput = ReceiptMapper.toItemInput(itemDto)
        assertEquals("Item", itemInput.name)
        assertEquals(100.0, itemInput.price)
        assertEquals(2.0, itemInput.quantity)
        assertEquals("VAT_16", itemInput.vatGroup)
        assertEquals("796", itemInput.measureUnitCode)

        // 7. ReceiptPaymentDto using ReceiptMapper (ReceiptPaymentDto to PaymentInput)
        val paymentDto = ReceiptPaymentDto(type = "CASH", sum = 150.0)
        val paymentInput = ReceiptMapper.toPaymentInput(paymentDto)
        assertEquals("CASH", paymentInput.type)
        assertEquals(150.0, paymentInput.sum)

        // 8. ParentTicketDto using ReceiptMapper (ParentTicketDto to ParentTicket)
        val parentDto = ParentTicketDto(
            parentTicketNumber = 12L,
            parentTicketDateTime = "2026-06-27T16:00:00Z",
            kgdKkmId = "kgd-12",
            parentTicketTotal = 500.0,
            parentTicketIsOffline = true
        )
        val domainParent = ReceiptMapper.toParentTicket(parentDto)!!
        assertEquals(12L, domainParent.parentTicketNumber)
        assertEquals("kgd-12", domainParent.kgdKkmId)
        assertTrue(domainParent.parentTicketIsOffline)
    }

    @Test
    fun testInvalidModelsAndValidationErrors() {
        // Validation tests for KkmListParams
        try {
            KkmListParams(sortBy = "invalid")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("sortBy"))
        }

        // Validation tests for ReceiptSellRequest
        try {
            ReceiptSellRequest(items = emptyList(), payments = emptyList(), discountPercent = 10.0, discountSum = 100.0, idempotencyKey = "key")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("discount"))
        }
        try {
            ReceiptSellRequest(items = emptyList(), payments = emptyList(), markupPercent = 10.0, markupSum = 100.0, idempotencyKey = "key")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("наценку"))
        }

        // Validation tests for ReceiptSellReturnRequest
        try {
            ReceiptSellReturnRequest(items = emptyList(), payments = emptyList(), discountPercent = 10.0, discountSum = 100.0, idempotencyKey = "key")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("discount"))
        }
        try {
            ReceiptSellReturnRequest(items = emptyList(), payments = emptyList(), markupPercent = 10.0, markupSum = 100.0, idempotencyKey = "key")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("наценку"))
        }

        // Validation tests for ReceiptBuyRequest
        try {
            ReceiptBuyRequest(items = emptyList(), payments = emptyList(), discountPercent = 10.0, discountSum = 100.0, idempotencyKey = "key")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("discount"))
        }
        try {
            ReceiptBuyRequest(items = emptyList(), payments = emptyList(), markupPercent = 10.0, markupSum = 100.0, idempotencyKey = "key")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("наценку"))
        }

        // Validation tests for ReceiptBuyReturnRequest
        try {
            ReceiptBuyReturnRequest(items = emptyList(), payments = emptyList(), discountPercent = 10.0, discountSum = 100.0, idempotencyKey = "key")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("discount"))
        }
        try {
            ReceiptBuyReturnRequest(items = emptyList(), payments = emptyList(), markupPercent = 10.0, markupSum = 100.0, idempotencyKey = "key")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("наценку"))
        }

        // Validation tests for ReceiptItemDto
        try {
            ReceiptItemDto(name = "Item", price = 10.0, quantity = 1.0, discountPercent = 10.0, discountSum = 100.0)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("discount"))
        }
        try {
            ReceiptItemDto(name = "Item", price = 10.0, quantity = 1.0, markupPercent = 10.0, markupSum = 100.0)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("наценку"))
        }

        // Access properties of KkmInitDraftRequest
        val initDraft = KkmInitDraftRequest(kkmId = "draft-1", ofdSystemId = "sys-1", ofdToken = "token-1", kkmKgdId = "kgd-1")
        assertEquals("sys-1", initDraft.ofdSystemId)
        assertEquals("token-1", initDraft.ofdToken)
        assertEquals("kgd-1", initDraft.kkmKgdId)

        // Access properties of KkmResponse
        val kkmResp = KkmResponse(
            kkmId = "kkm-1",
            createdAt = 1000L,
            updatedAt = 2000L,
            mode = "ACTIVE",
            state = "ACTIVE",
            ofdId = "telecom",
            kkmKgdId = "kgd-1",
            factoryNumber = "FN",
            ofdSystemId = "sys"
        )
        assertEquals(1000L, kkmResp.createdAt)
        assertEquals(2000L, kkmResp.updatedAt)
        assertEquals("telecom", kkmResp.ofdId)
        assertEquals("kgd-1", kkmResp.kkmKgdId)
        assertEquals("FN", kkmResp.factoryNumber)
        assertEquals("sys", kkmResp.ofdSystemId)
    }
}
