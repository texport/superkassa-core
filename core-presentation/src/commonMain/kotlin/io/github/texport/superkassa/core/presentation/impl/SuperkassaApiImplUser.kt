package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.model.auth.UserRole as DomainUserRole
import io.github.texport.superkassa.core.presentation.api.model.user.*
import io.github.texport.superkassa.core.presentation.impl.mapper.UserMapper

fun SuperkassaApiImpl.listUsersImpl(kkmId: String, pin: String): List<UserResponse> {
    authorization.requireKkm(kkmId)
    authorization.requireRole(kkmId, pin, setOf(DomainUserRole.ADMIN), allowDefaultPin = true)
    return storage.listUsers(kkmId).map { UserMapper.toResponse(it) }
}

fun SuperkassaApiImpl.createUserImpl(kkmId: String, pin: String, request: UserCreateRequest): UserResponse {
    val user = createUserUseCase.execute(
        kkmId,
        pin,
        request.name,
        DomainUserRole.valueOf(request.role.name),
        request.userPin
    )
    return UserMapper.toResponse(user)
}

fun SuperkassaApiImpl.updateUserImpl(
    kkmId: String,
    userId: String,
    pin: String,
    request: UserUpdateRequest
): UserResponse {
    val user = updateUserUseCase.execute(
        kkmId,
        userId,
        pin,
        request.name,
        request.role?.let { DomainUserRole.valueOf(it.name) },
        request.userPin
    )
    return UserMapper.toResponse(user)
}

fun SuperkassaApiImpl.deleteUserImpl(kkmId: String, userId: String, pin: String): Boolean {
    deleteUserUseCase.execute(kkmId, userId, pin)
    return true
}
