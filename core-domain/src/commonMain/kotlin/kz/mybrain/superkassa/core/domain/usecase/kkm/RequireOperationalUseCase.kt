package kz.mybrain.superkassa.core.domain.usecase.kkm

import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper

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
            throw ValidationException(ErrorMessages.kkmBlocked(), "KKM_BLOCKED")
        }
        if (kkm.state == KkmState.PROGRAMMING.name) {
            throw ValidationException(ErrorMessages.kkmInProgramming(), "KKM_IN_PROGRAMMING")
        }
        enforceAutonomousLimitsUseCase.execute(kkm)
    }
}
