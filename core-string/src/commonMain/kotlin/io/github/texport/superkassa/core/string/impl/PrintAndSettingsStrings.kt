package io.github.texport.superkassa.core.string.impl

import io.github.texport.superkassa.core.string.api.TrilingualMessage

/** Отказы печати по пакету протокола и отказы изменения настроек ядра. */
internal object PrintAndSettingsStrings {
    fun protocolPacketUnreadable(): TrilingualMessage = TrilingualMessage(
        ru = "Пакет протокола не разобран: печатать нечего",
        kk = "Хаттама пакеті талданбады: басып шығаратын ештеңе жоқ",
        en = "Protocol packet is unreadable: there is nothing to print"
    )

    fun protocolPacketNotADocument(): TrilingualMessage = TrilingualMessage(
        ru = "Команда пакета не порождает документа с печатной формой",
        kk = "Пакет командасы басып шығару пішіні бар құжат жасамайды",
        en = "Packet command does not produce a printable document"
    )

    fun settingsFrozenServerMode(): TrilingualMessage = TrilingualMessage(
        ru = "Настройки не могут быть изменены через API в режиме SERVER.",
        kk = "Параметрлерді SERVER режимінде API арқылы өзгерту мүмкін емес.",
        en = "Settings cannot be modified via API in SERVER mode."
    )

    fun settingsFrozen(): TrilingualMessage = TrilingualMessage(
        ru = "Изменение настроек заморожено. Разрешите изменения в файле конфигурации.",
        kk = "Параметрлерді өзгерту бұғатталған. Алдымен конфигурация файлында рұқсат етіңіз.",
        en = "Settings changes are frozen. Allow changes in configuration file first."
    )

    fun ofdProtocolVersionFixedAtStartup(current: String): TrilingualMessage = TrilingualMessage(
        ru = "Версия протокола задаётся при запуске и через API не меняется. Сейчас $current.",
        kk = "Хаттама нұсқасы іске қосу кезінде беріледі және API арқылы өзгермейді. Қазір $current.",
        en = "Protocol version is set at startup and cannot be changed through the API. Currently $current."
    )
}
