package kz.mybrain.superkassa.core.domain.model.ofd

actual object DefaultOfdProvidersRegistry {
    private val providers = mutableMapOf<String, OfdProviderConfig>()

    init {
        loadHardcodedDefaults()
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
