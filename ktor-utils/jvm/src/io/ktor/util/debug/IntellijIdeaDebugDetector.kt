/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.util.debug

import java.lang.management.*
import java.lang.ClassNotFoundException

internal actual object IntellijIdeaDebugDetector {
    actual val isDebuggerConnected: Boolean by lazy {
        try {
            ManagementFactory.getRuntimeMXBean()
                .inputArguments.toString()
                .contains("jdwp")
        } catch (error: ClassNotFoundException) {
            // Relevant case for Android (KTOR-3426) in tessts. Android does not support ManagementFactory
            false
        }
    }
}
