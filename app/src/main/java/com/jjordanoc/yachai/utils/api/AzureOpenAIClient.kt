package com.jjordanoc.yachai.utils.api

import android.graphics.Bitmap
import android.util.Base64
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object AzureOpenAIClient {
    // IMPORTANT: Replace these placeholders with your actual Azure OpenAI details.
    private const val API_KEY = "YOUR_API_KEY"
    private const val ENDPOINT = "https://eastus.api.cognitive.microsoft.com/"
    private const val DEPLOYMENT_NAME = "gpt-4o-mini"
    private const val API_VERSION = "2024-05-01-preview"

    private val client = OkHttpClient()

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun callAzureOpenAI(prompt: String, bitmap: Bitmap? = null): String {
        if (API_KEY.contains("YOUR_") || ENDPOINT.contains("YOUR_") || DEPLOYMENT_NAME.contains("YOUR_")) {
            return "Error: Please configure your Azure OpenAI API Key, Endpoint, and Deployment Name in AzureOpenAIClient.kt"
        }

        val url = "$ENDPOINT/openai/deployments/$DEPLOYMENT_NAME/chat/completions?api-version=$API_VERSION"

        val userContent = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "text")
                put("text", prompt)
            })
            if (bitmap != null) {
                val base64Image = bitmapToBase64(bitmap)
                put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:image/jpeg;base64,$base64Image")
                    })
                })
            }
        }

        val requestBodyJson = JSONObject().apply {
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userContent)
                })
            })
            put("max_tokens", 800)
            put("temperature", 0.7)
        }

        val requestBody = requestBodyJson.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("api-key", API_KEY)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP error: ${response.code} ${response.message}")

            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            // Parse the response to get the content of the message
            val jsonResponse = JSONObject(responseBody)
            val content = jsonResponse.getJSONArray("choices")
                                      .getJSONObject(0)
                                      .getJSONObject("message")
                                      .getString("content")
            return content
        }
    }
} 