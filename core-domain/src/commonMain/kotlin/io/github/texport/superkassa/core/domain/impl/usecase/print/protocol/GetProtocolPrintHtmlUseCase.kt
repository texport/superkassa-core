package io.github.texport.superkassa.core.domain.impl.usecase.print.protocol

import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLayoutType
import io.github.texport.superkassa.core.domain.api.port.internal.ReceiptRenderPort
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.string.api.CoreStrings

/**
 * Печатная форма документа по пакету протокола.
 *
 * Кабинет присылает сырой пакет документа, пробитого на любой кассе,
 * и получает его печатную форму. Рисовальщик тот же, что у своих
 * документов кассы: вид документа один, и второго рисовальщика для него
 * заводить незачем.
 *
 * Своих документов это не касается: они лежат в журнале этой кассы
 * и рисуются по идентификатору.
 *
 * @property authorization проверка кассы и пина оператора.
 * @property renderer рисовальщик печатных форм кассы.
 */
class GetProtocolPrintHtmlUseCase(
    private val authorization: AuthorizeUserUseCase,
    private val renderer: ReceiptRenderPort
) {

    /**
     * Рисует документ пакета разметкой печатной формы.
     *
     * Порядок: касса, пин, разбор пакета, документ пакета, отрисовка.
     *
     * @param kkmId касса, которой рисуется документ: её оформление и её права.
     * @param pin пин кассира или администратора этой кассы; стандартный пин допускается,
     *   как и при входе на кассу.
     * @param packet тело пакета: объект с полями `request` и `response`.
     * @param layout ширина ленты; не задана — та, что настроена у кассы.
     * @return HTML печатной формы.
     * @throws io.github.texport.superkassa.core.domain.api.exception.NotFoundException касса не найдена.
     * @throws io.github.texport.superkassa.core.domain.api.exception.ForbiddenException пин не принят.
     * @throws ValidationException пакет не разобран или его команда документа не порождает.
     */
    fun execute(kkmId: String, pin: String, packet: String, layout: ReceiptLayoutType?): String {
        val kkm = authorization.requireKkm(kkmId)
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER), allowDefaultPin = true)
        val parsed = ProtocolPacket.of(packet)
            ?: throw ValidationException(CoreStrings.protocolPacketUnreadable(), PACKET_INVALID)
        val drawnBy = kkm.withProtocolRegistration(parsed.service)
        val document = documentOf(parsed, drawnBy)
            ?: throw ValidationException(CoreStrings.protocolPacketNotADocument(), PACKET_INVALID)
        return document.draw(renderer, drawnBy, layout)
    }

    private companion object {
        const val PACKET_INVALID = "PROTOCOL_PACKET_INVALID"
    }
}
