package io.github.texport.superkassa.testing.api.bfd

/**
 * Налогоплательщик и точка, которые [FakeBfd] сообщает кассе в ответ
 * на запрос сведений: с ними касса заводится и печатает шапку чека.
 *
 * @property title наименование налогоплательщика.
 * @property bin БИН или ИИН, 12 цифр.
 * @property oked код ОКЭД основного вида деятельности.
 * @property address адрес точки на русском.
 * @property addressKz адрес точки на казахском.
 */
data class BfdOrganization(
    val title: String = "ТОО Дала",
    val bin: String = "123456789012",
    val oked: String = "47111",
    val address: String = "Алматы, пр. Абая, 1",
    val addressKz: String = "Алматы, Абай даңғылы, 1"
)
