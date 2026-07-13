package io.github.texport.superkassa.core.presentation.api.model.auth

import kotlinx.serialization.Serializable

@Serializable
enum class AuthMode {
    NONE,
    BEARER
}
