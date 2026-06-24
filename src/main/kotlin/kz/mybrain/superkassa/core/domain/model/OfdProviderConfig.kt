package kz.mybrain.superkassa.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class OfdProviderConfig(
    val nameRu: String,
    val nameKk: String,
    val website: String,
    val checkDomain: String
)
