/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.application.plugins.api

import io.ktor.server.application.*

@PluginsDslMarker
public interface Event<Config> {
    public fun install(application: Application, config: Config)
}
