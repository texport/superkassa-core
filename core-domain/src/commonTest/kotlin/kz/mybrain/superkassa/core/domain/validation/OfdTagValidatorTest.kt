package kz.mybrain.superkassa.core.domain.validation

import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OfdTagValidatorTest {

    @Test
    fun testValidateAndFormatTagSuccess() {
        val tag = OfdTagValidator.validateAndFormatTag("KAZAKHTELECOM", "PROD")
        assertEquals("KAZAKHTELECOM:PROD", tag)
    }

    @Test
    fun testValidateAndFormatTagUnknownProvider() {
        val ex = assertFailsWith<ValidationException> {
            OfdTagValidator.validateAndFormatTag("INVALID_PROVIDER", "PROD")
        }
        assertEquals("OFD_PROVIDER_UNKNOWN", ex.code)
    }

    @Test
    fun testValidateAndFormatTagUnknownEnvironment() {
        val ex = assertFailsWith<ValidationException> {
            OfdTagValidator.validateAndFormatTag("KAZAKHTELECOM", "INVALID_ENV")
        }
        assertEquals("OFD_ENVIRONMENT_UNKNOWN", ex.code)
    }

    @Test
    fun testParseTagSuccess() {
        val (provider, env) = OfdTagValidator.parseTag("KAZAKHTELECOM:PROD")
        assertEquals("KAZAKHTELECOM", provider)
        assertEquals("PROD", env)
    }

    @Test
    fun testParseTagInvalidFormat() {
        val ex = assertFailsWith<ValidationException> {
            OfdTagValidator.parseTag("INVALIDTAG")
        }
        assertEquals("OFD_PROVIDER_TAG_INVALID", ex.code)
    }

    @Test
    fun testParseTagUnknownProvider() {
        val ex = assertFailsWith<ValidationException> {
            OfdTagValidator.parseTag("INVALID_PROVIDER:PROD")
        }
        assertEquals("OFD_PROVIDER_UNKNOWN", ex.code)
    }

    @Test
    fun testParseTagUnknownEnvironment() {
        val ex = assertFailsWith<ValidationException> {
            OfdTagValidator.parseTag("KAZAKHTELECOM:INVALID_ENV")
        }
        assertEquals("OFD_ENVIRONMENT_UNKNOWN", ex.code)
    }
}
