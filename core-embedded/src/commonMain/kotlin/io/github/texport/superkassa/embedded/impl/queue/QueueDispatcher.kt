package io.github.texport.superkassa.embedded.impl.queue

import kotlinx.coroutines.CoroutineDispatcher

/** Потоки для досылки: она ждёт сети и базы, и считающим потокам её не отдают. */
internal expect val queueDispatcher: CoroutineDispatcher
