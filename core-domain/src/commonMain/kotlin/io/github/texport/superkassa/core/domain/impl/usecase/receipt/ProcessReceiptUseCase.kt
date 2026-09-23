package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.domain.impl.helper.tax.TaxCalculator
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.impl.helper.ReceiptDeliveryHelper

import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.common.UnitOfMeasurement
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.receipt.ParentTicket
import io.github.texport.superkassa.core.domain.api.model.kkm.becameFiscal
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem
import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.queue.OfflineQueueCommandRequest
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptResult
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.helper.common.IdempotentOperationExecutor
import io.github.texport.superkassa.core.domain.impl.helper.KkmCommonHelper
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.RequireOperationalUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.shift.RecalculateShiftCountersUseCase

/**
 * Сценарий обработки чека продажи или возврата на ККМ.
 *
 * Осуществляет расчет налогов (НДС), сохраняет информацию о чеке в локальное хранилище данных,
 * ставит команду в офлайн-очередь или сразу отправляет в ОФД, инициирует доставку чека покупателю
 * и возвращает фискальный результат выполнения.
 *
 * @property storage Порт для доступа к персистентному хранилищу данных.
 * @property queue Порт для работы с офлайн-очередью команд ККМ.
 * @property fiscalOperationExecutor Компонент обеспечения идемпотентности фискальных операций.
 * @property kkmCommonHelper Вспомогательный класс для выполнения общих операций ККМ.
 * @property receiptDeliveryHelper Помощник для форматирования и отправки фискальных чеков.
 * @property authorizeUser Сценарий авторизации и загрузки ККМ.
 * @property requireOperational Сценарий проверки работоспособности ККМ.
 * @property processOfdDocumentResult Лямбда-функция для обработки результатов фискализации документов в ОФД.
 * @property ofdResultQueuedOffline Лямбда-функция для создания фиктивного успешного ответа при офлайн-очереди.
 */
class ProcessReceiptUseCase(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val fiscalOperationExecutor: IdempotentOperationExecutor,
    private val kkmCommonHelper: KkmCommonHelper,
    private val receiptDeliveryHelper: ReceiptDeliveryHelper,
    private val authorizeUser: AuthorizeUserUseCase,
    private val requireOperational: RequireOperationalUseCase,
    private val recalculateShiftCounters: RecalculateShiftCountersUseCase,
    private val processOfdDocumentResult:
    (KkmInfo, String, String, OfdCommandResult, OfdCommandType, Long, Pair<ReceiptRequest, String>?) -> Unit,
    private val ofdResultQueuedOffline: () -> OfdCommandResult,
    /**
     * Виды оплаты, разрешённые действующей версией протокола. По умолчанию
     * разрешены все: сборка узла подставляет набор своей версии.
     */
    private val supportedPayments: Set<PaymentType> = PaymentType.entries.toSet(),
    private val protocolVersion: String = "203"
) {
    /**
     * Калькулятор для вычисления налогов на товары и услуги в чеке.
     */
    private val taxCalculator = TaxCalculator()

    /**
     * Выполняет обработку и регистрацию фискального чека.
     *
     * @param request Запрос чека с перечнем товаров, платежей и настроек доставки.
     * @param kkm Информация о текущем кассовом аппарате (налоговый режим, дефолтная группа НДС).
     * @return Объект результата обработки чека с фискальными признаками.
     * @throws ConflictException если смена на ККМ закрыта.
     */
    fun execute(command: CreateReceiptCommand): ReceiptResult {
        val kkm = authorizeUser.requireKkm(command.kkmId)
        requireOperational.execute(kkm)
        requireReceiptWithinBounds(command)

        // 1. Валидация входных данных команды
        if ((command.operation == ReceiptOperationType.SELL_RETURN || command.operation == ReceiptOperationType.BUY_RETURN) && command.parentTicket == null) {
            throw ValidationException(
                CoreStrings.parentTicketRequiredForReturns(),
                "PARENT_TICKET_REQUIRED"
            )
        }

        // 2. Расчет сумм и маппинг позиций
        val receiptItems = command.items.map { dto ->
            val quantityThousandths = dto.quantity.scaled(QUANTITY_SCALE)
            val baseSumTiyn = Decimal.roundedDiv(
                dto.price.scaled(TIYN_SCALE) * quantityThousandths,
                THOUSANDTHS_IN_ONE
            )
            val itemDiscountTiyn = partOf(baseSumTiyn, dto.discountPercent, dto.discountSum)
            val itemMarkupTiyn = partOf(baseSumTiyn, dto.markupPercent, dto.markupSum)
            val itemSumTiyn = (baseSumTiyn - itemDiscountTiyn + itemMarkupTiyn).coerceAtLeast(0L)
            val itemDiscount = if (itemDiscountTiyn > 0) Money.fromTiyn(itemDiscountTiyn) else null
            val itemMarkup = if (itemMarkupTiyn > 0) Money.fromTiyn(itemMarkupTiyn) else null
            val measureUnitCode = dto.measureUnitCode?.takeIf { it.isNotBlank() }?.let { raw ->
                try {
                    UnitOfMeasurement.fromCode(raw).code
                } catch (_: IllegalArgumentException) {
                    throw ValidationException(CoreStrings.measureUnitCodeInvalid(raw), "MEASURE_UNIT_CODE_INVALID")
                }
            }
            ReceiptItem(
                name = dto.name,
                nameKk = dto.nameKk?.takeIf { it.isNotBlank() },
                sectionCode = "001",
                quantity = quantityThousandths,
                price = Money.fromTenge(dto.price),
                sum = Money.fromTiyn(itemSumTiyn),
                barcode = dto.barcode?.takeIf { it.isNotBlank() },
                vatGroup = dto.vatGroup?.let { value ->
                    try {
                        VatGroup.valueOf(value)
                    } catch (_: IllegalArgumentException) {
                        throw IllegalArgumentException(
                            "Invalid vatGroup: $value. Valid: " +
                                VatGroup.entries.joinToString { it.name }
                        )
                    }
                },
                discount = itemDiscount,
                markup = itemMarkup,
                measureUnitCode = measureUnitCode,
                listExciseStamp = dto.listExciseStamp?.takeIf { it.isNotEmpty() },
                ntin = dto.ntin?.takeIf { it.isNotBlank() },
                isStorno = dto.isStorno
            )
        }

        // 3. Проверка конфликтов скидок на уровне позиций и чека
        // Скидка и наценка живут либо на позициях, либо на чеке — вместе
        // их не принимает ни ОФД, ни касса парка. Наценка проверяется
        // наравне со скидкой: правило про уровень, а не про знак.
        val hasItemChanges = receiptItems.any { it.discount != null || it.markup != null }
        val hasReceiptChanges = command.discountPercent != null || command.discountSum != null ||
            command.markupPercent != null || command.markupSum != null
        if (hasItemChanges && hasReceiptChanges) {
            throw ValidationException(
                CoreStrings.receiptDiscountScopesConflict(),
                "RECEIPT_DISCOUNT_SCOPES_CONFLICT"
            )
        }
        // Скидка и наценка на сам чек — одно из двух: `ticket.amounts`
        // с обеими сервис приёма отвергает по существу, и на 2.0.4 отказ
        // приходил кассиру путём поля JSON вместо человеческих слов.
        val discountOnReceipt = command.discountPercent != null || command.discountSum != null
        val markupOnReceipt = command.markupPercent != null || command.markupSum != null
        if (discountOnReceipt && markupOnReceipt) {
            throw ValidationException(
                CoreStrings.receiptDiscountAndMarkupConflict(),
                "RECEIPT_DISCOUNT_AND_MARKUP"
            )
        }

        // 4. Расчет сумм скидок/наценок на чек и итоговой суммы чека
        val itemsTotalTiyn = receiptItems.sumOf { item ->
            if (item.isStorno) -item.sum.tiyn() else item.sum.tiyn()
        }
        val receiptDiscountTiyn = partOf(itemsTotalTiyn, command.discountPercent, command.discountSum)
        val receiptMarkupTiyn = partOf(itemsTotalTiyn, command.markupPercent, command.markupSum)
        val totalTiyn = (itemsTotalTiyn - receiptDiscountTiyn + receiptMarkupTiyn).coerceAtLeast(0L)
        val totalMoney = Money.fromTiyn(totalTiyn)

        // 5. Оплаты и сдача
        val receiptPayments = paymentsOf(command.payments, supportedPayments, protocolVersion)
        requireBalanced(receiptPayments, totalTiyn)
        val (takenMoney, changeMoney) = takenAndChange(receiptPayments, command.taken)

        val receiptDiscount = if (receiptDiscountTiyn > 0) Money.fromTiyn(receiptDiscountTiyn) else null
        val receiptMarkup = if (receiptMarkupTiyn > 0) Money.fromTiyn(receiptMarkupTiyn) else null
        val defaultVatGroup = command.defaultVatGroup?.takeIf { it.isNotBlank() }?.let { value ->
            try {
                VatGroup.valueOf(value)
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Invalid defaultVatGroup: $value. Valid: " + VatGroup.entries.joinToString { it.name }
                )
            }
        }

        // Ставка, которую режим кассы не допускает, отвергается, а не
        // отбрасывается молча. Раньше «Без НДС» + VAT_12 на позиции
        // доходило до ОФД чеком без налога: кассир видел и печатал
        // «НДС 12%», а в кабинете у чека налог был пуст.
        requireVatAllowedByRegime(kkm, receiptItems, defaultVatGroup)

        // 5a. Возврат не может превысить остаток по чеку-основанию.
        requireRefundFitsBasis(command, totalMoney)

        // 6. Подготовка доменного ReceiptRequest
        val request = ReceiptRequest(
            kkmId = command.kkmId,
            pin = command.pin,
            operation = command.operation,
            items = receiptItems,
            payments = receiptPayments,
            total = totalMoney,
            taken = takenMoney,
            change = changeMoney,
            idempotencyKey = command.idempotencyKey,
            parentTicket = command.parentTicket,
            defaultVatGroup = defaultVatGroup ?: kkm.defaultVatGroup,
            taxRegime = kkm.taxRegime,
            discount = receiptDiscount,
            markup = receiptMarkup,
            customerBin = command.customerBin,
            domain = command.domain,
            // Имя оформившего сохраняется вместе с чеком: пин на диск
            // не пишется, а разбор отказа ОФД без имени упирается
            // в «кто-то пробил в 14:53».
            operatorName = authorizeUser.identify(command.kkmId, command.pin).name
        )

        // Расчет сумм налогов (НДС) по каждой позиции чека
        val taxResult = taxCalculator.calculateTicketTaxes(
            items = request.items,
            taxRegime = request.taxRegime,
            defaultVatGroup = request.defaultVatGroup ?: VatGroup.NO_VAT
        )
        val requestWithTaxes = request.copy(
            ticketTaxes = taxResult.ticketTaxes
        )

        // Оцениваем статус автономной очереди ДО сохранения нового документа
        val hasQueue = !queue.canSendDirectly(requestWithTaxes.kkmId)

        // Запуск фискальной операции с гарантией идемпотентности
        return fiscalOperationExecutor.executeIdempotentFiscalOperation(
            kkmId = requestWithTaxes.kkmId,
            pin = requestWithTaxes.pin,
            idempotencyKey = requestWithTaxes.idempotencyKey,
            operationType = "CREATE_RECEIPT",
            checkShift = {
                // Проверяем, что смена открыта, и получаем её ID
                val shift = storage.findOpenShift(requestWithTaxes.kkmId)
                    ?: throw ConflictException(CoreStrings.shiftNotOpen(), "SHIFT_NOT_OPEN")
                requireCashForBuy(requestWithTaxes, shift)
                shift.id
            },
            saveOperation = { documentId, now, shiftId ->
                // Сохраняем рассчитанный чек в базу данных
                storage.saveReceipt(requestWithTaxes, documentId, shiftId, now)
            },
            sendOfdCommand = { currentKkm, documentId ->
                val command = OfflineQueueCommandRequest(
                    kkmId = requestWithTaxes.kkmId,
                    type = OfdCommandType.TICKET.value,
                    payloadRef = documentId
                )
                // Если ККМ работает офлайн, ставим чек в очередь, иначе шлем в ОФД сразу
                if (hasQueue) {
                    queue.enqueueOffline(command)
                    ofdResultQueuedOffline()
                } else {
                    kkmCommonHelper.sendOfdCommand(
                        kkm = currentKkm,
                        commandType = OfdCommandType.TICKET,
                        payloadRef = documentId
                    )
                }
            },
            processResult = { currentKkm, documentId, currentKkmId, ofdResult, commandType, now, receiptContext ->
                // Обрабатываем ответ ОФД (обновление счетчиков, перевод ККМ в автономный режим при таймаутах)
                processOfdDocumentResult(
                    currentKkm,
                    documentId,
                    currentKkmId,
                    ofdResult,
                    commandType,
                    now,
                    receiptContext
                )
                // Если чек успешно фискализован в ОФД, инициируем его отправку покупателю
                if (commandType == OfdCommandType.TICKET && receiptContext != null && ofdResult.resultCode == 0) {
                    val (receipt, _) = receiptContext
                    val doc = storage.findFiscalDocumentById(documentId)
                    if (doc != null) {
                        receiptDeliveryHelper.deliverReceipt(
                            kkmId = requestWithTaxes.kkmId,
                            documentId = documentId,
                            receipt = receipt,
                            docSnapshot = doc,
                            receiptUrl = ofdResult.receiptUrl,
                            responseBin = ofdResult.responseBin
                        )
                    }
                }
            },
            buildResult = { documentId, ofdResult, deliveryStatus ->
                // Сборка ответа для вызывающего слоя презентации
                val doc = storage.findFiscalDocumentById(documentId)
                ReceiptResult(
                    documentId = documentId,
                    fiscalSign = doc?.fiscalSign ?: ofdResult.fiscalSign,
                    autonomousSign = doc?.autonomousSign ?: ofdResult.autonomousSign,
                    deliveryPayload = ofdResult.responseBin,
                    deliveryStatus = deliveryStatus,
                    deliveryError = ofdResult.errorMessage
                )
            },
            receiptContextProvider = { shiftId -> Pair(requestWithTaxes, shiftId) }
        )
    }

    /**
     * Не превышает ли возврат остаток по чеку-основанию.
     *
     * Касса складывает всё, что по этому чеку уже возвращено, и сравнивает
     * с его итогом. Без этого по чеку на 700 ₸ проходил возврат 350 ₸,
     * а следом ещё 700 ₸: ни ОФД, ни сервис приёма такой пары не ловят,
     * и касса отдавала покупателю больше, чем получила.
     *
     * Ищется по документам от даты чека-основания до сегодняшнего дня:
     * возврат бывает и в другой смене, а ссылка на основание лежит
     * в самом чеке.
     *
     * @param command что пришло от кассира.
     * @param refund сумма нового возврата.
     * @throws ValidationException если возвращать больше нечего.
     */
    private fun requireRefundFitsBasis(command: CreateReceiptCommand, refund: Money) {
        val basis = command.parentTicket ?: return
        val basisTiyn = basis.parentTicketTotal.tiyn()
        val already = refundedAgainst(command.kkmId, basis)
        val left = basisTiyn - already
        if (refund.tiyn() > left) {
            val words = if (left <= 0L) {
                CoreStrings.refundAlreadyFull()
            } else {
                CoreStrings.refundExceedsBasis(Money.fromTiyn(left).asTenge())
            }
            throw ValidationException(words, "REFUND_EXCEEDS_BASIS")
        }
    }

    /** Сколько по этому чеку-основанию уже возвращено, в тиынах. */
    private fun refundedAgainst(kkmId: String, basis: ParentTicket): Long {
        var offset = 0
        var sum = 0L
        while (true) {
            val page = storage.listFiscalDocumentsByPeriod(
                kkmId = kkmId,
                fromInclusive = basis.parentTicketDateTimeMillis,
                toExclusive = Long.MAX_VALUE,
                limit = PAGE,
                offset = offset
            )
            if (page.isEmpty()) return sum
            page.filter { it.becameFiscal() }.forEach { doc ->
                val stored = storage.findFiscalDocumentWithReceiptPayload(doc.id)?.second
                val sameBasis = stored?.parentTicket?.parentTicketNumber == basis.parentTicketNumber
                if (sameBasis) sum += stored.total.tiyn()
            }
            offset += PAGE
        }
    }

    /**
     * Хватает ли в ящике наличных на покупку у населения.
     *
     * По покупке касса деньги отдаёт, а не получает, и ОФД чек на сумму
     * больше остатка отвергает. Без этой проверки отказ приходил уже после
     * отправки: в журнале оставался отклонённый документ, а кассир читал
     * «Not enough cash» по-английски от ОФД. Правило то же, что у изъятия
     * наличных, и остаток считается тем же расчётом, что в X- и Z-отчёте.
     *
     * @param request чек с уже рассчитанными налогами.
     * @param shift открытая смена этой кассы.
     * @throws ValidationException если наличных в ящике меньше, чем платит чек.
     */
    private fun requireCashForBuy(request: ReceiptRequest, shift: ShiftInfo) {
        if (request.operation != ReceiptOperationType.BUY) return
        val payingCash = request.payments
            .filter { it.type == PaymentType.CASH }
            .sumOf { it.sum.tiyn() }
        if (payingCash == 0L) return
        val inDrawer = recalculateShiftCounters.execute(request.kkmId, shift)[CounterKeyFormats.CASH_SUM] ?: 0L
        if (payingCash > inDrawer) {
            throw ValidationException(CoreStrings.insufficientCash(), "INSUFFICIENT_CASH")
        }
    }
}

/**
 * Скидка или наценка в тиынах: процентом от базы либо готовой суммой.
 *
 * Процент считается от целых тиынов и округляется один раз к ближайшему.
 * Правило одно и для позиции, и для чека, поэтому и место одно.
 *
 * @param baseTiyn база, от которой берётся процент.
 * @param percent доля в процентах либо `null`.
 * @param sum готовая сумма в тенге либо `null`.
 * @return сумма скидки или наценки в тиынах; ноль, если не задана ни одна.
 */
private fun partOf(baseTiyn: Long, percent: Decimal?, sum: Decimal?): Long = when {
    percent != null -> Decimal.roundedDiv(
        baseTiyn * percent.unscaled,
        HUNDRED_PERCENT * pow10(percent.scale)
    )
    sum != null -> sum.scaled(TIYN_SCALE)
    else -> 0L
}

private fun pow10(power: Int): Long {
    var result = 1L
    repeat(power) { result *= 10 }
    return result
}

/** Знаков после запятой у тенге. */
private const val TIYN_SCALE: Int = 2

/** Количество хранится в тысячных долях единицы. */
private const val QUANTITY_SCALE: Int = 3

/** Тысячных в единице. */
private const val THOUSANDTHS_IN_ONE: Long = 1_000

/** Сто процентов. */
private const val HUNDRED_PERCENT: Long = 100

/**
 * Проверяет, что ставки чека допускает налоговый режим кассы.
 *
 * Неплательщик НДС налог не выделяет: при режиме NO_VAT расчёт налога
 * даёт пустой список, и ставка позиции никуда не уходит. Принять такую
 * ставку — значит показать кассиру и покупателю налог, которого в чеке
 * ОФД нет. Отказ громкий и называет ставку, чтобы кассир понял, что
 * исправлять.
 *
 * @param kkm касса, от имени которой оформляется чек.
 * @param items позиции чека.
 * @param defaultVatGroup ставка чека, присланная вызывающим, либо `null`.
 * @throws ValidationException если режим кассы ставку не допускает.
 */
private fun requireVatAllowedByRegime(
    kkm: KkmInfo,
    items: List<ReceiptItem>,
    defaultVatGroup: VatGroup?
) {
    if (kkm.taxRegime != TaxRegime.NO_VAT) return
    val offending = items.firstNotNullOfOrNull { it.vatGroup?.takeIf { group -> group != VatGroup.NO_VAT } }
        ?: defaultVatGroup?.takeIf { it != VatGroup.NO_VAT }
        ?: return
    throw ValidationException(CoreStrings.receiptVatNotAllowed(offending.name), "RECEIPT_VAT_NOT_ALLOWED")
}

/** Сколько документов читается за один заход при поиске прежних возвратов. */
private const val PAGE = 200
