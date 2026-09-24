package io.github.texport.superkassa.core.domain.impl.helper.ofd

import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.string.api.CoreStrings
import kotlin.test.Test
import kotlin.test.assertEquals

/** Кассиру — причина словами; сетевой текст обмена в неё не попадает. */
class BfdFailureReasonTest {

    @Test
    fun `отказ с кодом - причина этого кода`() {
        val refused = OfdCommandResult(status = OfdCommandStatus.FAILED, resultCode = 13, resultText = "Bad data")

        assertEquals(CoreStrings.bfdRefusal(13), bfdFailureReason(refused))
    }

    @Test
    fun `без ответа - проверить связь, а не текст сетевой ошибки`() {
        val silent = OfdCommandResult(status = OfdCommandStatus.TIMEOUT, errorMessage = CoreStrings.ofdRequestFailedData("BFD response timeout"))

        assertEquals(CoreStrings.bfdNoAnswer(), bfdFailureReason(silent))
    }

    @Test
    fun `запрос не ушёл - проверить настройки`() {
        val unsent = OfdCommandResult(status = OfdCommandStatus.FAILED, resultCode = 0, errorMessage = "Missing required request parameters")

        assertEquals(CoreStrings.bfdRequestNotSent(), bfdFailureReason(unsent))
    }
}
