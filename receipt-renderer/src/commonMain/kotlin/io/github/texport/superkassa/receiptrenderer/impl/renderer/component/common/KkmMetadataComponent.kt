package io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common

import io.github.texport.superkassa.core.domain.api.model.kkm.*

import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.escaped

/**
 * Блок сведений о кассе на ленте.
 *
 * Кассу на чеке называют регистрационный и заводской номера — они и есть
 * обязательные реквизиты (требования к содержанию чека, пункты 56.4
 * и 56.5). Внутренний идентификатор узла отсюда убран: покупателю он
 * ничего не говорит, в требованиях его нет, а на ленте 58 мм он занимал
 * две строки под тридцать шесть знаков.
 */
internal object KkmMetadataComponent {
    fun render(
        kkm: KkmInfo,
        shiftNo: Long?,
        docNo: String?,
        formattedDateTime: String,
        additionalMeta: List<Pair<String, String>>,
        translateInlineKey: (String) -> String
    ): String {
        val kkmInfoLabel = translateInlineKey("kkm_info")
        val rnmLabel = translateInlineKey("rnm")
        val znmLabel = translateInlineKey("znm")
        val shiftNoLabel = translateInlineKey("shift_no")
        val docNoLabel = translateInlineKey("doc_no")
        val dateTimeLabel = translateInlineKey("date_time")

        val regNo = kkm.registrationNumber ?: "-"
        val factNo = kkm.factoryNumber ?: "-"

        val docNoRow = if (docNo != null) {
            "<tr><td>$docNoLabel</td><td style=\"word-break: break-all; white-space: normal;\">$docNo</td></tr>"
        } else {
            ""
        }

        val addMetaRows = additionalMeta.joinToString("") { (k, v) ->
            "<tr><td>$k</td><td style=\"word-break: break-all; white-space: normal;\">$v</td></tr>"
        }

        return """
            <fieldset class="section-card">
                <legend class="card-label">$kkmInfoLabel</legend>
                <table class="meta-table" style="margin-top: 4px;">
                    <tr><td>$rnmLabel</td><td>${regNo.escaped()}</td></tr>
                    <tr><td>$znmLabel</td><td>${factNo.escaped()}</td></tr>
                    <tr><td>$shiftNoLabel</td><td>${shiftNo ?: "-"}</td></tr>
                    $docNoRow
                    <tr><td>$dateTimeLabel</td><td>$formattedDateTime</td></tr>
                    $addMetaRows
                </table>
            </fieldset>
        """.trimIndent()
    }
}
