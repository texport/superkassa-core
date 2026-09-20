package io.github.texport.superkassa.core.domain.api.model.receipt

/**
 * Рекламная строка, присланная ОФД для печати на чеке.
 *
 * Версию и вид касса хранит вместе с текстом не для печати: их она сообщает
 * ОФД в служебной части (`ticket_ad_infos`), и по ним ОФД решает, что кассе
 * присылать. Пока хранился один текст, кассе нечего было сообщить, ОФД
 * нечего было сравнивать — и объявления не приходили никогда.
 *
 * @property type Вид объявления по CPCR: `TICKET_AD_OFD`, `TICKET_AD_ORG` и прочие.
 * @property version Версия объявления этого вида; растёт у ОФД.
 * @property text Печатаемая строка.
 */
data class TicketAd(
    val type: String,
    val version: Long,
    val text: String
)
