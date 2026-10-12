package dev.coupleapp.lovecounter.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun counterHttpClient() = HttpClient(OkHttp) { counterDefaults() }
