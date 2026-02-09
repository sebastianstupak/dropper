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
    val success: Boolean = code in 200..299
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
    var nextResponse: HttpResponse = HttpResponse(200, "{\"success\": true}")

    override fun post(url: String, headers: Map<String, String>, body: String): HttpResponse {
        requests.add(MockRequest("POST", url, headers, body))
        return nextResponse
    }

    override fun postMultipart(url: String, headers: Map<String, String>, parts: Map<String, Any>): HttpResponse {
        requests.add(MockRequest("POST_MULTIPART", url, headers, parts.toString()))
        return nextResponse
    }

    override fun get(url: String, headers: Map<String, String>): HttpResponse {
        requests.add(MockRequest("GET", url, headers, ""))
        return nextResponse
    }

    fun reset() {
        requests.clear()
        nextResponse = HttpResponse(200, "{\"success\": true}")
    }

    data class MockRequest(
        val method: String,
        val url: String,
        val headers: Map<String, String>,
        val body: String
    )
}
