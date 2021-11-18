/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

package io.ktor.server.plugins

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.application.plugins.api.*
import io.ktor.server.response.*
import io.ktor.util.reflect.*
import kotlin.native.concurrent.*
import kotlin.reflect.*

public typealias HandlerFunction = suspend (call: ApplicationCall, cause: Throwable) -> Unit

/**
 * Status pages plugin config.
 */
public class StatusPagesConfig {
    /**
     * Exception handlers map by exception class.
     */
    public val exceptions: MutableMap<KClass<*>, HandlerFunction> = mutableMapOf()

    /**
     * Status handlers by status code
     */
    public val statuses: MutableMap<HttpStatusCode, suspend (call: ApplicationCall, code: HttpStatusCode) -> Unit> =
        mutableMapOf()

    /**
     * Register exception [handler] for exception type [T] and it's children
     */
    public inline fun <reified T : Throwable> exception(
        noinline handler: suspend (call: ApplicationCall, T) -> Unit
    ): Unit = exception(T::class, handler)

    /**
     * Register exception [handler] for exception class [klass] and it's children
     */
    public fun <T : Throwable> exception(
        klass: KClass<T>,
        handler: suspend (call: ApplicationCall, T) -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        val cast = handler as suspend (ApplicationCall, Throwable) -> Unit

        exceptions[klass] = cast
    }

    /**
     * Register status [handler] for [status] code
     */
    public fun status(
        vararg status: HttpStatusCode,
        handler: suspend (ApplicationCall, HttpStatusCode) -> Unit
    ) {
        status.forEach {
            statuses[it] = handler
        }
    }
}

@SharedImmutable
public val StatusPages: ApplicationPlugin<Application, StatusPagesConfig, PluginInstance> = createApplicationPlugin(
    "StatusPages",
    { StatusPagesConfig() }
) {
    val exceptions = HashMap(pluginConfig.exceptions)
    val statuses = HashMap(pluginConfig.statuses)

    fun findHandlerByValue(cause: Throwable): HandlerFunction? {
        val key = exceptions.keys.find { cause.instanceOf(it) } ?: return null
        return exceptions[key]
    }

    onCallRespond { call: ApplicationCall, body: Any ->
        val status = when (body) {
            is OutgoingContent -> body.status
            is HttpStatusCode -> body
            else -> null
        } ?: return@onCallRespond

        val handler = statuses[status] ?: return@onCallRespond
        handler(call, status)
    }

    on(CallFailed) { call, cause ->
        val status = call.response.status()
        if (status != null) return@on
        val handler = findHandlerByValue(cause) ?: throw cause
        handler(call, cause)
    }
}
