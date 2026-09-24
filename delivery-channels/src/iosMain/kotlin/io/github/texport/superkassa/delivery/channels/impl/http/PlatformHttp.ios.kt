package io.github.texport.superkassa.delivery.channels.impl.http

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

internal actual fun platformHttpEngine(): HttpClientEngineFactory<*> = Darwin
