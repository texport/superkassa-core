package io.github.texport.superkassa.core.string.impl

import io.github.texport.superkassa.core.string.api.TrilingualMessage

/**
 * Причины, по которым БФД не выполнил команду кассы, словами для кассира:
 * что случилось и что сделать.
 *
 * Коды — ResultTypeEnum протокола CPCR. Номер кода и текст сетевой ошибки
 * в сообщение не входят: кассиру они ничего не говорят, а их место —
 * журнал и код ошибки.
 */
internal object BfdResultStrings {

    /** Отказ БФД с кодом [code]; код, которого протокол не знает, — общими словами. */
    fun refusal(code: Int): TrilingualMessage = REFUSALS[code] ?: unknownRefusal()

    fun noAnswer(): TrilingualMessage = TrilingualMessage(
        ru = "БФД не ответил. Проверьте подключение к интернету и повторите попытку.",
        kk = "БФД жауап бермеді. Интернет байланысын тексеріп, әрекетті қайталаңыз.",
        en = "The BFD did not answer. Check the internet connection and try again."
    )

    fun notSent(): TrilingualMessage = TrilingualMessage(
        ru = "Касса не смогла отправить запрос в БФД. Проверьте настройки БФД и повторите попытку; " +
            "если не поможет, обратитесь в сервисную службу.",
        kk = "Касса БФД-ға сұранысты жібере алмады. БФД баптауларын тексеріп, әрекетті қайталаңыз; " +
            "көмектеспесе, сервистік қызметке хабарласыңыз.",
        en = "The register could not send the request to the BFD. Check the BFD settings and try again; " +
            "if that does not help, contact the service desk."
    )

    private fun unknownRefusal() = TrilingualMessage(
        ru = "БФД отказал по причине, которую касса не знает. Обратитесь в сервисную службу.",
        kk = "БФД кассаға белгісіз себеппен бас тартты. Сервистік қызметке хабарласыңыз.",
        en = "The BFD refused for a reason the register does not know. Contact the service desk."
    )

    private fun serviceDesk(ru: String, kk: String, en: String) = TrilingualMessage(
        ru = "$ru Обратитесь в сервисную службу.",
        kk = "$kk Сервистік қызметке хабарласыңыз.",
        en = "$en Contact the service desk."
    )

    private fun repeatThenServiceDesk(ru: String, kk: String, en: String) = TrilingualMessage(
        ru = "$ru Повторите операцию; если отказ повторится, обратитесь в сервисную службу.",
        kk = "$kk Операцияны қайталаңыз; бас тарту қайталанса, сервистік қызметке хабарласыңыз.",
        en = "$en Repeat the operation; if it is refused again, contact the service desk."
    )

    private fun retryLater(ru: String, kk: String, en: String) = TrilingualMessage(
        ru = "$ru Повторите попытку через несколько минут; если это длится долго, обратитесь в БФД.",
        kk = "$kk Бірнеше минуттан кейін қайталаңыз; ұзаққа созылса, БФД-ға хабарласыңыз.",
        en = "$en Try again in a few minutes; if this lasts, contact the BFD."
    )

    private val REFUSALS: Map<Int, TrilingualMessage> = mapOf(
        1 to TrilingualMessage(
            ru = "БФД не знает эту кассу. Проверьте регистрационный номер КГД в настройках или обратитесь в БФД.",
            kk = "БФД бұл кассаны білмейді. Баптаулардағы КГД тіркеу нөмірін тексеріңіз немесе БФД-ға хабарласыңыз.",
            en = "The BFD does not know this register. Check the KGD registration number in the settings or contact the BFD."
        ),
        2 to TrilingualMessage(
            ru = "БФД не принял токен кассы. Введите действующий токен в настройках.",
            kk = "БФД касса токенін қабылдамады. Баптауларда жарамды токенді енгізіңіз.",
            en = "The BFD rejected the register token. Enter a valid token in the settings."
        ),
        3 to serviceDesk(
            ru = "БФД не разобрал запрос кассы.",
            kk = "БФД касса сұранысын түсінбеді.",
            en = "The BFD could not read the register's request."
        ),
        4 to serviceDesk(
            ru = "БФД не знает эту команду кассы.",
            kk = "БФД кассаның бұл командасын білмейді.",
            en = "The BFD does not know this register command."
        ),
        5 to serviceDesk(
            ru = "БФД не выполняет эту команду.",
            kk = "БФД бұл команданы орындамайды.",
            en = "The BFD does not support this command."
        ),
        6 to serviceDesk(
            ru = "БФД считает настройки кассы неверными.",
            kk = "БФД касса баптауларын қате деп санайды.",
            en = "The BFD considers the register settings invalid."
        ),
        7 to TrilingualMessage(
            ru = "БФД не разрешает этой кассе защищённое соединение. Подключите услугу в БФД или выберите открытое соединение.",
            kk = "БФД бұл кассаға қорғалған байланысқа рұқсат бермейді. БФД-да қызметті қосыңыз немесе ашық байланысты таңдаңыз.",
            en = "The BFD does not allow this register a secure connection. Enable the service at the BFD or choose an open connection."
        ),
        8 to repeatThenServiceDesk(
            ru = "БФД не принял номер запроса кассы.",
            kk = "БФД касса сұранысының нөмірін қабылдамады.",
            en = "The BFD rejected the register's request number."
        ),
        9 to repeatThenServiceDesk(
            ru = "БФД не принял повторный запрос кассы.",
            kk = "БФД кассаның қайталама сұранысын қабылдамады.",
            en = "The BFD rejected the register's repeated request."
        ),
        11 to TrilingualMessage(
            ru = "Смена открыта дольше допустимого. Закройте смену Z-отчётом.",
            kk = "Ауысым рұқсат етілгеннен ұзақ ашық. Ауысымды Z-есеппен жабыңыз.",
            en = "The shift has been open too long. Close it with a Z-report."
        ),
        12 to TrilingualMessage(
            ru = "БФД не принял имя или пароль кассы. Проверьте учётные данные БФД в настройках.",
            kk = "БФД кассаның атын немесе құпия сөзін қабылдамады. Баптаулардағы БФД тіркелгі деректерін тексеріңіз.",
            en = "The BFD rejected the register login or password. Check the BFD credentials in the settings."
        ),
        13 to TrilingualMessage(
            ru = "БФД не принял данные документа. Проверьте позиции, суммы и реквизиты и оформите документ заново.",
            kk = "БФД құжат деректерін қабылдамады. Позицияларды, сомаларды және деректемелерді тексеріп, құжатты қайта ресімдеңіз.",
            en = "The BFD rejected the document data. Check the items, amounts and details and issue the document again."
        ),
        14 to TrilingualMessage(
            ru = "В кассе недостаточно наличных для этой операции. Внесите наличные или уменьшите сумму.",
            kk = "Кассада бұл операция үшін қолма-қол ақша жеткіліксіз. Қолма-қол ақша салыңыз немесе соманы азайтыңыз.",
            en = "There is not enough cash in the register for this operation. Deposit cash or reduce the amount."
        ),
        15 to TrilingualMessage(
            ru = "БФД заблокировал кассу. Обратитесь в БФД, чтобы узнать причину и снять блокировку.",
            kk = "БФД кассаны бұғаттады. Себебін білу және бұғатты алу үшін БФД-ға хабарласыңыз.",
            en = "The BFD has blocked the register. Contact the BFD to find out why and lift the block."
        ),
        17 to TrilingualMessage(
            ru = "ИИН/БИН покупателя совпадает с продавцом. Исправьте ИИН/БИН покупателя и оформите чек заново.",
            kk = "Сатып алушының ЖСН/БСН сатушыныкімен сәйкес келеді. Сатып алушының ЖСН/БСН түзетіп, чекті қайта ресімдеңіз.",
            en = "The customer IIN/BIN matches the seller's. Correct the customer IIN/BIN and issue the receipt again."
        ),
        18 to TrilingualMessage(
            ru = "Касса снята с учёта и оформлять документы больше не может. Обратитесь в БФД.",
            kk = "Касса есептен шығарылды және енді құжат ресімдей алмайды. БФД-ға хабарласыңыз.",
            en = "The register has been deregistered and can no longer issue documents. Contact the BFD."
        ),
        19 to TrilingualMessage(
            ru = "Касса отключена от БФД. Обратитесь в БФД, чтобы подключить её снова.",
            kk = "Касса БФД-дан ажыратылды. Оны қайта қосу үшін БФД-ға хабарласыңыз.",
            en = "The register has been disconnected from the BFD. Contact the BFD to reconnect it."
        ),
        254 to retryLater(
            ru = "БФД временно недоступен.",
            kk = "БФД уақытша қолжетімсіз.",
            en = "The BFD is temporarily unavailable."
        ),
        255 to retryLater(
            ru = "БФД ответил неизвестной ошибкой.",
            kk = "БФД белгісіз қатемен жауап берді.",
            en = "The BFD answered with an unknown error."
        )
    )
}
