package io.github.texport.superkassa.core.data.ofd.builder

import io.github.texport.superkassa.core.domain.model.common.Money
import io.github.texport.superkassa.core.domain.model.common.VatGroup
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Вспомогательный класс для формирования общих объектов и структур данных
 * при взаимодействии с оператором фискальных данных (ОФД).
 */
object OfdCommonRequestHelper {

    /**
     * Преобразует временную метку в миллисекундах [epochMillis] в JSON-объект даты и времени,
     * разделенный на составляющие (год, месяц, день, час, минута, секунда) в системной часовой зоне.
     *
     * @param epochMillis временная метка в миллисекундах.
     * @return [JsonObject], содержащий объекты "date" и "time".
     */
    fun toDateTime(epochMillis: Long): JsonObject {
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMillis)
        val tz = TimeZone.of("Asia/Almaty")
        val ldt = instant.toLocalDateTime(tz)
        return buildJsonObject {
            put(
                "date",
                buildJsonObject {
                    put("year", JsonPrimitive(ldt.year))
                    put("month", JsonPrimitive(ldt.monthNumber))
                    put("day", JsonPrimitive(ldt.dayOfMonth))
                }
            )
            put(
                "time",
                buildJsonObject {
                    put("hour", JsonPrimitive(ldt.hour))
                    put("minute", JsonPrimitive(ldt.minute))
                    put("second", JsonPrimitive(ldt.second))
                }
            )
        }
    }

    /**
     * Создает JSON-объект для представления денежной суммы, разделенной на купюры/тенге [bills] и монеты/тиын [coins].
     *
     * @param bills сумма в целых единицах (купюры/тенге).
     * @param coins сумма в дробных единицах (монеты/тиын).
     * @return [JsonObject] с полями "bills" и "coins".
     */
    fun moneyObject(bills: Long, coins: Int): JsonObject {
        return buildJsonObject {
            put("bills", JsonPrimitive(bills))
            put("coins", JsonPrimitive(coins))
        }
    }

    /**
     * Суммирует список денежных объектов [values], корректно складывая купюры и монеты,
     * обрабатывая переполнение монет и игнорируя null-значения.
     *
     * @param values список денежных сумм для сложения (может содержать null).
     * @return итоговый объект [Money] или `null`, если список пуст.
     */
    fun sumMoney(values: List<Money?>): Money? {
        if (values.isEmpty()) return null

        var totalCoins = 0L
        values.forEach { money ->
            if (money != null) {
                totalCoins += money.bills * 100 + money.coins
            }
        }

        if (totalCoins == 0L) return Money(0, 0)

        val bills = totalCoins / 100
        val coins = (totalCoins % 100).toInt()
        return Money(bills, coins)
    }

    /**
     * Определяет числовой код типа налога в системе ОФД для заданной группы НДС [group].
     * В текущей реализации возвращает фиксированное значение 100.
     *
     * @param group группа НДС [VatGroup].
     * @return код типа налога.
     */
    fun taxTypeForGroup(group: VatGroup): Int = if (group == VatGroup.VAT_0) 100 else 100
}
