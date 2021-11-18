/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("DEPRECATION")

package io.ktor.server.application.plugins.api

import io.ktor.server.application.*
import io.ktor.server.application.plugins.api.debug.*
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.ktor.util.debug.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.*
import kotlin.random.*

/**
 * A builder that is available inside a plugin creation block. It allows you to define handlers for different stages
 * (a.k.a. phases) of the HTTP pipeline.
 **/
@PluginsDslMarker
public interface PluginBuilder<PluginConfig : Any> {
    /**
     * Specifies how to modify HTTP call handling for the current [ApplicationPluginBuilder].
     * @see OnCall
     **/
    public fun handle(block: suspend HandleContext<PluginConfig>.(call: ApplicationCall) -> Unit)

    /**
     * Specifies how to modify HTTP call handling for the current [ApplicationPluginBuilder].
     * @see OnCall
     **/
    public fun onCall(block: suspend OnCallContext<PluginConfig>.(call: ApplicationCall) -> Unit)

    public fun onCallReceive(block: suspend CallReceiveContext<PluginConfig>.(ApplicationCall) -> Unit)

    public fun onCallRespond(block: suspend CallRespondContext<PluginConfig>.(ApplicationCall, body: Any) -> Unit)

    /**
     * Specifies how to modify sending data within an HTTP call for the current [ApplicationPluginBuilder].
     * @see OnCallRespond
     **/
    public val onCallRespond: OnCallRespond<PluginConfig>

    /**
     * Specifies a shutdown hook. This method is useful for closing resources allocated by the plugin.
     *
     * @param hook An action that needs to be executed when the application shuts down.
     **/
    public fun applicationShutdownHook(hook: (Application) -> Unit)
}
