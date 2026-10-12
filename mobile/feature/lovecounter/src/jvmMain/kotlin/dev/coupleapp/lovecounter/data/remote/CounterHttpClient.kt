package dev.coupleapp.lovecounter.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

internal actual fun counterHttpClient() = HttpClient(CIO) { counterDefaults() }
