package kz.mybrain.superkassa.core.data.adapter

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kz.mybrain.network.OfdNetworkClient
import kz.mybrain.superkassa.core.data.exception.DataErrorMessages
import kz.mybrain.superkassa.core.data.exception.OfdProtocolException
import kz.mybrain.superkassa.core.data.ofd.OfdProtocolCodec
import kz.mybrain.superkassa.core.data.ofd.OfdConfig
import kz.mybrain.superkassa.core.data.ofd.builder.strategy.OfdRequestBuilderStrategy
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandRequest
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType

class OfdManagerAdapterTest {
    private val config = OfdConfig("2.0.3")
    private val codec: OfdProtocolCodec = mockk()
    private val networkClient: OfdNetworkClient = mockk()
    private val builder: OfdRequestBuilderStrategy = mockk()
    
    private val adapter = OfdManagerAdapter(
        config = config,
        codec = codec,
        networkClient = networkClient,
        requestBuilders = listOf(builder),
        timeoutSeconds = 1L,
        reconnectIntervalSeconds = 1L
    )

    @Test
    fun testSendSuccess() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        val requestBytes = byteArrayOf(1, 2)
        val responseBytes = byteArrayOf(3, 4)
        
        val responseJson = Json.parseToJsonElement(
            """{
                "header": {"token": 124, "reqNum": 11},
                "payload": {
                    "result": {
                        "resultCode": 0,
                        "resultText": "Success"
                    },
                    "ticket": {
                        "fiscalSign": "12345678",
                        "qr_code": "http://ofd.kz/t/123"
                    }
                }
            }"""
        ) as JsonObject

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns responseJson
        every { codec.encode(any()) } returns requestBytes
        
        coEvery { networkClient.sendAndReceive(any(), requestBytes) } returns Result.success(responseBytes)
        every { codec.decode(responseBytes) } returns responseJson

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.OK, result.status)
        assertEquals(0, result.resultCode)
        assertEquals("Success", result.resultText)
        assertEquals(124L, result.responseToken)
        assertEquals(11, result.responseReqNum)
        assertEquals("12345678", result.fiscalSign)
        assertEquals("http://ofd.kz/t/123", result.receiptUrl)
        assertNull(result.errorMessage)
    }

    @Test
    fun testSendInvalidEndpoint() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "INVALID_PROVIDER",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun testSendNoStrategyFound() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        every { builder.canHandle(OfdCommandType.TICKET) } returns false

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun testSendTimeoutException() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns JsonObject(emptyMap())
        every { codec.encode(any()) } returns byteArrayOf(1)
        
        coEvery { networkClient.sendAndReceive(any(), any()) } coAnswers {
            kotlinx.coroutines.withTimeout(0L) { kotlinx.coroutines.yield() }
            Result.failure<ByteArray>(RuntimeException("Timeout"))
        }

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.TIMEOUT, result.status)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun testSendNetworkFailureTimeout() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns JsonObject(emptyMap())
        every { codec.encode(any()) } returns byteArrayOf(1)
        
        coEvery { networkClient.sendAndReceive(any(), any()) } returns Result.failure(RuntimeException("read timeout"))

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.TIMEOUT, result.status)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun testSendNetworkFailureGeneral() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns JsonObject(emptyMap())
        every { codec.encode(any()) } returns byteArrayOf(1)
        
        coEvery { networkClient.sendAndReceive(any(), any()) } returns Result.failure<ByteArray>(RuntimeException("Connection refused"))

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun testSendNetworkFailureIOException() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns JsonObject(emptyMap())
        every { codec.encode(any()) } throws java.io.IOException()

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun testSendNetworkFailureWrappedIOException() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns JsonObject(emptyMap())
        every { codec.encode(any()) } throws RuntimeException(java.io.IOException("Wrapped error"))

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun testSendNetworkFailureTimeoutException() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns JsonObject(emptyMap())
        every { codec.encode(any()) } throws java.util.concurrent.TimeoutException("Timeout")

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun testSendNetworkFailureNestedRuntimeException() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns JsonObject(emptyMap())
        every { codec.encode(any()) } throws RuntimeException(RuntimeException("not io exception"))

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun testSendEmptyResponse() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns JsonObject(emptyMap())
        every { codec.encode(any()) } returns byteArrayOf(1)
        
        coEvery { networkClient.sendAndReceive(any(), any()) } returns Result.success(byteArrayOf())

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun testSendProtocolException() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns JsonObject(emptyMap())
        every { codec.encode(any()) } throws OfdProtocolException("Invalid protocol header")

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertEquals("Invalid protocol header", result.errorMessage)
    }

    @Test
    fun testSendUnexpectedException() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } throws RuntimeException("Fatal memory out")

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun testSendThrottlingActive() {
        val request = OfdCommandRequest(
            kkmId = "kkm-2",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns JsonObject(emptyMap())
        every { codec.encode(any()) } returns byteArrayOf(1)
        
        // First request fails and sets lastNoConnectionMillis
        coEvery { networkClient.sendAndReceive(any(), any()) } returns Result.failure<ByteArray>(RuntimeException("Connection refused"))
        val firstResult = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, firstResult.status)

        // Second request triggers throttle immediately because it is within 60s
        val secondResult = adapter.send(request)
        assertEquals(OfdCommandStatus.TIMEOUT, secondResult.status)
        assertNotNull(secondResult.errorMessage)
    }

    @Test
    fun testDefaultRequestBuilders() {
        val builders = OfdManagerAdapter.defaultRequestBuilders()
        assertEquals(2, builders.size)
    }

    @Test
    fun testSendFailedResultCode() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        val requestBytes = byteArrayOf(1, 2)
        val responseBytes = byteArrayOf(3, 4)
        
        val responseJson = Json.parseToJsonElement(
            """{
                "header": {"token": 124, "reqNum": 11},
                "payload": {
                    "result": {
                        "resultCode": 1,
                        "resultText": "Database error"
                    }
                }
            }"""
        ) as JsonObject

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns responseJson
        every { codec.encode(any()) } returns requestBytes
        
        coEvery { networkClient.sendAndReceive(any(), requestBytes) } returns Result.success(responseBytes)
        every { codec.decode(responseBytes) } returns responseJson

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertEquals(1, result.resultCode)
        assertEquals("Database error", result.resultText)
    }

    @Test
    fun testSendNullResultCode() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        val requestBytes = byteArrayOf(1, 2)
        val responseBytes = byteArrayOf(3, 4)
        
        // Missing payload/result entirely
        val responseJson = Json.parseToJsonElement(
            """{
                "header": {"token": 124, "reqNum": 11}
            }"""
        ) as JsonObject

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns responseJson
        every { codec.encode(any()) } returns requestBytes
        
        coEvery { networkClient.sendAndReceive(any(), requestBytes) } returns Result.success(responseBytes)
        every { codec.decode(responseBytes) } returns responseJson

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertNull(result.resultCode)
    }

    @Test
    fun testSendEndpointMissingFromProvider() {
        // "DEV" is a valid environment, but KAZAKHTELECOM has no endpoint for it, returning null
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "DEV",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )
        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertTrue(result.errorMessage!!.contains("provider or endpoint not found"))
    }

    @Test
    fun testSendBuilderReturnsNull() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )
        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns null // Builder returns null!
        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertEquals(DataErrorMessages.ofdRequestFailed("Missing required request parameters"), result.errorMessage)
    }

    @Test
    fun testSendMissingHeaderAndPayloadDetails() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )
        val requestBytes = byteArrayOf(1, 2)
        val responseBytes = byteArrayOf(3, 4)

        // Missing header completely, payload result exists but resultCode is null
        val responseJson = Json.parseToJsonElement(
            """{
                "payload": {
                    "result": {}
                }
            }"""
        ) as JsonObject

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns responseJson
        every { codec.encode(any()) } returns requestBytes
        
        coEvery { networkClient.sendAndReceive(any(), requestBytes) } returns Result.success(responseBytes)
        every { codec.decode(responseBytes) } returns responseJson

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertNull(result.responseToken)
        assertNull(result.responseReqNum)
    }

    @Test
    fun testSendNonNumericHeaderAndPayloadDetails() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )
        val requestBytes = byteArrayOf(1, 2)
        val responseBytes = byteArrayOf(3, 4)

        // "token" and "reqNum" are non-numeric string values, resulting in null parse
        val responseJson = Json.parseToJsonElement(
            """{
                "header": {"token": "abc", "reqNum": "def"}
            }"""
        ) as JsonObject

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns responseJson
        every { codec.encode(any()) } returns requestBytes
        
        coEvery { networkClient.sendAndReceive(any(), requestBytes) } returns Result.success(responseBytes)
        every { codec.decode(responseBytes) } returns responseJson

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertNull(result.responseToken)
        assertNull(result.responseReqNum)
    }

    @Test
    fun testSendFailureNullMessage() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )
        val requestBytes = byteArrayOf(1, 2)
        
        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns JsonObject(emptyMap())
        every { codec.encode(any()) } returns requestBytes
        
        // Return a failure exception with null message
        coEvery { networkClient.sendAndReceive(any(), requestBytes) } returns Result.failure(RuntimeException())

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertEquals(DataErrorMessages.ofdRequestFailed("unknown"), result.errorMessage)
    }

    @Test
    fun testSendUnexpectedExceptionNullMessage() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )
        
        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns JsonObject(emptyMap())
        // codec.encode throws RuntimeException with null message
        every { codec.encode(any()) } throws RuntimeException()

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertEquals(DataErrorMessages.ofdRequestFailed(null), result.errorMessage)
    }



    @Test
    fun testCoerceReconnectInterval() {
        val adapterWithLargeInterval = OfdManagerAdapter(
            config = config,
            codec = codec,
            networkClient = networkClient,
            requestBuilders = listOf(builder),
            timeoutSeconds = 1L,
            reconnectIntervalSeconds = 120L // larger than 60
        )
        assertNotNull(adapterWithLargeInterval)
    }

    @Test
    fun testSendInvalidEnvironment() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "INVALID_ENV", // Invalid environment!
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )
        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertTrue(result.errorMessage!!.contains("provider or endpoint not found"))
    }

    @Test
    fun testSendVariousMalformedHeadersAndPayloads() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )
        val requestBytes = byteArrayOf(1, 2)
        val responseBytes = byteArrayOf(3, 4)

        val malformedJsons = listOf(
            // 1. Missing reqNum inside header
            """{"header": {"token": 124}}""",
            // 2. Missing token inside header
            """{"header": {"reqNum": 11}}""",
            // 3. result is null inside payload
            """{"header": {"token": 124, "reqNum": 11}, "payload": {"result": null}}""",
            // 4. result is non-object inside payload
            """{"header": {"token": 124, "reqNum": 11}, "payload": {"result": 123}}""",
            // 5. result is missing entirely inside payload
            """{"header": {"token": 124, "reqNum": 11}, "payload": {}}"""
        )

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(any(), config) } returns JsonObject(emptyMap())
        every { codec.encode(any()) } returns requestBytes

        for ((idx, jsonStr) in malformedJsons.withIndex()) {
            val iterRequest = request.copy(kkmId = "kkm-malformed-$idx")
            val responseJson = Json.parseToJsonElement(jsonStr) as JsonObject
            coEvery { networkClient.sendAndReceive(any(), requestBytes) } returns Result.success(responseBytes)
            every { codec.decode(responseBytes) } returns responseJson

            val result = adapter.send(iterRequest)
            assertEquals(OfdCommandStatus.FAILED, result.status)
        }
    }

    @Test
    fun testConstructorDefaultArguments() {
        val adapterWithDefaults = OfdManagerAdapter(
            config = config,
            codec = codec,
            networkClient = networkClient,
            requestBuilders = listOf(builder)
        )
        assertNotNull(adapterWithDefaults)
    }

    @Test
    fun testSendCaseInsensitiveEnvironment() {
        // Use lowercase "prod" to test case-insensitive comparison inside resolveEndpoint
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "prod",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        val requestBytes = byteArrayOf(1, 2)
        val responseBytes = byteArrayOf(3, 4)
        val responseJson = Json.parseToJsonElement(
            """{"header": {"token": 124, "reqNum": 11}}"""
        ) as JsonObject

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns responseJson
        every { codec.encode(any()) } returns requestBytes
        
        coEvery { networkClient.sendAndReceive(any(), requestBytes) } returns Result.success(responseBytes)
        every { codec.decode(responseBytes) } returns responseJson

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertEquals(124L, result.responseToken)
        assertEquals(11, result.responseReqNum)
    }

    @Test
    fun testSendMultipleBuildersSkipFirst() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        val requestBytes = byteArrayOf(1, 2)
        val responseBytes = byteArrayOf(3, 4)
        val responseJson = Json.parseToJsonElement(
            """{"header": {"token": 124, "reqNum": 11}}"""
        ) as JsonObject

        val firstBuilder = mockk<OfdRequestBuilderStrategy>()
        every { firstBuilder.canHandle(OfdCommandType.TICKET) } returns false // returns false to skip

        val secondBuilder = mockk<OfdRequestBuilderStrategy>()
        every { secondBuilder.canHandle(OfdCommandType.TICKET) } returns true
        every { secondBuilder.build(request, config) } returns responseJson

        val customAdapter = OfdManagerAdapter(
            config = config,
            codec = codec,
            networkClient = networkClient,
            requestBuilders = listOf(firstBuilder, secondBuilder),
            timeoutSeconds = 1L,
            reconnectIntervalSeconds = 1L
        )

        every { codec.encode(any()) } returns requestBytes
        coEvery { networkClient.sendAndReceive(any(), requestBytes) } returns Result.success(responseBytes)
        every { codec.decode(responseBytes) } returns responseJson

        val result = customAdapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
    }

    @Test
    fun testSendHeaderTokenAndReqNumAsObjects() {
        val request = OfdCommandRequest(
            kkmId = "kkm-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )
        val requestBytes = byteArrayOf(1, 2)
        val responseBytes = byteArrayOf(3, 4)

        // token and reqNum are objects, which makes jsonPrimitive return null/throw-safe
        val responseJson = Json.parseToJsonElement(
            """{
                "header": {
                    "token": {},
                    "reqNum": {}
                }
            }"""
        ) as JsonObject

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns responseJson
        every { codec.encode(any()) } returns requestBytes
        
        coEvery { networkClient.sendAndReceive(any(), requestBytes) } returns Result.success(responseBytes)
        every { codec.decode(responseBytes) } returns responseJson

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertNull(result.responseToken)
        assertNull(result.responseReqNum)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testSendThrottlingExpired() {
        val request = OfdCommandRequest(
            kkmId = "kkm-throttle-expired",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "PROD",
            commandType = OfdCommandType.TICKET,
            payloadRef = "doc-1",
            token = 123L,
            reqNum = 10,
            deviceId = 1L
        )

        val requestBytes = byteArrayOf(1, 2)
        val responseBytes = byteArrayOf(3, 4)
        val responseJson = Json.parseToJsonElement(
            """{"header": {"token": 124, "reqNum": 11}}"""
        ) as JsonObject

        every { builder.canHandle(OfdCommandType.TICKET) } returns true
        every { builder.build(request, config) } returns responseJson
        every { codec.encode(any()) } returns requestBytes

        // Manually populate the private map with a timestamp from 70 seconds ago
        val field = OfdManagerAdapter::class.java.getDeclaredField("lastNoConnectionMillis")
        field.isAccessible = true
        val map = field.get(adapter) as java.util.concurrent.ConcurrentHashMap<String, Long>
        val throttleKey = "${request.kkmId}:${request.ofdProviderId}:${request.ofdEnvironmentId}"
        map[throttleKey] = System.currentTimeMillis() - 70000L

        // The request should bypass throttle and try sending again
        coEvery { networkClient.sendAndReceive(any(), requestBytes) } returns Result.success(responseBytes)
        every { codec.decode(responseBytes) } returns responseJson

        val result = adapter.send(request)
        assertEquals(OfdCommandStatus.FAILED, result.status)
        assertEquals(124L, result.responseToken)
    }
}
