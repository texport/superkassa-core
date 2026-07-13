package io.github.texport.superkassa.core.presentation.impl.mapper

import io.github.texport.superkassa.core.domain.api.model.auth.KkmUser
import io.github.texport.superkassa.core.presentation.api.model.user.*

object UserMapper {

    fun toResponse(user: KkmUser): UserResponse = UserResponse(
        userId = user.id,
        name = user.name,
        role = UserRole.valueOf(user.role.name),
        pin = user.pin
    )
}
