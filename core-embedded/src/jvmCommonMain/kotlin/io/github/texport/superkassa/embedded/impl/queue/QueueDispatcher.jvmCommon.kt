package io.github.texport.superkassa.embedded.impl.queue

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val queueDispatcher: CoroutineDispatcher = Dispatchers.IO
