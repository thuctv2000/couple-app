package dev.coupleapp.lovecounter.data.local

import dev.coupleapp.lovecounter.data.remote.HttpCounterRemote
import dev.coupleapp.lovecounter.domain.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.*
import kotlin.test.*

class HttpCounterRemoteTest {
    private val op = PreparedCounterOperation("91cdbf3b-460f-4230-97d8-cd6df142f399", "uid-a", 0, 1, 1, CounterDraft("Linh", "Nam", LocalDate(2020, 1, 1), "UTC"))
    private val credential = CounterCredential("uid-a", "test-token")
    @Test fun readsSnapshotInVerifiedCredentialScope() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/v1/me/counter", request.url.encodedPath)
            assertEquals("Bearer test-token", request.headers[HttpHeaders.Authorization])
            assertTrue(request.url.parameters.isEmpty())
            respond("""{"version":9,"payload":{"displayName":"Server name","partnerName":"Nam","startDate":"2020-01-01","timeZoneId":"UTC"}}""")
        })
        try {
            val found = assertIs<CounterReadResult.Found>(HttpCounterRemote(client, "https://api.example.test").read(credential))
            assertEquals("uid-a", found.snapshot.firebaseUid)
            assertEquals(9L, found.snapshot.serverVersion)
            assertEquals("Server name", found.snapshot.payload.displayName)
        } finally { client.close() }
    }
    @Test fun missingSnapshotIsDistinctFromFailureAndMalformedData() = runBlocking {
        for ((status, body, expected) in listOf(
            Triple(404, "{}", CounterReadResult.Missing),
            Triple(401, "{}", CounterReadResult.Unauthorized),
            Triple(403, "{}", CounterReadResult.Forbidden),
            Triple(503, "{}", CounterReadResult.RetryLater),
            Triple(200, "{}", CounterReadResult.InvalidResponse),
            Triple(200, """{"version":0,"payload":{"displayName":"A","partnerName":"","startDate":"2020-01-01","timeZoneId":"UTC"}}""", CounterReadResult.InvalidResponse),
            Triple(200, """{"version":2,"payload":{"displayName":"A","partnerName":"","startDate":"2020-01-01","timeZoneId":"invalid-zone"}}""", CounterReadResult.InvalidResponse),
        )) {
            val client = HttpClient(MockEngine { respond(body, HttpStatusCode.fromValue(status)) })
            try { assertEquals(expected, HttpCounterRemote(client, "https://api.example.test").read(credential)) }
            finally { client.close() }
        }
    }
    @Test fun requestMatchesGoContractAndParsesAck() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Put, request.method)
            assertEquals("https://api.example.test/v1/me/counter", request.url.toString())
            assertEquals("Bearer test-token", request.headers[HttpHeaders.Authorization])
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals(setOf("operationId", "baseVersion", "payloadVersion", "payload"), body.keys)
            assertEquals(op.operationId, body.getValue("operationId").jsonPrimitive.content)
            assertEquals(0, body.getValue("baseVersion").jsonPrimitive.int)
            respond("""{"operationId":"${op.operationId}","counter":{"version":1,"payload":{"displayName":"Linh","partnerName":"Nam","startDate":"2020-01-01","timeZoneId":"UTC"}},"replay":true}""", HttpStatusCode.OK)
        })
        try {
            assertEquals(CounterRemoteResult.Accepted(op.operationId, 1, op.payload), HttpCounterRemote(client, "https://api.example.test").apply(credential, op))
        } finally { client.close() }
    }
    @Test fun errorStatusesAndMalformedAckStayDistinct() = runBlocking {
        for ((status, expected) in listOf(
            200 to CounterRemoteResult.InvalidResponse, 401 to CounterRemoteResult.Unauthorized,
            403 to CounterRemoteResult.Forbidden, 409 to CounterRemoteResult.Conflict,
            400 to CounterRemoteResult.InvalidOperation, 429 to CounterRemoteResult.RetryLater,
            503 to CounterRemoteResult.RetryLater, 302 to CounterRemoteResult.InvalidResponse,
        )) {
            val client = HttpClient(MockEngine { respond(if (status == 409) """{"error":"version_conflict"}""" else "{}", HttpStatusCode.fromValue(status)) }) { followRedirects = false }
            try { assertEquals(expected, HttpCounterRemote(client, "https://api.example.test").apply(credential, op)) }
            finally { client.close() }
        }
    }
    @Test fun rejectsUnsafeOriginAndNeverSendsAcrossAccount() = runBlocking {
        var calls = 0
        val client = HttpClient(MockEngine { calls++; respond("{}") })
        try {
            assertFailsWith<IllegalArgumentException> { HttpCounterRemote(client, "http://example.test", true) }
            assertFailsWith<IllegalArgumentException> { HttpCounterRemote(client, "https://example.test?token=x") }
            val remote = HttpCounterRemote(client, "http://10.0.2.2:8080", true)
            assertEquals(CounterRemoteResult.Forbidden, remote.apply(CounterCredential("uid-b", "other"), op))
            assertEquals(0, calls)
        } finally { client.close() }
    }
}
