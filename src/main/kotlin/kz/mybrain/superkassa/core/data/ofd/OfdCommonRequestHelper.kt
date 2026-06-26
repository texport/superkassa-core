package kz.mybrain.superkassa.core.data.ofd

import kz.mybrain.superkassa.core.domain.model.Money
import kz.mybrain.superkassa.core.domain.model.VatGroup
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object OfdCommonRequestHelper {

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

    fun moneyObject(bills: Long, coins: Int): JsonObject {
        return buildJsonObject {
            put("bills", JsonPrimitive(bills))
            put("coins", JsonPrimitive(coins))
        }
    }

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

    @Suppress("FunctionOnlyReturningConstant", "UNUSED_PARAMETER")
    fun taxTypeForGroup(group: VatGroup): Int = 100
}
