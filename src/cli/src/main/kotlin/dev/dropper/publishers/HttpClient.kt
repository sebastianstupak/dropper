package dev.dropper.publishers

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

/**
 * HTTP client wrapper for platform APIs
 * This allows us to mock HTTP calls in tests
 */
interface HttpClient {
    fun post(url: String, headers: Map<String, String>, body: String): HttpResponse
    fun postMultipart(url: String, headers: Map<String, String>, parts: Map<String, Any>): HttpResponse
    fun get(url: String, headers: Map<String, String>): HttpResponse
}

/**
 * HTTP response
 */
data class HttpResponse(
    val code: Int,
    val body: String,
    val success: Boolean = code in 200..299,
    val headers: Map<String, String> = emptyMap()
)

/**
 * Real HTTP client implementation using OkHttp
 */
class OkHttpClientImpl : HttpClient {
    private val client = OkHttpClient()

    override fun post(url: String, headers: Map<String, String>, body: String): HttpResponse {
        val requestBody = body.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (key, value) -> addHeader(key, value) } }
            .post(requestBody)
            .build()

        return executeRequest(request)
    }

    override fun postMultipart(url: String, headers: Map<String, String>, parts: Map<String, Any>): HttpResponse {
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .apply {
                parts.forEach { (key, value) ->
                    when (value) {
                        is File -> {
                            addFormDataPart(
                                key,
                                value.name,
                                value.asRequestBody("application/octet-stream".toMediaType())
                            )
                        }
                        is String -> addFormDataPart(key, value)
                        else -> addFormDataPart(key, value.toString())
                    }
                }
            }
            .build()

        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (key, value) -> addHeader(key, value) } }
            .post(multipartBody)
            .build()

        return executeRequest(request)
    }

    override fun get(url: String, headers: Map<String, String>): HttpResponse {
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (key, value) -> addHeader(key, value) } }
            .get()
            .build()

        return executeRequest(request)
    }

    private fun executeRequest(request: Request): HttpResponse {
        return try {
            client.newCall(request).execute().use { response ->
                HttpResponse(
                    code = response.code,
                    body = response.body?.string() ?: "",
                    success = response.isSuccessful
                )
            }
        } catch (e: IOException) {
            HttpResponse(code = 0, body = "Network error: ${e.message}", success = false)
        }
    }
}

/**
 * Mock HTTP client for testing
 */
class MockHttpClient : HttpClient {
    val requests = mutableListOf<MockRequest>()
    var defaultResponse: HttpResponse = HttpResponse(200, "{\"success\": true}")
    val responses = mutableListOf<HttpResponse>()

    private var shouldThrowNetworkError = false
    private var shouldThrowTimeout = false
    private var shouldThrowSSLError = false

    override fun post(url: String, headers: Map<String, String>, body: String): HttpResponse {
        requests.add(MockRequest("POST", url, headers, body))
        return getNextResponse()
    }

    override fun postMultipart(url: String, headers: Map<String, String>, parts: Map<String, Any>): HttpResponse {
        requests.add(MockRequest("POST_MULTIPART", url, headers, parts.toString()))
        return getNextResponse()
    }

    override fun get(url: String, headers: Map<String, String>): HttpResponse {
        requests.add(MockRequest("GET", url, headers, ""))
        return getNextResponse()
    }

    private fun getNextResponse(): HttpResponse {
        if (shouldThrowNetworkError) {
            shouldThrowNetworkError = false
            return HttpResponse(0, "Network error: Connection refused", false)
        }
        if (shouldThrowTimeout) {
            shouldThrowTimeout = false
            return HttpResponse(0, "Network error: Timeout", false)
        }
        if (shouldThrowSSLError) {
            shouldThrowSSLError = false
            return HttpResponse(0, "Network error: SSL handshake failed", false)
        }

        return if (responses.isNotEmpty()) {
            responses.removeAt(0)
        } else {
            defaultResponse
        }
    }

    fun simulateNetworkError() {
        shouldThrowNetworkError = true
    }

    fun simulateTimeout() {
        shouldThrowTimeout = true
    }

    fun simulateSSLError() {
        shouldThrowSSLError = true
    }

    fun reset() {
        requests.clear()
        responses.clear()
        defaultResponse = HttpResponse(200, "{\"success\": true}")
        shouldThrowNetworkError = false
        shouldThrowTimeout = false
        shouldThrowSSLError = false
    }

    data class MockRequest(
        val method: String,
        val url: String,
        val headers: Map<String, String>,
        val body: String
    )
}
