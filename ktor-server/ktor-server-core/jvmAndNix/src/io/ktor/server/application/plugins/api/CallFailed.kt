/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.application.plugins.api

import io.ktor.server.application.*

public object CallFailed : Event<suspend (call: ApplicationCall, cause: Throwable) -> Unit> {

    override fun install(application: Application, config: suspend (call: ApplicationCall, cause: Throwable) -> Unit) {
        application.intercept(ApplicationCallPipeline.Monitoring) {
            try {
                proceed()
            } catch (cause: Throwable) {
                config(call, cause)
            }
        }
    }
}
