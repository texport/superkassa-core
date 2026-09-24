package io.github.texport.superkassa.core.string.impl

import io.github.texport.superkassa.core.string.api.TrilingualMessage

/**
 * Причины, по которым чек не дошёл до покупателя, для журнала кассира:
 * что случилось и что сделать.
 */
internal object DeliveryTaskStrings {
    fun failedWithoutReason(channel: String): TrilingualMessage = TrilingualMessage(
        ru = "Канал $channel не отправил чек и не назвал причину. Касса повторит отправку сама.",
        kk = "$channel арнасы чекті жібермеді және себебін көрсетпеді. Касса жіберуді өзі қайталайды.",
        en = "Channel $channel did not send the receipt and gave no reason. The register will retry by itself."
    )

    fun documentMissing(): TrilingualMessage = TrilingualMessage(
        ru = "Чек для отправки не найден в базе кассы. Проверьте документ в журнале.",
        kk = "Жіберуге арналған чек касса базасында табылмады. Құжатты журналдан тексеріңіз.",
        en = "The receipt to send is not in the register database. Check the document in the journal."
    )

    fun linkMissing(): TrilingualMessage = TrilingualMessage(
        ru = "Ссылки на чек от БФД нет, отправить ссылку нельзя. Выберите отправку чека файлом.",
        kk = "БФД-дан чекке сілтеме жоқ, сілтемені жіберу мүмкін емес. Чекті файлмен жіберуді таңдаңыз.",
        en = "The BFD gave no receipt link, so a link cannot be sent. Choose sending the receipt as a file."
    )

    fun preparationFailed(channel: String): TrilingualMessage = TrilingualMessage(
        ru = "Не удалось подготовить чек для отправки через $channel. Касса повторит отправку сама.",
        kk = "$channel арқылы жіберу үшін чекті дайындау мүмкін болмады. Касса жіберуді өзі қайталайды.",
        en = "Could not prepare the receipt for sending via $channel. The register will retry by itself."
    )

    fun channelUnknown(channel: String): TrilingualMessage = TrilingualMessage(
        ru = "Канал доставки $channel кассе неизвестен. Выберите SMS, Telegram, WhatsApp, почту или печать.",
        kk = "$channel жеткізу арнасы кассаға белгісіз. SMS, Telegram, WhatsApp, поштаны немесе басып шығаруды таңдаңыз.",
        en = "Delivery channel $channel is unknown to the register. Choose SMS, Telegram, WhatsApp, email or print."
    )

    fun printerNeedsEscPos(): TrilingualMessage = TrilingualMessage(
        ru = "Принтер чеков принимает только ESC/POS. Отправьте на печать чек, а не файл.",
        kk = "Чек принтері тек ESC/POS қабылдайды. Басып шығаруға файлды емес, чекті жіберіңіз.",
        en = "The receipt printer accepts ESC/POS only. Send the receipt to print, not a file."
    )

    fun printerUnreachable(): TrilingualMessage = TrilingualMessage(
        ru = "Принтер чеков в сети не отвечает. Проверьте, что он включён и подключён к сети.",
        kk = "Желідегі чек принтері жауап бермейді. Оның қосулы және желіге қосылғанын тексеріңіз.",
        en = "The network receipt printer does not answer. Check that it is on and connected."
    )

    fun printPayloadMissing(): TrilingualMessage = TrilingualMessage(
        ru = "Нечего печатать: чек для принтера не подготовлен. Повторите печать из журнала.",
        kk = "Басып шығаратын ештеңе жоқ: принтерге чек дайындалмаған. Журналдан басып шығаруды қайталаңыз.",
        en = "Nothing to print: the receipt was not prepared for the printer. Print again from the journal."
    )
}
