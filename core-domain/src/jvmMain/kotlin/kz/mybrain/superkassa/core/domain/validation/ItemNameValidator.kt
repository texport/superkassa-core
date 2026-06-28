package kz.mybrain.superkassa.core.domain.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * Валидатор соответствия стандарту Jakarta Bean Validation (JSR-303) для наименования товара или услуги.
 *
 * Реализует интерфейс [ConstraintValidator] для обработки аннотации [ItemNameValid].
 * Является связующим звеном между инфраструктурным слоем валидации Jakarta и чистыми
 * бизнес-правилами предметной области, описанными в [ItemNameRules].
 */
class ItemNameValidator : ConstraintValidator<ItemNameValid, String?> {

    /**
     * Выполняет проверку корректности наименования товара.
     *
     * @param value Проверяемая строка (наименование товара), может быть null.
     * @param context Контекст выполнения валидатора Jakarta.
     * @return true, если значение null, пустое или соответствует бизнес-правилам [ItemNameRules].
     */
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        // Делегируем фактическую проверку чистым бизнес-правилам домена
        return ItemNameRules.isValid(value)
    }
}
