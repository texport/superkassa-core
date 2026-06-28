package kz.mybrain.superkassa.core.domain.validation

import io.mockk.mockk
import jakarta.validation.ConstraintValidatorContext
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ItemNameValidatorTest {

    @Test
    fun testItemNameRulesValid() {
        assertTrue(ItemNameRules.isValid("Молоко 3.2%"))
        assertTrue(ItemNameRules.isValid(null))
        assertTrue(ItemNameRules.isValid("   "))
        assertTrue(ItemNameRules.isValid("Хлеб"))
    }

    @Test
    fun testItemNameRulesInvalidTooShort() {
        assertFalse(ItemNameRules.isValid("Аа"))
        assertFalse(ItemNameRules.isValid("12"))
    }

    @Test
    fun testItemNameRulesInvalidSameLetters() {
        assertFalse(ItemNameRules.isValid("Аааа"))
    }

    @Test
    fun testItemNameRulesInvalidDigitsOnly() {
        assertFalse(ItemNameRules.isValid("123 456"))
    }

    @Test
    fun testItemNameRulesInvalidTooManyDigits() {
        assertFalse(ItemNameRules.isValid("Хлеб12345"))
    }

    @Test
    fun testItemNameRulesForbiddenWords() {
        assertFalse(ItemNameRules.isValid("товар"))
        assertFalse(ItemNameRules.isValid("товар 123"))
        assertFalse(ItemNameRules.isValid("товар молоко"))
        assertFalse(ItemNameRules.isValid("молоко товар"))
        assertFalse(ItemNameRules.isValid("молоко товар хлеб"))
        assertFalse(ItemNameRules.isValid("товар123"))
    }

    @Test
    fun testJakartaValidatorInterface() {
        val validator = ItemNameValidator()
        val mockContext = mockk<ConstraintValidatorContext>(relaxed = true)
        assertTrue(validator.isValid("Молоко", mockContext))
        assertFalse(validator.isValid("товар", mockContext))
    }
}
