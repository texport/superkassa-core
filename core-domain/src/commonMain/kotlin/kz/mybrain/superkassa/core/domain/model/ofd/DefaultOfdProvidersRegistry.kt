package kz.mybrain.superkassa.core.domain.model.ofd

expect object DefaultOfdProvidersRegistry {
    val defaultOfdProviders: Map<String, OfdProviderConfig>
    fun registerProvider(key: String, config: OfdProviderConfig)
}
