/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.application.plugins.api

import io.ktor.server.application.*
import io.ktor.server.application.plugins.api.debug.*
import io.ktor.server.application.plugins.api.debug.PHASE_ON_CALL
import io.ktor.server.application.plugins.api.debug.PHASE_ON_CALL_RECEIVE
import io.ktor.server.application.plugins.api.debug.PHASE_ON_CALL_RESPOND
import io.ktor.server.application.plugins.api.debug.PHASE_ON_CALL_RESPOND_AFTER
import io.ktor.server.application.plugins.api.debug.ijDebugReportHandlerFinished
import io.ktor.server.application.plugins.api.debug.ijDebugReportHandlerStarted
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.ktor.util.debug.*
import io.ktor.util.pipeline.*
import kotlin.random.*

/**
 * A plugin that embeds into the HTTP pipeline and extends Ktor functionality.
 **/
public abstract class ApplicationPluginBuilder<PluginConfig : Any> internal constructor(
    internal val key: AttributeKey<PluginInstance>
) : PluginBuilder<PluginConfig> {

    public abstract val pluginConfig: PluginConfig

    internal abstract val application: Application

    /**
     * A pipeline PluginConfig for the current plugin. See [Pipelines](https://ktor.io/docs/pipelines.html)
     * for more information.
     **/
    internal abstract val pipeline: ApplicationCallPipeline

    /**
     * Allows you to access the environment of the currently running application where the plugin is installed.
     **/
    public val environment: ApplicationEnvironment? get() = pipeline.environment

    /**
     * Configuration of your current application (incl. host, port and anything else you can define in application.conf).
     **/
    public val applicationConfig: ApplicationConfig? get() = environment?.config

    internal val callInterceptions: MutableList<CallInterception> = mutableListOf()

    internal val onReceiveInterceptions: MutableList<ReceiveInterception> = mutableListOf()

    internal val onResponseInterceptions: MutableList<ResponseInterception> = mutableListOf()

    internal val afterResponseInterceptions: MutableList<ResponseInterception> = mutableListOf()

    internal val pipelineHandlers: MutableList<PipelineHandler> = mutableListOf()

    public override fun handle(block: suspend HandleContext<PluginConfig>.(call: ApplicationCall) -> Unit) {
        onDefaultPhase(
            callInterceptions,
            ApplicationCallPipeline.Plugins,
            PHASE_ON_CALL,
            ::HandleContext
        ) { call, _ ->
            if (call.response.status() == null) {
                block(call)
            }
        }
    }

    /**
     * Defines how processing an HTTP call needs to be modified by the current [ApplicationPluginBuilder].
     *
     * @param block An action that needs to be executed when your application receives an HTTP call.
     **/
    public override fun onCall(block: suspend OnCallContext<PluginConfig>.(call: ApplicationCall) -> Unit) {
        onDefaultPhase(
            callInterceptions,
            ApplicationCallPipeline.Plugins,
            PHASE_ON_CALL,
            ::OnCallContext
        ) { call, _ ->
            block(call)
        }
    }

    /**
     * Defines how current [ApplicationPluginBuilder] needs to transform data received from a client.
     *
     * @param block An action that needs to be executed when your application receives data from a client.
     **/
    public override fun onCallReceive(block: suspend CallReceiveContext<PluginConfig>.(ApplicationCall) -> Unit) {
        onDefaultPhase(
            onReceiveInterceptions,
            ApplicationReceivePipeline.Transform,
            PHASE_ON_CALL_RECEIVE,
            ::CallReceiveContext,
        ) { call, _ -> block(call) }
    }

    /**
     * Specifies how to transform the data. For example, you can write a custom serializer using this method.
     *
     * @param block An action that needs to be executed when your server is sending a response to a client.
     **/
    public override fun onCallRespond(
        block: suspend CallRespondContext<PluginConfig>.(call: ApplicationCall, body: Any) -> Unit
    ) {
        onDefaultPhase(
            onResponseInterceptions,
            ApplicationSendPipeline.Transform,
            PHASE_ON_CALL_RESPOND,
            ::CallRespondContext,
            block
        )
    }

    /**
     * Executes specific actions after all [targetPlugins] are executed.
     *
     * @param targetPlugins Plugins that need to be executed before your current [ApplicationPluginBuilder].
     * @param build Defines the code of your plugin that needs to be executed after [targetPlugins].
     *
     * Note: you can define multiple actions inside a [build] callback for multiple stages of handling an HTTP call
     * (such as [onCall], [onCallRespond], and so on). These actions are executed right after all actions defined
     * by the given [plugin] are already executed in the same stage.
     **/
    public fun after(
        vararg targetPlugins: Plugin<*, *, PluginInstance>,
        build: AfterPluginsBuilder<PluginConfig>.() -> Unit
    ) {
        pipelineHandlers.add { pipeline ->
            AfterPluginsBuilder(this, targetPlugins.map { pipeline.plugin(it).builder }).build()
        }
    }

    /**
     * Executes specific actions before all [targetPlugins] are executed.
     *
     * @param targetPlugins Plugins that need to be executed after your current [ApplicationPluginBuilder].
     * @param build Defines the code of your plugin that needs to be executed before [targetPlugins].
     *
     * Note: you can define multiple actions inside a [build] callback for multiple stages of handling an HTTP call
     * (such as [onCall], [onCallRespond], and so on) and each of these actions will be executed right before all actions defined
     * by the given [targetPlugins] were already executed in the same stage.
     **/
    public fun before(
        vararg targetPlugins: Plugin<*, *, PluginInstance>,
        build: BeforePluginsBuilder<PluginConfig>.() -> Unit
    ) {
        pipelineHandlers.add { pipeline ->
            BeforePluginsBuilder(this, targetPlugins.map { pipeline.plugin(it).builder }).build()
        }
    }

    public fun <EventConfig> on(
        event: Event<EventConfig>,
        config: EventConfig
    ) {
        event.install(application, config)
    }

    override fun applicationShutdownHook(hook: (Application) -> Unit) {
        environment?.monitor?.subscribe(ApplicationStopped) { app ->
            hook(app)
        }
    }

    /**
     * Specifies how to modify sending data within an HTTP call for the current [ApplicationPluginBuilder].
     * @see OnCallRespond
     **/
    public override val onCallRespond: OnCallRespond<PluginConfig> = object : OnCallRespond<PluginConfig> {
        private val plugin: ApplicationPluginBuilder<PluginConfig> = this@ApplicationPluginBuilder

        override fun afterTransform(
            block: suspend CallRespondAfterTransformContext<PluginConfig>.(ApplicationCall, Any) -> Unit
        ) {
            plugin.onDefaultPhaseWithMessage(
                plugin.afterResponseInterceptions,
                ApplicationSendPipeline.After,
                PHASE_ON_CALL_RESPOND_AFTER,
                ::CallRespondAfterTransformContext,
                block
            )
        }
    }

    private fun <T : Any, ContextT : CallContext<PluginConfig>> onDefaultPhaseWithMessage(
        interceptions: MutableList<Interception<T>>,
        phase: PipelinePhase,
        handlerName: String,
        contextInit: (pluginConfig: PluginConfig, PipelineContext<T, ApplicationCall>) -> ContextT,
        block: suspend ContextT.(ApplicationCall, Any) -> Unit
    ) {
        interceptions.add(
            Interception(
                phase,
                action = { pipeline ->
                    pipeline.intercept(phase) {
                        // Information about the plugin name is needed for Intellij Idea debugger.
                        addToContextInDebugMode(PluginName(key.name)) {
                            ijDebugReportHandlerStarted(pluginName = key.name, handler = handlerName)

                            // Perform current plugin's handler
                            contextInit(pluginConfig, this@intercept).block(call, subject)

                            ijDebugReportHandlerFinished(pluginName = key.name, handler = handlerName)
                        }
                    }
                }
            )
        )
    }

    private fun <T : Any, ContextT : CallContext<PluginConfig>> onDefaultPhase(
        interceptions: MutableList<Interception<T>>,
        phase: PipelinePhase,
        handlerName: String,
        contextInit: (pluginConfig: PluginConfig, PipelineContext<T, ApplicationCall>) -> ContextT,
        block: suspend ContextT.(call: ApplicationCall, body: Any) -> Unit
    ) {
        onDefaultPhaseWithMessage(interceptions, phase, handlerName, contextInit) { call, body -> block(call, body) }
    }

    internal fun newPhase(): PipelinePhase = PipelinePhase("${key.name}Phase${Random.nextInt()}")
}
