/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

@file:Suppress("DEPRECATION_ERROR")

package io.ktor.features

@Deprecated(
    message = "Moved to io.ktor.server.plugins",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("CachedTransformationResult", "io.ktor.server.plugins.*")
)
public class StatusPages
