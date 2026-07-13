package io.github.texport.superkassa.core.domain.api.model.common

/**
 * Области видимости счетчиков ККМ.
 */
object CounterScopes {
    /** Глобальные счетчики (накапливаемый итог за все время работы ККМ). */
    const val GLOBAL = "GLOBAL"

    /** Счетчики текущей смены ККМ (сбрасываются при закрытии смены). */
    const val SHIFT = "SHIFT"
}
