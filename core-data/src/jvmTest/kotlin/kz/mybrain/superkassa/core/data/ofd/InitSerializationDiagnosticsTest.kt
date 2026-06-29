package kz.mybrain.superkassa.core.data.ofd

import kotlin.test.Test
import kotlin.test.assertTrue
import kz.mybrain.superkassa.core.data.ofd.builder.strategy.ServiceRequestBuilderStrategy
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandRequest
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.ofd.OfdServiceInfo

class InitSerializationDiagnosticsTest {

    @Test
    fun `command system payload serializes for init`() {
        val command = baseServiceCommand(OfdCommandType.SYSTEM)
        val json = ServiceRequestBuilderStrategy().build(command, OfdConfig("203"))
            ?: error("Service request json is null")
        val bytes = OfdProtocolCodec().encode(json)
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun `command info payload serializes for init`() {
        val command = baseServiceCommand(OfdCommandType.INFO)
        val json = ServiceRequestBuilderStrategy().build(command, OfdConfig("203"))
            ?: error("Service request json is null")
        val bytes = OfdProtocolCodec().encode(json)
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun `probe reqNum`() {
        val codec = OfdProtocolCodec()
        val strategy = ServiceRequestBuilderStrategy()
        val resultsFile = java.io.File("build/probe_results.txt")
        resultsFile.parentFile.mkdirs()
        resultsFile.writeText("PARALLEL PROBE START\n")
        
        val tokens = listOf(1894042328L)
        val reqNums = 1..15
        
        val tasks = mutableListOf<java.util.concurrent.Future<*>>()
        val executor = java.util.concurrent.Executors.newFixedThreadPool(32)
        
        val successFound = java.util.concurrent.atomic.AtomicBoolean(false)
        
        for (t in tokens) {
            for (r in reqNums) {
                tasks.add(executor.submit {
                    if (successFound.get()) return@submit
                    
                    val command = OfdCommandRequest(
                        kkmId = "probe-kkm",
                        commandType = OfdCommandType.SYSTEM,
                        payloadRef = "payload-probe",
                        ofdProviderId = "kazakhtelecom",
                        ofdEnvironmentId = "test",
                        deviceId = 203539L,
                        token = t,
                        reqNum = r,
                        registrationNumber = "TEMP_REG_12345678",
                        factoryNumber = "TEMP_FACTORY_12345678",
                        ofdSystemId = "203539",
                        serviceInfo = OfdServiceInfo(
                            orgTitle = "UNKNOWN",
                            orgAddress = "UNKNOWN",
                            orgAddressKz = "UNKNOWN",
                            orgInn = "000000000000",
                            orgOkved = "00000",
                            geoLatitude = 0,
                            geoLongitude = 0,
                            geoSource = "UNKNOWN"
                        ),
                        offlineBeginMillis = System.currentTimeMillis(),
                        offlineEndMillis = System.currentTimeMillis()
                    )
                    
                    val json = strategy.build(command, OfdConfig("203")) ?: return@submit
                    val requestBytes = codec.encode(json)
                    
                    try {
                        val socket = java.net.Socket("37.150.215.187", 7777)
                        socket.soTimeout = 1500
                        val out = socket.getOutputStream()
                        out.write(requestBytes)
                        out.flush()
                        
                        val input = socket.getInputStream()
                        val buffer = ByteArray(4096)
                        var totalRead = 0
                        try {
                            while (totalRead < buffer.size) {
                                val n = input.read(buffer, totalRead, buffer.size - totalRead)
                                if (n < 0) break
                                totalRead += n
                            }
                        } catch (e: java.io.InterruptedIOException) {
                        }
                        
                        if (totalRead > 0) {
                            val responseBytes = buffer.copyOf(totalRead)
                            val responseJson = codec.decode(responseBytes)
                            val payload = responseJson["payload"] as? kotlinx.serialization.json.JsonObject
                            val result = payload?.get("result") as? kotlinx.serialization.json.JsonObject
                            val resultCode = result?.get("resultCode")?.toString()?.toIntOrNull()
                            val resultText = result?.get("resultText")?.toString()
                            
                            synchronized(resultsFile) {
                                resultsFile.appendText("PROBE token=$t, reqNum=$r: resultCode=$resultCode, text=$resultText, response=$responseJson\n")
                                if (resultCode == 0) {
                                    resultsFile.appendText("SUCCESS! FOUND WORKING token=$t, reqNum=$r\n")
                                    successFound.set(true)
                                }
                            }
                        }
                        socket.close()
                    } catch (e: Exception) {
                        // ignore network/socket connection errors
                    }
                })
            }
        }
        
        executor.shutdown()
        try {
            executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: Exception) {}
        executor.shutdownNow()
    }

    private fun baseServiceCommand(type: OfdCommandType): OfdCommandRequest {
        return OfdCommandRequest(
            kkmId = "kkm-test",
            commandType = type,
            payloadRef = "payload-1",
            ofdProviderId = "KAZAKHTELECOM",
            ofdEnvironmentId = "TEST",
            deviceId = 201873L,
            token = 208627316L,
            reqNum = 1,
            registrationNumber = "TEMP_REG_12345678",
            factoryNumber = "TEMP_FACTORY_12345678",
            ofdSystemId = "201873",
            serviceInfo = OfdServiceInfo(
                orgTitle = "UNKNOWN",
                orgAddress = "UNKNOWN",
                orgAddressKz = "UNKNOWN",
                orgInn = "000000000000",
                orgOkved = "00000",
                geoLatitude = 0,
                geoLongitude = 0,
                geoSource = "UNKNOWN"
            ),
            offlineBeginMillis = 1_700_000_000_000L,
            offlineEndMillis = 1_700_000_000_000L
        )
    }
}
