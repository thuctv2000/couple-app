package dev.coupleapp.lovecounter.data.remote

import dev.coupleapp.lovecounter.domain.*
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal expect fun counterHttpClient(): HttpClient

class CounterRemoteConnection(baseUrl: String, allowLocalHttp: Boolean = false) {
    private val client = counterHttpClient()
    private val adapter = HttpCounterRemote(client, baseUrl, allowLocalHttp)
    val remote: CounterRemote = adapter
    val reader: CounterSnapshotReader = adapter
    fun close() = client.close()
}

internal fun io.ktor.client.HttpClientConfig<*>.counterDefaults() {
    expectSuccess = false
    followRedirects = false
    install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 10_000; socketTimeoutMillis = 15_000 }
}

class HttpCounterRemote(private val client: HttpClient, baseUrl: String, allowLocalHttp: Boolean = false) : CounterRemote, CounterSnapshotReader {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val endpoint: String
    init {
        val url = Url(baseUrl)
        require(url.protocol == URLProtocol.HTTPS || (allowLocalHttp && url.protocol == URLProtocol.HTTP && url.host in setOf("127.0.0.1", "localhost", "10.0.2.2", "::1"))) { "https_required" }
        require(url.user == null && url.password == null && url.parameters.isEmpty() && url.fragment.isEmpty() && url.encodedPath in setOf("", "/")) { "invalid_api_origin" }
        endpoint = baseUrl.trimEnd('/') + "/v1/me/counter"
    }

    override suspend fun read(credential: CounterCredential): CounterReadResult {
        if (credential.uid.isBlank() || credential.idToken.isBlank()) return CounterReadResult.Unauthorized
        return try {
            val response = client.get(endpoint) { bearerAuth(credential.idToken) }
            when (response.status.value) {
                200 -> try {
                    val counter = json.decodeFromString<ServerCounter>(response.bodyAsText())
                    require(counter.version > 0)
                    CounterReadResult.Found(CounterSnapshot(credential.uid, counter.version, counter.payload.toDomain()))
                } catch (e: CancellationException) { throw e
                } catch (_: Exception) { CounterReadResult.InvalidResponse }
                404 -> CounterReadResult.Missing
                401 -> CounterReadResult.Unauthorized
                403 -> CounterReadResult.Forbidden
                429, in 500..599 -> CounterReadResult.RetryLater
                else -> CounterReadResult.InvalidResponse
            }
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) { CounterReadResult.RetryLater }
    }

    override suspend fun apply(credential: CounterCredential, operation: PreparedCounterOperation): CounterRemoteResult {
        if (credential.uid != operation.firebaseUid || credential.idToken.isBlank()) return CounterRemoteResult.Forbidden
        return try {
            val response = client.put(endpoint) {
                bearerAuth(credential.idToken)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(OperationBody(operation.operationId, operation.baseServerVersion, operation.payloadVersion, Payload.from(operation.payload))))
            }
            when (response.status.value) {
                200 -> try {
                    val ack = json.decodeFromString<Acknowledgement>(response.bodyAsText())
                    CounterRemoteResult.Accepted(ack.operationId, ack.counter.version, ack.counter.payload.toDomain())
                } catch (e: CancellationException) { throw e
                } catch (_: Exception) { CounterRemoteResult.InvalidResponse }
                401 -> CounterRemoteResult.Unauthorized
                403 -> CounterRemoteResult.Forbidden
                409 -> try {
                    when (json.decodeFromString<ApiError>(response.bodyAsText()).error) {
                        "version_conflict" -> CounterRemoteResult.Conflict
                        "operation_id_reused" -> CounterRemoteResult.InvalidOperation
                        else -> CounterRemoteResult.InvalidResponse
                    }
                } catch (e: CancellationException) { throw e
                } catch (_: Exception) { CounterRemoteResult.InvalidResponse }
                400, 422 -> CounterRemoteResult.InvalidOperation
                429, in 500..599 -> CounterRemoteResult.RetryLater
                else -> CounterRemoteResult.InvalidResponse
            }
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) { CounterRemoteResult.RetryLater }
    }
}

@Serializable private data class OperationBody(val operationId: String, val baseVersion: Long, val payloadVersion: Int, val payload: Payload)
@Serializable private data class Acknowledgement(val operationId: String, val counter: ServerCounter)
@Serializable private data class ApiError(val error: String)
@Serializable private data class ServerCounter(val version: Long, val payload: Payload)
@Serializable private data class Payload(val displayName: String, val partnerName: String, val startDate: String, val timeZoneId: String) {
    fun toDomain(): CounterDraft {
        require(displayName == displayName.trim() && displayName.length <= 80)
        require(partnerName == partnerName.trim() && partnerName.length <= 80)
        TimeZone.of(timeZoneId)
        return CounterDraft(displayName, partnerName, LocalDate.parse(startDate), timeZoneId)
    }
    companion object {
        fun from(value: CounterDraft) = Payload(value.displayName, value.partnerName, value.startDate.toString(), value.timeZoneId)
    }
}
