// ktlint-disable filename
/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.testing.client

import io.ktor.client.engine.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

internal actual class TestHttpClientEngineBridge actual constructor(
    private val engine: TestHttpClientEngine,
    private val app: TestApplicationEngine
) {

    actual val supportedCapabilities = setOf<HttpClientEngineCapability<*>>(WebSocketCapability)

    @OptIn(InternalAPI::class)
    actual suspend fun runWebSocketRequest(
        url: String,
        headers: Headers,
        content: OutgoingContent,
        coroutineContext: CoroutineContext
    ): Pair<TestApplicationCall, WebSocketSession> {
        val sessionDeferred = CompletableDeferred<WebSocketSession>()
        val call = app.handleWebSocketConversation(
            url,
            { with(engine) { appendRequestHeaders(headers, content) } },
            awaitCallback = false,
        ) { incoming, outgoing ->
            val session = TestEngineWebsocketSession(coroutineContext, incoming, outgoing)
            sessionDeferred.complete(session)
            session.run()
        }
        return call to sessionDeferred.await()
    }
}
