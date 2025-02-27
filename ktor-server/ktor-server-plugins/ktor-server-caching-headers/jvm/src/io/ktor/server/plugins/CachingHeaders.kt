/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

package io.ktor.server.plugins

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.ktor.util.pipeline.*

/**
 * A plugin that adds the capability to configure the Cache-Control and Expires headers using [CachingOptions].
 * It invokes [optionsProviders] for every response and use first non-null [CachingOptions]
 */
public class CachingHeaders private constructor(
    private val optionsProviders: List<(OutgoingContent) -> CachingOptions?>
) {
    /**
     * A configuration for the [CachingHeaders] plugin
     */
    public class Configuration {
        internal val optionsProviders = mutableListOf<(OutgoingContent) -> CachingOptions?>()

        init {
            optionsProviders.add { content -> content.caching }
        }

        /**
         * Registers a function that can provide caching options for a given [OutgoingContent]
         */
        public fun options(provider: (OutgoingContent) -> CachingOptions?) {
            optionsProviders.add(provider)
        }
    }

    internal fun interceptor(context: PipelineContext<Any, ApplicationCall>, message: Any) {
        val call = context.call
        val options = if (message is OutgoingContent) optionsFor(message) else emptyList()

        if (options.isNotEmpty()) {
            val headers = Headers.build {
                options.mapNotNull { it.cacheControl }
                    .mergeCacheControlDirectives()
                    .ifEmpty { null }?.let { directives ->
                        append(HttpHeaders.CacheControl, directives.joinToString(separator = ", "))
                    }
                options.firstOrNull { it.expires != null }?.expires?.let { expires ->
                    append(HttpHeaders.Expires, expires.toHttpDate())
                }
            }

            val responseHeaders = call.response.headers
            headers.forEach { name, values ->
                values.forEach { responseHeaders.append(name, it) }
            }
        }
    }

    /**
     * Retrieves caching options for a given content
     */
    public fun optionsFor(content: OutgoingContent): List<CachingOptions> {
        return optionsProviders.mapNotNullTo(ArrayList(optionsProviders.size)) { it(content) }
    }

    /**
     * `ApplicationPlugin` implementation for [ConditionalHeaders]
     */
    public companion object Plugin : RouteScopedPlugin<Configuration, CachingHeaders> {
        override val key: AttributeKey<CachingHeaders> = AttributeKey("Caching Headers")
        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): CachingHeaders {
            val configuration = Configuration().apply(configure)
            val plugin = CachingHeaders(configuration.optionsProviders)

            pipeline.sendPipeline.intercept(ApplicationSendPipeline.After) { message ->
                plugin.interceptor(
                    this,
                    message
                )
            }

            return plugin
        }
    }
}
