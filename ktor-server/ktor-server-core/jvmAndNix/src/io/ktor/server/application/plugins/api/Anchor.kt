/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.application.plugins.api

import io.ktor.server.application.*

@PluginsDslMarker
public interface AnchorContext {
    public fun install(application: Application)
}

@PluginsDslMarker
public interface Anchor<Context : AnchorContext> {
    public fun before(): Context
    public fun after(): Context
}
