package io.github.texport.superkassa.receiptrenderer.impl.renderer.base

import io.github.texport.superkassa.core.domain.model.receipt.*
import kotlinx.serialization.json.Json

/**
 * Синглтон для локализации текстов чеков (русский, казахский, мультиязычный режим).
 */
internal object ReceiptTranslator {
    private val translations: Map<String, Map<String, String>> by lazy {
        try {
            ResourceLoader.readText("/translations.json")?.let {
                Json.decodeFromString<Map<String, Map<String, String>>>(it)
            } ?: emptyMap()
        } catch (e: kotlinx.serialization.SerializationException) {
            println("Translation serialization failed: ${e.message}")
            emptyMap()
        } catch (e: IllegalArgumentException) {
            println("Translation loader invalid argument: ${e.message}")
            emptyMap()
        } catch (e: IllegalStateException) {
            println("Translation loader state error: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Возвращает карту переводов для заданного ключа.
     *
     * @param key ключ перевода
     * @return карта переводов по языкам или null
     */
    fun getTranslationMap(key: String): Map<String, String>? {
        return translations[key]
    }

    /**
     * Переводит заданный ключ для указанного языка.
     *
     * @param key ключ перевода
     * @param lang язык чека
     * @return переведенная строка
     */
    fun translate(key: String, lang: ReceiptLanguage): String {
        val trans = translations[key]
        val ru = trans?.get("ru") ?: key
        val kk = trans?.get("kk") ?: key
        return translate(ru, kk, lang)
    }

    /**
     * Формирует локализованную строку на основе переданных строк на русском и казахском.
     *
     * @param labelRu строка на русском
     * @param labelKk строка на казахском
     * @param lang язык чека
     * @return локализованная (возможно, двухэтажная HTML) строка
     */
    fun translate(labelRu: String, labelKk: String, lang: ReceiptLanguage): String {
        if (labelRu == labelKk) return labelRu
        return when (lang) {
            ReceiptLanguage.RU -> labelRu
            ReceiptLanguage.KK -> labelKk
            ReceiptLanguage.MIXED -> {
                TemplateRenderer.render(
                    "lang_fraction.html",
                    mapOf("kk" to labelKk, "ru" to labelRu)
                )
            }
        }
    }

    /**
     * Переводит заданный ключ в одну строку (через разделитель).
     *
     * @param key ключ перевода
     * @param lang язык чека
     * @param separator разделитель языков
     * @return переведенная строка
     */
    fun translateInline(key: String, lang: ReceiptLanguage, separator: String = " / "): String {
        val trans = translations[key]
        val ru = trans?.get("ru") ?: key
        val kk = trans?.get("kk") ?: key
        return translateInline(ru, kk, lang, separator)
    }

    /**
     * Формирует локализованную строку в одну строку через разделитель.
     *
     * @param labelRu строка на русском
     * @param labelKk строка на казахском
     * @param lang язык чека
     * @param separator разделитель языков
     * @return локализованная однострочная строка
     */
    fun translateInline(labelRu: String, labelKk: String, lang: ReceiptLanguage, separator: String = " / "): String {
        if (labelRu == labelKk) return labelRu
        return when (lang) {
            ReceiptLanguage.RU -> labelRu
            ReceiptLanguage.KK -> labelKk
            ReceiptLanguage.MIXED -> "$labelKk$separator$labelRu"
        }
    }
}
