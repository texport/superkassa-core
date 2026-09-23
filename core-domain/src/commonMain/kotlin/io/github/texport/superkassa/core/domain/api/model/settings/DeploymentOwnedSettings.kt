package io.github.texport.superkassa.core.domain.api.model.settings

/**
 * Настройки, которыми владеет запуск, а не сохранённая запись.
 *
 * Какой ОФД обслуживает кассу и по какой версии протокола она с ним
 * разговаривает, решает запуск: узел — свойствами, приложение —
 * конфигурацией сборки. Эти поля при старте перекрывают то, что лежит
 * в файле или в базе, и знание об этом объявлено здесь один раз:
 * прочитанные заново настройки иначе называли бы версию, на которой
 * касса не разговаривает.
 *
 * @receiver настройки, прочитанные из хранилища.
 * @param deployment настройки запуска.
 * @return настройки хранилища с полями запуска.
 */
fun CoreSettings.withDeploymentOwned(deployment: CoreSettings): CoreSettings = copy(
    ofdProviderId = deployment.ofdProviderId,
    ofdProtocolVersion = deployment.ofdProtocolVersion
)
