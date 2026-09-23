package io.github.texport.superkassa.coredatabase.api

import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptBranding
import io.github.texport.superkassa.coredatabase.impl.entity.strictBrandingOf

/**
 * Оформление чека из строки `branding_json` узла.
 *
 * Формат тот же, в каком оформление хранит касса: имена полей и умолчания
 * представления оформления узла, пустое поле — умолчание. Касса при чтении
 * своей колонки неразборную строку заменяет умолчанием; здесь — отказ:
 * перенос не должен молча сбрасывать оформление владельца.
 *
 * @throws IllegalArgumentException если строка не разбирается или язык чека незнаком.
 */
fun parseBrandingJson(json: String): ReceiptBranding = strictBrandingOf(json)
