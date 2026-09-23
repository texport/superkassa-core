package io.github.texport.superkassa.core.presentation.api.model

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmInitSimpleRequest
import io.github.texport.superkassa.core.presentation.api.model.kkm.OfdServiceInfoResponse
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * БИН/ИИН и ОКЭД называются по-казахстански, а прежние имена полей
 * (`orgInn`, `orgOkved`, `okved`) принимаются при чтении: узел хранит
 * сведения кассы этим видом, и приложения шлют заведение кассы по-старому.
 */
class TaxpayerFieldNamesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `сведения кассы, записанные узлом под прежними именами, читаются`() {
        val info = json.decodeFromString(OfdServiceInfoResponse.serializer(), LEGACY_SERVICE_INFO)

        assertEquals("060140012345" to "47111", info.orgIinOrBin to info.orgOked)
    }

    @Test
    fun `сведения кассы пишутся под именами БИН-ИИН и ОКЭД`() {
        val info = json.decodeFromString(OfdServiceInfoResponse.serializer(), LEGACY_SERVICE_INFO)

        val written = json.encodeToString(OfdServiceInfoResponse.serializer(), info)

        assertTrue("\"orgIinOrBin\":\"060140012345\"" in written && "\"orgOked\":\"47111\"" in written, written)
    }

    @Test
    fun `ОКЭД заведения кассы принимается и под прежним именем`() {
        val request = json.decodeFromString(
            KkmInitSimpleRequest.serializer(),
            """{"ofdId":"KAZAKHTELECOM","ofdEnvironment":"TEST","ofdSystemId":"4100","ofdToken":"1","okved":"47111"}"""
        )

        assertEquals("47111", request.oked)
    }

    private companion object {
        const val LEGACY_SERVICE_INFO = """{"orgTitle":"ТОО Дала","orgAddress":"Алматы","orgAddressKz":"Алматы",""" +
            """"orgInn":"060140012345","orgOkved":"47111","geoLatitude":1,"geoLongitude":2,"geoSource":"MANUAL"}"""
    }
}
