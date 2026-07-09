package io.github.texport.superkassa.core.domain.usecase.kkm

import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.exception.ValidationException
import io.github.texport.superkassa.core.domain.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.helper.KkmCommonHelper

/**
 * Сценарий (Use Case) проверки рабочего состояния ККМ.
 *
 * Проверяет корректность системного времени, блокировки кассы, режима программирования
 * и лимитов автономной работы.
 */
class RequireOperationalUseCase(
    private val kkmCommonHelper: KkmCommonHelper,
    private val enforceAutonomousLimitsUseCase: EnforceAutonomousLimitsUseCase
) {
    /**
     * Выполняет валидацию рабочего состояния кассы.
     *
     * @param kkm Данные кассы.
     * @throws ValidationException если касса заблокирована, находится в режиме программирования или системное время некорректно.
     */
    fun execute(kkm: KkmInfo) {
        kkmCommonHelper.ensureSystemTimeValid()
        if (kkm.state == KkmState.BLOCKED.name) {
            throw ValidationException(CoreStrings.kkmBlocked(), "KKM_BLOCKED")
        }
        if (kkm.state == KkmState.PROGRAMMING.name) {
            throw ValidationException(CoreStrings.kkmInProgramming(), "KKM_IN_PROGRAMMING")
        }
        enforceAutonomousLimitsUseCase.execute(kkm)
    }
}
