package kz.mybrain.superkassa.core.data.ofd.builder

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kz.mybrain.superkassa.core.domain.model.common.Money
import kz.mybrain.superkassa.core.domain.model.common.VatGroup

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
        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
        return buildJsonObject {
            put(
                "date",
                buildJsonObject {
                    put("year", JsonPrimitive(zdt.year))
                    put("month", JsonPrimitive(zdt.monthValue))
                    put("day", JsonPrimitive(zdt.dayOfMonth))
                }
            )
            put(
                "time",
                buildJsonObject {
                    put("hour", JsonPrimitive(zdt.hour))
                    put("minute", JsonPrimitive(zdt.minute))
                    put("second", JsonPrimitive(zdt.second))
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
    @Suppress("FunctionOnlyReturningConstant", "UNUSED_PARAMETER")
    fun taxTypeForGroup(group: VatGroup): Int = 100
}
