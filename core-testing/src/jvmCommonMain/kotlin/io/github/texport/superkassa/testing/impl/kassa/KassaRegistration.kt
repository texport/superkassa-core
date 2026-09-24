package io.github.texport.superkassa.testing.impl.kassa

import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmInitSimpleRequest
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.TaxRegime
import io.github.texport.superkassa.core.presentation.api.model.kkm.VatGroup
import io.github.texport.superkassa.core.presentation.api.model.user.UserCreateRequest
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import io.github.texport.superkassa.testing.api.kassa.KassaSetup
import io.github.texport.superkassa.testing.api.kassa.VatMode

/**
 * Заведение кассы тем же путём, что у владельца: упрощённая регистрация
 * по номеру и токену БФД, налоговый режим в режиме программирования,
 * название и кассир — всё через фасад.
 */
internal class KassaRegistration(private val api: SuperkassaApi, private val firstToken: Long) {
    /** Заводит кассу [setup] под номером [systemId] в БФД. */
    fun register(setup: KassaSetup, systemId: Long): KkmResponse {
        val kkm = api.initKkmSimple(
            KkmInitSimpleRequest(
                ofdId = setup.ofdProvider,
                ofdEnvironment = ENVIRONMENT,
                ofdSystemId = systemId.toString(),
                ofdToken = firstToken.toString(),
                defaultVatGroup = setup.vat.defaultVat,
                adminPin = setup.adminPin
            )
        )
        val regime = regimeOf(setup.vat)
        if (kkm.taxRegime != regime.name) retax(kkm.kkmId, setup.adminPin, regime, setup.vat.defaultVat)
        setup.name?.let { api.updateKkmName(kkm.kkmId, setup.adminPin, it) }
        api.createUser(
            kkm.kkmId,
            setup.adminPin,
            UserCreateRequest(setup.cashierName, UserRole.CASHIER, setup.cashierPin)
        )
        return api.getKkm(kkm.kkmId)
    }

    /**
     * Упрощённая регистрация выводит режим из ставки: без НДС — не плательщик,
     * иначе плательщик. Смешанный режим и плательщик со ставкой «без НДС»
     * задаются потом, как это сделал бы администратор.
     */
    private fun retax(kkmId: String, adminPin: String, regime: TaxRegime, vat: VatGroup) {
        api.enterProgramming(kkmId, adminPin)
        api.updateTaxSettings(kkmId, adminPin, regime, vat)
        api.exitProgramming(kkmId, adminPin)
    }

    private fun regimeOf(vat: VatMode): TaxRegime = when (vat) {
        VatMode.NotPayer -> TaxRegime.NO_VAT
        is VatMode.Payer -> TaxRegime.VAT_PAYER
        is VatMode.Mixed -> TaxRegime.MIXED
    }

    private companion object {
        const val ENVIRONMENT = "TEST"
    }
}
