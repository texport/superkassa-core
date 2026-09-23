package io.github.texport.superkassa.core.domain.api.model.shift

import io.github.texport.superkassa.core.domain.api.model.kkm.CashOperationType
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot

/**
 * Состояние документа изъятия, которое едет внутри Z-отчёта.
 *
 * Отдельной командой в ОФД такое изъятие не уходит: протокол передаёт
 * его признаком `withdraw_money` запроса закрытия смены, и БФД проводит
 * его в закрываемой смене. Слово то же, что у открытия смены, — «своей
 * дороги в ОФД у документа нет»; в счётчики смены документ входит.
 */
const val WITHDRAWAL_IN_Z_REPORT_STATUS = "INTERNAL"

/** Изъятие всей наличности, проведённое внутри Z-отчёта своей смены. */
fun FiscalDocumentSnapshot.isWithdrawalInZReport(): Boolean =
    docType == CashOperationType.CASH_OUT.name && ofdStatus == WITHDRAWAL_IN_Z_REPORT_STATUS
