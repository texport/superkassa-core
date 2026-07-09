package io.github.texport.superkassa.core.string.api

/**
 * Кроссплатформенный хелпер для форматирования строк вида %s в KMP.
 */
fun String.format(vararg args: Any?): String {
    var result = this
    val regex = Regex("%[sdf]")
    for (arg in args) {
        result = result.replaceFirst(regex, arg?.toString() ?: "null")
    }
    return result
}
