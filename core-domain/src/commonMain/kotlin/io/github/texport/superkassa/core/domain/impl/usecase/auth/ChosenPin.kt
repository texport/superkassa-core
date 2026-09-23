package io.github.texport.superkassa.core.domain.impl.usecase.auth

import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.string.api.CoreStrings

/**
 * Правила пина, который пользователь задаёт сам: администратору при
 * заведении кассы, новому пользователю и при смене пина.
 *
 * Пинов по умолчанию у кассы нет, поэтому нет и особых пинов: любой пин,
 * прошедший эти правила, — обычный пин пользователя. Вход по пину этих
 * правил не спрашивает: заданный прежде пин пускает и дальше.
 */
object ChosenPin {
    const val SHORTEST = 4
    const val LONGEST = 10

    /**
     * Проверяет пин, который пользователь выбрал себе.
     *
     * @param pin Выбранный пин.
     * @throws ValidationException `USER_PIN_REQUIRED`, если пин пуст;
     * `USER_PIN_LENGTH`, если он короче [SHORTEST] или длиннее [LONGEST] символов.
     */
    fun require(pin: String) {
        if (pin.isBlank()) {
            throw ValidationException(CoreStrings.userPinRequired(), "USER_PIN_REQUIRED")
        }
        if (pin.length !in SHORTEST..LONGEST) {
            throw ValidationException(CoreStrings.pinLengthInvalid(SHORTEST, LONGEST), "USER_PIN_LENGTH")
        }
    }
}
