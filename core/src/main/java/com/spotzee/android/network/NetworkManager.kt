package com.spotzee.android.network

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.spotzee.android.Alias
import com.spotzee.android.Config
import com.spotzee.android.Constants
import com.spotzee.android.NotificationContent
import com.spotzee.android.SpotzeeNotification
import com.spotzee.android.TrackUser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.net.URL
import java.util.Date

class NetworkManager(
    internal val config: Config,
) {

    private val gson: Gson = GsonBuilder()
        .serializeNulls()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(Date::class.java, DateAdapter())
        .registerTypeAdapter(SpotzeeNotification::class.java, SpotzeeNotificationDeserializer())
        .registerTypeAdapterFactory(TrackUserAdapterFactory())
        .create()

    private val httpLoggingInterceptor: HttpLoggingInterceptor
        get() {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = if (config.isDebug) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            return interceptor
        }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(httpLoggingInterceptor)
        .build()

    internal suspend inline fun <reified T> get(
        path: String,
        user: Alias,
        useBaseUri: Boolean = true,
    ): Result<T> {
        requireNotNull(user.externalId)

        val url = if (useBaseUri) URL("${Constants.DEFAULT_URL_ENDPOINT}/client/$path") else URL(path)
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeaders(config)
            .addHeader("x-anonymous-id", user.anonymousId)
            .addHeader("x-external-id", user.externalId)
            .build()
        return execute(request)
    }

    internal suspend inline fun <reified T> put(
        path: String,
        body: Any,
        useBaseUri: Boolean = true,
    ): Result<T> {
        val url = if (useBaseUri) URL("${Constants.DEFAULT_URL_ENDPOINT}/client/$path") else URL(path)
        val requestBody = gson.toJson(body).toRequestBody()
        val request = Request.Builder().url(url)
            .put(requestBody)
            .addHeaders(config)
            .build()
        return execute(request)
    }

    internal suspend inline fun <reified T> post(
        path: String,
        body: Any,
        useBaseUri: Boolean = true,
    ): Result<T> {
        val url = if (useBaseUri) URL("${Constants.DEFAULT_URL_ENDPOINT}/client/$path") else URL(path)
        val requestBody = gson.toJson(body).toRequestBody()
        val request = Request.Builder().url(url)
            .post(requestBody)
            .addHeaders(config)
            .build()
        return execute(request)
    }

    private fun Request.Builder.addHeaders(config: Config): Request.Builder = this
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Spotzee-Version", Constants.SPOTZEE_API_VERSION)
            .addHeader("x-spotzee-client-type", Constants.CLIENT_TYPE)

    private suspend inline fun <reified T> execute(request: Request): Result<T> {
        try {
            val response = client.newCall(request).executeAsync()
            if (!response.isSuccessful) {
                val rawBody = response.body.string()
                val requestId = response.header("X-Request-Id")
                return Result.failure(parseError(response.code, rawBody, requestId))
            }
            val isValid = (200 until 299).contains(response.code)

            if (!isValid) {
                val requestId = response.header("X-Request-Id")
                return Result.failure(parseError(response.code, "", requestId))
            }
            val typeToken = object : TypeToken<T>() {}

            // Handle cases where T might be Unit (for responses with no body expected)
            if (typeToken.type == Unit::class.java || typeToken.rawType == Nothing::class.java) {
                return Result.success(Unit as T)
            } else {
                val parsedObject: T = gson.fromJson(response.body.string(), typeToken.type)
                return Result.success(parsedObject)
            }
        } catch (e: IOException) {
            return Result.failure(e)
        }
    }

    /**
     * Best-effort parse of the new RFC 7807 error envelope (Spotzee API
     * 2026-04-28+). Falls back to the legacy shape when fields are missing,
     * so this works across the cutover window.
     */
    private fun parseError(statusCode: Int, body: String, requestId: String?): Throwable {
        var code: String? = null
        var message: String? = null
        var bodyRequestId: String? = null
        if (body.isNotEmpty()) {
            try {
                @Suppress("UNCHECKED_CAST")
                val parsed = gson.fromJson(body, Map::class.java) as? Map<String, Any?>
                if (parsed != null) {
                    code = parsed["code"] as? String
                    message = (parsed["message"] as? String) ?: (parsed["error"] as? String)
                    bodyRequestId = parsed["request_id"] as? String
                }
            } catch (_: Exception) {
                // Not JSON or not the expected shape — fall through to a plain HTTP error.
            }
        }
        val resolvedRequestId = requestId ?: bodyRequestId
        val parts = mutableListOf("HTTP $statusCode")
        if (code != null) parts.add("code=$code")
        if (message != null) parts.add(message)
        if (resolvedRequestId != null) parts.add("request_id=$resolvedRequestId")
        if (body.isNotEmpty() && code == null && message == null) {
            // Couldn't parse a structured envelope; surface the raw body for diagnostics.
            parts.add("body=$body")
        }
        return IOException(parts.joinToString(separator = " | "))
    }
}