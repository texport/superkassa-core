package kz.mybrain.superkassa.core.domain.validation

/**
 * Чистые бизнес-правила валидации названия товара или услуги.
 *
 * Не зависят от внешних библиотек и фреймворков валидации (Java/Jakarta), что делает
 * данный объект полностью переносимым и готовым для использования в Kotlin Multiplatform (KMP).
 */
object ItemNameRules {
    
    // Список запрещенных обобщенных слов в нижнем регистре на трех языках (русский, казахский, английский).
    private val forbiddenWords = setOf(
        "товар", "товары", "продукт", "продукты", "продукция", "изделие",
        "услуга", "услуги", "сервис", "сервисы", "работа", "работы",
        "product", "products", "item", "items", "service", "services",
        "goods", "тауар", "қызмет", "жұмыс", "өнім"
    ).map { it.lowercase() }

    /**
     * Проверяет, является ли переданное наименование товара корректным согласно бизнес-правилам.
     *
     * @param value Проверяемая строка наименования.
     * @return true, если наименование корректно или пусто (null/blank). false в случае нарушения правил.
     */
    fun isValid(value: String?): Boolean {
        // Если значение не указано, считаем его валидным на данном уровне (обязательность проверяется отдельно)
        if (value.isNullOrBlank()) return true
        
        val normalized = value.trim().lowercase()
        // Минимальная длина нормализованной строки должна быть не менее 3 символов
        if (normalized.length < 3) return false

        // Требование конкретности: в наименовании должно быть как минимум 3 буквенных символа
        val letters = normalized.filter { it.isLetter() }
        if (letters.length < 3) return false

        // Требование конкретности: буквы в наименовании не должны быть одинаковыми (минимум 2 уникальные буквы)
        if (letters.toSet().size < 2) return false

        // Не допускается наименование, состоящее исключительно из цифр и пробелов
        if (normalized.all { it.isDigit() || it.isWhitespace() }) return false

        // Наименование не должно состоять преимущественно из цифр (доля цифр не должна превышать 50% от всей длины)
        val digitShare = normalized.count { it.isDigit() }.toDouble() / normalized.length
        if (digitShare > 0.5) return false

        // Проверка на точное совпадение или комбинации с запрещенными словами
        for (word in forbiddenWords) {
            // Точное совпадение с запрещенным словом (например, "товар")
            if (normalized == word) return false
            // Шаблон "запрещенное слово + пробелы + цифры" (например, "услуга 123")
            if (normalized.matches(Regex("^${Regex.escape(word)}\\s+\\d+$"))) return false
            // Начинается с запрещенного слова с пробелом или заканчивается им
            if (normalized.startsWith("$word ") || normalized.endsWith(" $word")) return false
            // Запрещенное слово окружено пробелами внутри строки
            if (" $word " in normalized) return false
        }

        // Проверка отдельных токенов (слов в строке), разделенных пробелами
        val tokens = normalized.split(Regex("\\s+"))
        for (token in tokens) {
            // Токен является запрещенным словом
            if (token in forbiddenWords) return false
            // Токен начинается на запрещенное слово и сразу же переходит в цифры (например, "товар123", "услуга456")
            for (word in forbiddenWords) {
                if (token.matches(Regex("^${Regex.escape(word)}\\d+$"))) return false
            }
        }

        return true
    }
}
