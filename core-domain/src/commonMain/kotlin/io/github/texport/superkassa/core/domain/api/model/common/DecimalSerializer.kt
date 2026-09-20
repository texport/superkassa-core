package io.github.texport.superkassa.core.domain.api.model.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral

/**
 * [Decimal] в JSON — числом, как его записал отправитель.
 *
 * Кассы и рабочее место присылают суммы числом: `"sum": 150.55`. Разбор идёт
 * из записи, а не из `Double`, — иначе точность теряется ещё до домена.
 * Обратно число уходит той же записью, без хвоста `.0` и без экспоненты.
 */
object DecimalSerializer : KSerializer<Decimal> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Decimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Decimal {
        val json = decoder as? JsonDecoder ?: return Decimal.parse(decoder.decodeString())
        return Decimal.parse(json.decodeJsonElement().let { it as JsonPrimitive }.content)
    }

    override fun serialize(encoder: Encoder, value: Decimal) {
        val json = encoder as? JsonEncoder ?: return encoder.encodeString(value.toString())
        json.encodeJsonElement(JsonUnquotedLiteral(value.toString()))
    }
}
