package io.github.texport.superkassa.offlinequeue.impl

import io.github.texport.superkassa.core.string.api.TrilingualMessage

/**
 * Исключение, выбрасываемое при неверном статусе отправки задачи из очереди.
 */
internal class QueueDispatchException(val error: TrilingualMessage) : IllegalArgumentException(error.compact())
