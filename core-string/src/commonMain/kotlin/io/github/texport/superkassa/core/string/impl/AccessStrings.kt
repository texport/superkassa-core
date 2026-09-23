package io.github.texport.superkassa.core.string.impl

import io.github.texport.superkassa.core.string.api.TrilingualMessage

/** Отказы по пину — при входе и при выборе нового — и границы выборок: страница и срок. */
internal object AccessStrings {
    private const val SECONDS_IN_MINUTE = 60L

    /** Касса заперта после неверных пинов; время до конца блокировки — с округлением вверх. */
    fun pinLocked(retryAfterSeconds: Long): TrilingualMessage {
        val minutes = (retryAfterSeconds + SECONDS_IN_MINUTE - 1) / SECONDS_IN_MINUTE
        val wait = if (retryAfterSeconds < SECONDS_IN_MINUTE) {
            TrilingualMessage(
                ru = "$retryAfterSeconds с",
                kk = "$retryAfterSeconds секундтан",
                en = if (retryAfterSeconds == 1L) "1 second" else "$retryAfterSeconds seconds"
            )
        } else {
            TrilingualMessage(
                ru = "$minutes мин",
                kk = "$minutes минуттан",
                en = if (minutes == 1L) "1 minute" else "$minutes minutes"
            )
        }
        return TrilingualMessage(
            ru = "Слишком много неверных ПИН-кодов. Касса заблокирована, повторите через ${wait.ru}.",
            kk = "Қате ПИН-код тым көп рет енгізілді. Касса бұғатталды, ${wait.kk} кейін қайталаңыз.",
            en = "Too many wrong PINs. The cash register is locked, try again in ${wait.en}."
        )
    }

    /** Пин короче или длиннее допустимого; границы входят в допустимое. */
    fun pinLengthInvalid(shortest: Int, longest: Int): TrilingualMessage = TrilingualMessage(
        ru = "ПИН-код должен содержать от $shortest до $longest символов.",
        kk = "ПИН-код ұзындығы $shortest мен $longest таңба аралығында болуы керек.",
        en = "PIN must be $shortest to $longest characters long."
    )

    /** Касса заводится без пина администратора: пина по умолчанию у неё нет. */
    fun kkmAdminPinRequired(): TrilingualMessage = TrilingualMessage(
        ru = "Задайте ПИН-код администратора новой кассы: без него в кассу не войти.",
        kk = "Жаңа кассаның әкімші ПИН-кодын енгізіңіз: онсыз кассаға кіру мүмкін емес.",
        en = "Set the administrator PIN for the new cash register: without it no one can sign in."
    )

    fun pageLimitOutOfRange(largest: Int): TrilingualMessage = TrilingualMessage(
        ru = "Количество записей за один запрос должно быть от 1 до $largest.",
        kk = "Бір сұраныстағы жазбалар саны 1 мен $largest аралығында болуы керек.",
        en = "Number of records per request must be between 1 and $largest."
    )

    fun pageOffsetNegative(): TrilingualMessage = TrilingualMessage(
        ru = "Смещение выборки не может быть отрицательным.",
        kk = "Таңдау ығысуы теріс бола алмайды.",
        en = "Offset cannot be negative."
    )

    fun periodInvalid(): TrilingualMessage = TrilingualMessage(
        ru = "Неверный период: начало должно быть раньше конца и не раньше 1 января 1970 года.",
        kk = "Кезең қате: басы соңынан ерте және 1970 жылғы 1 қаңтардан кейін болуы керек.",
        en = "Invalid period: start must precede end and be no earlier than 1 January 1970."
    )
}
