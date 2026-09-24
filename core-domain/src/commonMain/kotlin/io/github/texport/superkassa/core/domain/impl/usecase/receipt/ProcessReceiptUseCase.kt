package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.domain.impl.helper.tax.TaxCalculator

import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType
import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
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

    /** Чек-основание возврата и прежние возвраты по нему. */
    private val refundBasis = RefundBasis(storage)

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

        // 2. Позиции: сумма строки — цена × количество к ближайшему тиыну
        val receiptItems = command.items.map(::receiptItemOf)

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
        // НДС на весь чек или по позициям — одно из двух, как скидка.
        val receiptVat = command.vatGroup?.takeIf { it.isNotBlank() }?.let { vatGroupOf(it, "vatGroup") }
        requireOneVatScope(receiptVat, receiptItems)

        // Ставка, которую режим кассы не допускает, отвергается, а не
        // отбрасывается молча. Раньше «Без НДС» + VAT_12 на позиции
        // доходило до ОФД чеком без налога: кассир видел и печатал
        // «НДС 12%», а в кабинете у чека налог был пуст.
        requireVatAllowedByRegime(kkm, receiptItems, receiptVat)
        val vat = withRefundBasisVat(command, kkm, VatScope(receiptItems, receiptVat))

        // 6. Подготовка доменного ReceiptRequest
        val request = ReceiptRequest(
            kkmId = command.kkmId,
            pin = command.pin,
            operation = command.operation,
            items = vat.items,
            payments = receiptPayments,
            total = totalMoney,
            taken = takenMoney,
            change = changeMoney,
            idempotencyKey = command.idempotencyKey,
            parentTicket = command.parentTicket,
            defaultVatGroup = kkm.defaultVatGroup,
            vatGroup = vat.receiptVat,
            taxRegime = kkm.taxRegime,
            discount = receiptDiscount,
            markup = receiptMarkup,
            customerBin = command.customerBin,
            customerContact = command.customerContact,
            domain = command.domain,
            // Имя оформившего сохраняется вместе с чеком: пин на диск
            // не пишется, а разбор отказа ОФД без имени упирается
            // в «кто-то пробил в 14:53».
            operatorName = authorizeUser.identify(command.kkmId, command.pin).name
        )

        // Налог чека — тем же расчётом, что уйдёт в БФД и в счётчики смены
        val requestWithTaxes = request.copy(ticketTaxes = taxCalculator.calculate(request).ticketTaxes)

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
                // Остаток по основанию проверяется только у нового возврата:
                // повтор с тем же ключом находит свой документ раньше. Прежде
                // проверка шла до поиска по ключу, и повтор полного возврата
                // после потерянного ответа получал «возвращено всё».
                requireRefundFitsBasis(command, totalMoney)
                shift.id
            },
            saveOperation = { documentId, now, shiftId ->
                // Сохраняем рассчитанный чек в базу данных
                storage.saveReceipt(requestWithTaxes, documentId, shiftId, now)
            },
            sendOfdCommand = { currentKkm, documentId ->
                // При непустой очереди чек встаёт в её конец: в очередь его ставит
                // обработка ответа, как любой документ без ответа БФД. Ставить
                // его ещё и здесь — дважды: на Postgres вторая постановка
                // отвергается ключом, и продажа падала.
                if (hasQueue) {
                    ofdResultQueuedOffline()
                } else {
                    kkmCommonHelper.sendOfdCommand(
                        kkm = currentKkm,
                        commandType = OfdCommandType.TICKET,
                        payloadRef = documentId
                    )
                }
            },
            // Обработка ответа ставит в очередь, двигает счётчики и доставляет
            // чек покупателю. Доставка стояла и здесь, и покупатель получал
            // каждый принятый онлайн чек дважды.
            processResult = processOfdDocumentResult,
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
        val already = refundBasis.refundedAgainst(command.kkmId, basis)
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

    /**
     * НДС возврата суммой — тем способом и по тем ставкам, что у чека-основания.
     *
     * Основание ищется только у возврата и только когда касса выделяет НДС:
     * неплательщику ставка строки ни на что не влияет.
     */
    private fun withRefundBasisVat(command: CreateReceiptCommand, kkm: KkmInfo, sent: VatScope): VatScope {
        if (kkm.taxRegime == TaxRegime.NO_VAT || sent.items.all { it.vatGroup != null }) return sent
        val parent = command.parentTicket?.takeIf { command.operation.isReturn() } ?: return sent
        val basis = refundBasis.find(command.kkmId, kkm.registrationNumber, parent) ?: return sent
        return basisVat(sent.items, basis)
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

private fun ReceiptOperationType.isReturn(): Boolean =
    this == ReceiptOperationType.SELL_RETURN || this == ReceiptOperationType.BUY_RETURN
