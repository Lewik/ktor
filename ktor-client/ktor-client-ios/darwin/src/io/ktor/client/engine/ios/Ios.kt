/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.engine.ios

import io.ktor.client.engine.*
import io.ktor.util.*
import platform.Foundation.*

@Suppress("DEPRECATION")
@OptIn(ExperimentalStdlibApi::class)
@SharedImmutable
@EagerInitialization
private val initHook = Ios

/**
 * [HttpClientEngineFactory] using a [NSURLRequest] in implementation
 * with the associated requestConfig [HttpClientEngineConfig].
 */
@OptIn(InternalAPI::class)
public object Ios : HttpClientEngineFactory<IosClientEngineConfig> {
    init {
        engines.append(this)
    }

    override fun create(block: IosClientEngineConfig.() -> Unit): HttpClientEngine =
        IosClientEngine(IosClientEngineConfig().apply(block))

    override fun toString(): String = "Ios"
}
