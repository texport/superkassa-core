package io.github.texport.superkassa.embedded.api

import io.github.texport.superkassa.core.presentation.api.DeliveryApi
import io.github.texport.superkassa.core.presentation.api.OfflineQueueApi
import io.github.texport.superkassa.core.presentation.api.PrintApi
import io.github.texport.superkassa.core.presentation.api.SettingsApi
import io.github.texport.superkassa.core.presentation.api.SuperkassaApi

/**
 * Касса, работающая в процессе приложения.
 *
 * Одна касса владеет одним каталогом данных: второй экземпляр на том же
 * каталоге не создаётся, пока первый не закрыт. Все вызовы фасадов
 * блокирующие — из главного потока приложения их не делают.
 *
 * Пока касса открыта, она сама досылает автономную очередь в ОФД
 * и закрывает смену до предела в сутки, если у кассы включено автозакрытие.
 */
interface Superkassa : AutoCloseable {
    /** Кассовые и фискальные операции. */
    val api: SuperkassaApi

    /**
     * Печатные формы: HTML, PDF, PNG — по виду и идентификатору, по одному
     * идентификатору документа и по пакету протокола. Тот же объект, что [api].
     */
    val print: PrintApi

    /** Настройки ядра: чтение и правка по правилам ядра. */
    val settings: SettingsApi

    /** Автономная очередь документов. */
    val queue: OfflineQueueApi

    /** Повтор доставки чека покупателю. */
    val delivery: DeliveryApi

    /** Печать на принтер системы. */
    val printer: DocumentPrinter

    /** Останавливает досылку очереди и автозакрытие, закрывает базу и освобождает каталог. */
    override fun close()
}
