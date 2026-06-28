package kz.mybrain.superkassa.core.domain.model.ofd

import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.Json

actual object DefaultOfdProvidersRegistry {
    private val providers = ConcurrentHashMap<String, OfdProviderConfig>()

    init {
        try {
            val jsonStream = DefaultOfdProvidersRegistry::class.java.getResourceAsStream("/ofd-providers.json")
            if (jsonStream != null) {
                val jsonText = jsonStream.bufferedReader().use { it.readText() }
                val loaded = Json.decodeFromString<Map<String, OfdProviderConfig>>(jsonText)
                loaded.forEach { (k, v) -> providers[k.uppercase()] = v }
            } else {
                loadHardcodedDefaults()
            }
        } catch (_: Exception) {
            loadHardcodedDefaults()
        }
    }

    private fun loadHardcodedDefaults() {
        providers["KAZAKHTELECOM"] = OfdProviderConfig(
            nameRu = "АО «Казахтелеком»",
            nameKk = "«Қазақтелеком» АҚ",
            website = "oofd.kz",
            checkDomain = "consumer.oofd.kz"
        )
        providers["TRANSTELECOM"] = OfdProviderConfig(
            nameRu = "АО «Транстелеком»",
            nameKk = "«Транстелеком» АҚ",
            website = "o.oofd.kz",
            checkDomain = "o.oofd.kz"
        )
        providers["ALTECO"] = OfdProviderConfig(
            nameRu = "ТОО «Alteco Partners»",
            nameKk = "«Alteco Partners» ЖШС",
            website = "alteco.kz",
            checkDomain = "alteco.kz"
        )
    }

    actual val defaultOfdProviders: Map<String, OfdProviderConfig>
        get() = providers

    actual fun registerProvider(key: String, config: OfdProviderConfig) {
        providers[key.uppercase()] = config
    }
}
