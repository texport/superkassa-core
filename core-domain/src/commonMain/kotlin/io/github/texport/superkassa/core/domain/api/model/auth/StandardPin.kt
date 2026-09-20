package io.github.texport.superkassa.core.domain.api.model.auth

/**
 * Стандартные ПИН-коды, с которыми касса поставляется.
 *
 * Узел не пускает по ним в рабочие сценарии, поэтому касса, заведённая
 * с таким пином администратора, остаётся недоступной: сменить пин можно
 * только войдя, а войти нельзя.
 */
object StandardPin {
    /** Пин, которым подтверждается заведение первой кассы на узле. */
    const val BOOTSTRAP = "0000"

    private val known = setOf(BOOTSTRAP, "1111")

    /**
     * Проверяет, является ли ПИН-код стандартным.
     *
     * @param pin Проверяемый ПИН-код.
     * @return `true`, если по этому пину работать нельзя.
     */
    fun isStandard(pin: String): Boolean = pin in known
}
