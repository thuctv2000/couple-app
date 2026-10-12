package dev.coupleapp.lovecounter.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun counterHttpClient() = HttpClient(Darwin) { counterDefaults() }
