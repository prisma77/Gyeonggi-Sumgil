package com.gyeonggisumgil.app.data.gemini

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiApi(
    private val apiKey: String,
    private val model: String = GEMINI_MODEL,
    private val client: OkHttpClient = defaultClient()
) {
    fun generateWalkingAdvice(prompt: String): String {
        require(apiKey.isNotBlank()) { "GEMINI_API_KEY is blank." }
        require(prompt.isNotBlank()) { "prompt is blank." }

        val bodyJson = JsonObject().apply {
            add(
                "systemInstruction",
                JsonObject().apply {
                    add(
                        "parts",
                        JsonArray().apply {
                            add(
                                JsonObject().apply {
                                    addProperty(
                                        "text",
                                        "너는 경기 숨길 앱의 산책 상담 AI다. 한국어로 답하고, 미세먼지 수치와 경로 정보를 근거로 안전하고 실용적인 산책 조언만 제공한다. 의료 진단처럼 단정하지 말고 민감군 주의 문구를 포함한다."
                                    )
                                }
                            )
                        }
                    )
                }
            )
            add(
                "contents",
                JsonArray().apply {
                    add(
                        JsonObject().apply {
                            addProperty("role", "user")
                            add(
                                "parts",
                                JsonArray().apply {
                                    add(JsonObject().apply { addProperty("text", prompt) })
                                }
                            )
                        }
                    )
                }
            )
            add(
                "generationConfig",
                JsonObject().apply {
                    addProperty("temperature", 0.45)
                    addProperty("topP", 0.9)
                    addProperty("maxOutputTokens", 1_200)
                }
            )
        }.toString()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
            .addHeader("Accept", "application/json")
            .post(bodyJson.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("Gemini request failed: ${response.code} $body")
            }

            val root = JsonParser.parseString(body).asJsonObject
            val candidates = root.getAsJsonArray("candidates") ?: return ""
            return candidates
                .firstOrNull()
                ?.asJsonObject
                ?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")
                ?.mapNotNull { part ->
                    part.asJsonObject.get("text")?.asString
                }
                ?.joinToString("\n")
                .orEmpty()
        }
    }

    companion object {
        const val GEMINI_MODEL = "gemini-2.5-flash"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(25, TimeUnit.SECONDS)
                .build()
        }
    }
}
