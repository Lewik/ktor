/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.application.plugins.api

import io.ktor.server.application.*

/**
 * Descendents of [OnCallReceive] allow you to extend the process of sending a response to the client.
 *
 * Example:
 *
 * ```
 * onCallRespond { call ->
 *      println(call.request.uri)
 * }
 *
 * onCallRespond.afterTransform { call, content ->
 *      println("sending $content to a client")
 * }
 * ```
 *
 * This prints a URL once you execute call.respond() on your server
 * and also prints raw content that is going to be sent to the client.
 **/
@PluginsDslMarker
public interface OnCallRespond<PluginConfig : Any> {
    /**
     * Allows you to execute your code after response transformation has been made.
     * @see StatusPages
     *
     * @param block An action that needs to be executed after transformation of the response body.
     **/
    public fun afterTransform(
        block: suspend CallRespondAfterTransformContext<PluginConfig>.(call: ApplicationCall, responseBody: Any) -> Unit
    )
}
