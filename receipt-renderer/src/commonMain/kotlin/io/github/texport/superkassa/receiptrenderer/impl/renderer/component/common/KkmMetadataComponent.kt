package io.github.texport.superkassa.receiptrenderer.impl.renderer.component.common

import io.github.texport.superkassa.core.domain.model.kkm.*

import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.escaped

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
        val kkmIdLabel = translateInlineKey("kkm_id")
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
                    <tr><td>$kkmIdLabel</td><td style="word-break: break-all; white-space: normal;">${kkm.id.escaped()}</td></tr>
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
