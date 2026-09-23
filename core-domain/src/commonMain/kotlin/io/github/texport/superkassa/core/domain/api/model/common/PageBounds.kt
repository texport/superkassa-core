package io.github.texport.superkassa.core.domain.api.model.common

import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.string.api.CoreStrings

/**
 * Границы выборки: страница и срок.
 *
 * Проверяются на входе, а не подрезаются молча: отрицательный лимит
 * база понимала как «без лимита» и отдавала весь объём, отрицательное
 * смещение роняло хранилище в памяти, а перевёрнутый срок давал пустой
 * ответ, неотличимый от «документов нет».
 */
object PageBounds {
    /** Больше документов и смен за один запрос не отдаётся. */
    const val LARGEST_DOCUMENT_PAGE: Int = 500

    /** Больше касс за один запрос не отдаётся. */
    const val LARGEST_KKM_PAGE: Int = 1000

    /**
     * @throws ValidationException если [limit] вне `1..largest` или [offset] отрицательно.
     */
    fun requirePage(limit: Int, offset: Int, largest: Int = LARGEST_DOCUMENT_PAGE) {
        requireLimit(limit, largest)
        if (offset < 0) throw ValidationException(CoreStrings.pageOffsetNegative(), "PAGE_OFFSET_NEGATIVE")
    }

    /**
     * @throws ValidationException если [limit] вне `1..largest`.
     */
    fun requireLimit(limit: Int, largest: Int = LARGEST_DOCUMENT_PAGE) {
        if (limit !in 1..largest) {
            throw ValidationException(CoreStrings.pageLimitOutOfRange(largest), "PAGE_LIMIT_OUT_OF_RANGE")
        }
    }

    /**
     * Срок `[fromInclusive, toExclusive)` в epoch ms: не раньше эпохи и не пустой.
     *
     * @throws ValidationException если начало отрицательно или не раньше конца.
     */
    fun requirePeriod(fromInclusive: Long, toExclusive: Long) {
        if (fromInclusive < 0 || fromInclusive >= toExclusive) {
            throw ValidationException(CoreStrings.periodInvalid(), "PERIOD_INVALID")
        }
    }
}
